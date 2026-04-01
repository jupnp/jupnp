/*
 * Copyright (C) 2011-2026 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SPDX-License-Identifier: CDDL-1.0
 */
package org.jupnp.model.gena;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jupnp.internal.compat.java.beans.PropertyChangeEvent;
import org.jupnp.internal.compat.java.beans.PropertyChangeListener;
import org.jupnp.model.ServiceManager;
import org.jupnp.model.UserConstants;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An incoming subscription to a local service.
 * <p>
 * Uses the {@link org.jupnp.model.ServiceManager} to read the initial state of
 * the {@link org.jupnp.model.meta.LocalService} on instantation. Typically, the
 * {@link #registerOnService()} method is called next, and from this point forward all
 * {@link org.jupnp.model.ServiceManager#EVENTED_STATE_VARIABLES} property change
 * events are detected by this subscription. After moderation of state variable values
 * (frequency and range of changes), the {@link #eventReceived()} method is called.
 * Delivery of the event message to the subscriber is not part of this class, but the
 * implementor of {@link #eventReceived()}.
 * </p>
 *
 * @author Christian Bauer
 * @author Jochen Hiller - Changed to use Compact2 compliant Java Beans
 */
public abstract class LocalGENASubscription extends GENASubscription<LocalService> implements PropertyChangeListener {

    private final Logger logger = LoggerFactory.getLogger(LocalGENASubscription.class);

    final List<URL> callbackURLs;

    // Moderation history
    final Map<String, Long> lastSentTimestamp = new HashMap<>();
    final Map<String, Long> lastSentNumericValue = new HashMap<>();

    protected LocalGENASubscription(LocalService service, List<URL> callbackURLs) throws Exception {
        super(service);
        this.callbackURLs = callbackURLs;
    }

    protected LocalGENASubscription(LocalService service, Integer requestedDurationSeconds, List<URL> callbackURLs)
            throws Exception {
        super(service);

        setSubscriptionDuration(requestedDurationSeconds);

        logger.trace("Reading initial state of local service at subscription time");
        long currentTime = new Date().getTime();
        this.currentValues.clear();

        Collection<StateVariableValue> values = getService().getManager().getCurrentState();

        logger.trace("Got evented state variable values: {}", values.size());

        for (StateVariableValue value : values) {
            this.currentValues.put(value.getStateVariable().getName(), value);

            logger.trace("Read state variable value '{}': {}", value.getStateVariable().getName(), value);

            // Preserve "last sent" state for future moderation
            lastSentTimestamp.put(value.getStateVariable().getName(), currentTime);
            if (value.getStateVariable().isModeratedNumericType()) {
                lastSentNumericValue.put(value.getStateVariable().getName(), Long.valueOf(value.toString()));
            }
        }

        this.subscriptionId = SubscriptionIdHeader.PREFIX + UUID.randomUUID();
        this.currentSequence = new UnsignedIntegerFourBytes(0);
        this.callbackURLs = callbackURLs;
    }

    public synchronized List<URL> getCallbackURLs() {
        return callbackURLs;
    }

    /**
     * Adds a property change listener on the {@link org.jupnp.model.ServiceManager}.
     */
    public synchronized void registerOnService() {
        getService().getManager().getPropertyChangeSupport().addPropertyChangeListener(this);
    }

    public synchronized void establish() {
        established();
    }

    /**
     * Removes a property change listener on the {@link org.jupnp.model.ServiceManager}.
     */
    public synchronized void end(CancelReason reason) {
        try {
            getService().getManager().getPropertyChangeSupport().removePropertyChangeListener(this);
        } catch (Exception e) {
            logger.warn("Removal of local service property change listener failed", e);
        }
        ended(reason);
    }

    /**
     * Moderates {@link org.jupnp.model.ServiceManager#EVENTED_STATE_VARIABLES} events and state variable
     * values, calls {@link #eventReceived()}.
     */
    @Override
    public synchronized void propertyChange(PropertyChangeEvent e) {
        if (!e.getPropertyName().equals(ServiceManager.EVENTED_STATE_VARIABLES)) {
            return;
        }

        logger.trace("Eventing triggered, getting state for subscription: {}", getSubscriptionId());

        long currentTime = new Date().getTime();

        Collection<StateVariableValue> newValues = (Collection) e.getNewValue();
        Set<String> excludedVariables = moderateStateVariables(currentTime, newValues);

        currentValues.clear();
        for (StateVariableValue newValue : newValues) {
            String name = newValue.getStateVariable().getName();
            if (!excludedVariables.contains(name)) {
                logger.trace("Adding state variable value to current values of event: {} = {}",
                        newValue.getStateVariable(), newValue);
                currentValues.put(newValue.getStateVariable().getName(), newValue);

                // Preserve "last sent" state for future moderation
                lastSentTimestamp.put(name, currentTime);
                if (newValue.getStateVariable().isModeratedNumericType()) {
                    lastSentNumericValue.put(name, Long.valueOf(newValue.toString()));
                }
            }
        }

        if (!currentValues.isEmpty()) {
            logger.trace("Propagating new state variable values to subscription: {}", this);
            // TODO: I'm not happy with this design, this dispatches to a separate thread which _then_
            // is supposed to lock and read the values off this instance. That obviously doesn't work
            // so it's currently a hack in SendingEvent.java
            eventReceived();
        } else {
            logger.trace("No state variable values for event (all moderated out?), not triggering event");
        }
    }

    /**
     * Checks whether a state variable is moderated, and if this change is within the maximum rate and range limits.
     *
     * @param currentTime The current unix time.
     * @param values The state variable values to moderate.
     * @return A collection of state variable values that although they might have changed, are excluded from the event.
     */
    protected synchronized Set<String> moderateStateVariables(long currentTime, Collection<StateVariableValue> values) {

        Set<String> excludedVariables = new HashSet<>();

        // Moderate event variables that have a maximum rate or minimum delta
        for (StateVariableValue stateVariableValue : values) {

            StateVariable stateVariable = stateVariableValue.getStateVariable();
            String stateVariableName = stateVariableValue.getStateVariable().getName();

            if (stateVariable.getEventDetails().getEventMaximumRateMilliseconds() == 0
                    && stateVariable.getEventDetails().getEventMinimumDelta() == 0) {
                logger.trace("Variable is not moderated: {}", stateVariable);
                continue;
            }

            // That should actually never happen, because we always "send" it as the initial state/event
            if (!lastSentTimestamp.containsKey(stateVariableName)) {
                logger.trace("Variable is moderated but was never sent before: {}", stateVariable);
                continue;
            }

            if (stateVariable.getEventDetails().getEventMaximumRateMilliseconds() > 0) {
                long timestampLastSent = lastSentTimestamp.get(stateVariableName);
                long timestampNextSend = timestampLastSent
                        + stateVariable.getEventDetails().getEventMaximumRateMilliseconds();
                if (currentTime <= timestampNextSend) {
                    logger.trace("Excluding state variable with maximum rate: {}", stateVariable);
                    excludedVariables.add(stateVariableName);
                    continue;
                }
            }

            if (stateVariable.isModeratedNumericType() && lastSentNumericValue.get(stateVariableName) != null) {

                long oldValue = lastSentNumericValue.get(stateVariableName);
                long newValue = Long.parseLong(stateVariableValue.toString());
                long minDelta = stateVariable.getEventDetails().getEventMinimumDelta();

                if (newValue > oldValue && newValue - oldValue < minDelta) {
                    logger.trace("Excluding state variable with minimum delta: {}", stateVariable);
                    excludedVariables.add(stateVariableName);
                    continue;
                }

                if (newValue < oldValue && oldValue - newValue < minDelta) {
                    logger.trace("Excluding state variable with minimum delta: {}", stateVariable);
                    excludedVariables.add(stateVariableName);
                }
            }

        }
        return excludedVariables;
    }

    public synchronized void incrementSequence() {
        this.currentSequence.increment(true);
    }

    /**
     * @param requestedDurationSeconds If <code>null</code> defaults to
     *            {@link org.jupnp.model.UserConstants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS}
     */
    public synchronized void setSubscriptionDuration(Integer requestedDurationSeconds) {
        this.requestedDurationSeconds = requestedDurationSeconds == null
                ? UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS
                : requestedDurationSeconds;

        setActualSubscriptionDurationSeconds(this.requestedDurationSeconds);
    }

    public abstract void ended(CancelReason reason);
}
