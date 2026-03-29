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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jupnp.model.UserConstants;
import org.jupnp.model.meta.Service;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.UnsignedIntegerFourBytes;

/**
 * An established subscription, with identifer, expiration duration, sequence handling, and state variable values.
 * <p>
 * For every subscription, no matter if it's an incoming subscription to a local service,
 * or a local control point subscribing to a remote servce, an instance is maintained by
 * the {@link org.jupnp.registry.Registry}.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class GENASubscription<S extends Service> {

    protected final S service;
    protected volatile String subscriptionId;
    protected volatile int requestedDurationSeconds = UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS;
    protected volatile int actualDurationSeconds;
    protected volatile UnsignedIntegerFourBytes currentSequence;
    protected Map<String, StateVariableValue<S>> currentValues = new ConcurrentHashMap<>();

    /**
     * Defaults to {@link org.jupnp.model.UserConstants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS}.
     */
    protected GENASubscription(S service) {
        this.service = service;
    }

    protected GENASubscription(S service, int requestedDurationSeconds) {
        this(service);
        this.requestedDurationSeconds = requestedDurationSeconds;
    }

    public S getService() {
        return service;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public int getRequestedDurationSeconds() {
        return requestedDurationSeconds;
    }

    public int getActualDurationSeconds() {
        return actualDurationSeconds;
    }

    public void setActualSubscriptionDurationSeconds(int seconds) {
        this.actualDurationSeconds = seconds;
    }

    public UnsignedIntegerFourBytes getCurrentSequence() {
        return currentSequence;
    }

    public Map<String, StateVariableValue<S>> getCurrentValues() {
        return currentValues;
    }

    public abstract void established();

    public abstract void eventReceived();

    @Override
    public String toString() {
        return "(GENASubscription, SID: " + getSubscriptionId() + ", SEQUENCE: " + getCurrentSequence() + ")";
    }
}
