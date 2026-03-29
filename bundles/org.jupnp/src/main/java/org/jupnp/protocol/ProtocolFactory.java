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
package org.jupnp.protocol;

import java.net.URL;

import org.jupnp.UpnpService;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.protocol.async.SendingNotificationAlive;
import org.jupnp.protocol.async.SendingNotificationByebye;
import org.jupnp.protocol.async.SendingSearch;
import org.jupnp.protocol.sync.SendingAction;
import org.jupnp.protocol.sync.SendingEvent;
import org.jupnp.protocol.sync.SendingRenewal;
import org.jupnp.protocol.sync.SendingSubscribe;
import org.jupnp.protocol.sync.SendingUnsubscribe;

/**
 * Factory for UPnP protocols, the core implementation of the UPnP specification.
 * <p>
 * This factory creates an executable protocol either based on the received UPnP messsage, or
 * on local device/search/service metadata). A protocol is an aspect of the UPnP specification,
 * you can override individual protocols to customize the behavior of the UPnP stack.
 * </p>
 * <p>
 * An implementation has to be thread-safe.
 * </p>
 * 
 * @author Christian Bauer
 */
public interface ProtocolFactory {

    UpnpService getUpnpService();

    /**
     * Creates a {@link org.jupnp.protocol.async.ReceivingNotification},
     * {@link org.jupnp.protocol.async.ReceivingSearch},
     * or {@link org.jupnp.protocol.async.ReceivingSearchResponse} protocol.
     *
     * @param message The incoming message, either {@link org.jupnp.model.message.UpnpRequest} or
     *            {@link org.jupnp.model.message.UpnpResponse}.
     * @return The appropriate protocol that handles the messages or <code>null</code> if the message should be dropped.
     * @throws ProtocolCreationException If no protocol could be found for the message.
     */
    ReceivingAsync createReceivingAsync(IncomingDatagramMessage message) throws ProtocolCreationException;

    /**
     * Creates a {@link org.jupnp.protocol.sync.ReceivingRetrieval},
     * {@link org.jupnp.protocol.sync.ReceivingAction},
     * {@link org.jupnp.protocol.sync.ReceivingSubscribe},
     * {@link org.jupnp.protocol.sync.ReceivingUnsubscribe}, or
     * {@link org.jupnp.protocol.sync.ReceivingEvent} protocol.
     *
     * @param requestMessage The incoming message, examime {@link org.jupnp.model.message.UpnpRequest.Method}
     *            to determine the protocol.
     * @return The appropriate protocol that handles the messages.
     * @throws ProtocolCreationException If no protocol could be found for the message.
     */
    ReceivingSync createReceivingSync(StreamRequestMessage requestMessage) throws ProtocolCreationException;

    /**
     * Called by the {@link org.jupnp.registry.Registry}, creates a protocol for announcing local devices.
     */
    SendingNotificationAlive createSendingNotificationAlive(LocalDevice localDevice);

    /**
     * Called by the {@link org.jupnp.registry.Registry}, creates a protocol for announcing local devices.
     */
    SendingNotificationByebye createSendingNotificationByebye(LocalDevice localDevice);

    /**
     * Called by the {@link org.jupnp.controlpoint.ControlPoint}, creates a protocol for a multicast search.
     */
    SendingSearch createSendingSearch(UpnpHeader searchTarget, int mxSeconds);

    /**
     * Called by the {@link org.jupnp.controlpoint.ControlPoint}, creates a protocol for executing an action.
     */
    SendingAction createSendingAction(ActionInvocation actionInvocation, URL controlURL);

    /**
     * Called by the {@link org.jupnp.controlpoint.ControlPoint}, creates a protocol for GENA subscription.
     */
    SendingSubscribe createSendingSubscribe(RemoteGENASubscription subscription) throws ProtocolCreationException;

    /**
     * Called by the {@link org.jupnp.controlpoint.ControlPoint}, creates a protocol for GENA renewal.
     */
    SendingRenewal createSendingRenewal(RemoteGENASubscription subscription);

    /**
     * Called by the {@link org.jupnp.controlpoint.ControlPoint}, creates a protocol for GENA unsubscription.
     */
    SendingUnsubscribe createSendingUnsubscribe(RemoteGENASubscription subscription);

    /**
     * Called by the {@link org.jupnp.model.gena.GENASubscription}, creates a protocol for sending GENA events.
     */
    SendingEvent createSendingEvent(LocalGENASubscription subscription);
}
