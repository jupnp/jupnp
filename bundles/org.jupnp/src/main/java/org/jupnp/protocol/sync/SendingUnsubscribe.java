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
package org.jupnp.protocol.sync;

import org.jupnp.UpnpService;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.gena.OutgoingUnsubscribeRequestMessage;
import org.jupnp.protocol.SendingSync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disconnecting a GENA event subscription with a remote host.
 * <p>
 * Calls the
 * {@link org.jupnp.model.gena.RemoteGENASubscription#end(org.jupnp.model.gena.CancelReason, org.jupnp.model.message.UpnpResponse)}
 * method if the subscription request was responded to correctly. No {@link org.jupnp.model.gena.CancelReason}
 * will be provided if the unsubscribe procedure completed as expected, otherwise <code>UNSUBSCRIBE_FAILED</code>
 * is used. The response might be <code>null</code> if no response was received from the remote host.
 * </p>
 *
 * @author Christian Bauer
 */
public class SendingUnsubscribe extends SendingSync<OutgoingUnsubscribeRequestMessage, StreamResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(SendingUnsubscribe.class);

    protected final RemoteGENASubscription subscription;

    public SendingUnsubscribe(UpnpService upnpService, RemoteGENASubscription subscription) {
        super(upnpService, new OutgoingUnsubscribeRequestMessage(subscription,
                upnpService.getConfiguration().getEventSubscriptionHeaders(subscription.getService())));
        this.subscription = subscription;
    }

    @Override
    protected StreamResponseMessage executeSync() throws RouterException {

        logger.trace("Sending unsubscribe request: {}", getInputMessage());

        StreamResponseMessage response = null;
        try {
            response = getUpnpService().getRouter().send(getInputMessage());
            return response;
        } finally {
            onUnsubscribe(response);
        }
    }

    protected void onUnsubscribe(final StreamResponseMessage response) {
        // Always remove from the registry and end the subscription properly - even if it's failed
        getUpnpService().getRegistry().removeRemoteSubscription(subscription);

        getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(() -> {
            if (response == null) {
                logger.trace("Unsubscribe failed, no response received");
                subscription.end(CancelReason.UNSUBSCRIBE_FAILED, null);
            } else if (response.getOperation().isFailed()) {
                logger.trace("Unsubscribe failed, response was: {}", response);
                subscription.end(CancelReason.UNSUBSCRIBE_FAILED, response.getOperation());
            } else {
                logger.trace("Unsubscribe successful, response was: {}", response);
                subscription.end(null, response.getOperation());
            }
        });
    }
}
