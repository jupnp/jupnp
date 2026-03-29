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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.Location;
import org.jupnp.model.Namespace;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.UnsignedIntegerFourBytes;

/**
 * An outgoing subscription to a remote service.
 * <p>
 * Once established, calls its {@link #eventReceived()} method whenever an event has
 * been received from the remote service.
 * </p>
 *
 * @author Christian Bauer
 * @author Jochen Hiller - Changed to use Compact2 compliant Java Beans
 */
public abstract class RemoteGENASubscription extends GENASubscription<RemoteService> {

    protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    protected RemoteGENASubscription(RemoteService service, int requestedDurationSeconds) {
        super(service, requestedDurationSeconds);
    }

    public URL getEventSubscriptionURL() {
        return getService().getDevice().normalizeURI(getService().getEventSubscriptionURI());
    }

    public List<URL> getEventCallbackURLs(List<NetworkAddress> activeStreamServers, Namespace namespace) {
        List<URL> callbackURLs = new ArrayList<>();
        for (NetworkAddress activeStreamServer : activeStreamServers) {
            callbackURLs
                    .add(new Location(activeStreamServer, namespace.getEventCallbackPathString(getService())).getURL());
        }
        return callbackURLs;
    }

    /*
     * The following four methods should always be called in an independent thread, not within the
     * message receiving thread. Otherwise the user who implements the abstract delegate methods can
     * block the network communication.
     */

    public synchronized void establish() {
        established();
    }

    public synchronized void fail(UpnpResponse responseStatus) {
        failed(responseStatus);
    }

    public synchronized void end(CancelReason reason, UpnpResponse response) {
        ended(reason, response);
    }

    public synchronized void receive(UnsignedIntegerFourBytes sequence, Collection<StateVariableValue> newValues) {

        if (this.currentSequence != null) {

            // TODO: Handle rollover to 1!
            if (this.currentSequence.getValue().equals(this.currentSequence.getBits().getMaxValue())
                    && sequence.getValue() == 1) {
                System.err.println("TODO: HANDLE ROLLOVER");
                return;
            }

            if (this.currentSequence.getValue() >= sequence.getValue()) {
                return;
            }

            int difference;
            long expectedValue = currentSequence.getValue() + 1;
            if ((difference = (int) (sequence.getValue() - expectedValue)) != 0) {
                eventsMissed(difference);
            }

        }

        this.currentSequence = sequence;

        for (StateVariableValue newValue : newValues) {
            currentValues.put(newValue.getStateVariable().getName(), newValue);
        }

        eventReceived();
    }

    public abstract void invalidMessage(UnsupportedDataException e);

    public abstract void failed(UpnpResponse responseStatus);

    public abstract void ended(CancelReason reason, UpnpResponse responseStatus);

    public abstract void eventsMissed(int numberOfMissedEvents);

    @Override
    public String toString() {
        return "(SID: " + getSubscriptionId() + ") " + getService();
    }
}
