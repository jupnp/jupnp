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
import org.jupnp.model.message.gena.IncomingSubscribeResponseMessage;
import org.jupnp.model.message.gena.OutgoingRenewalRequestMessage;
import org.jupnp.protocol.SendingSync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renewing a GENA event subscription with a remote host.
 * <p>
 * This protocol is executed periodically by the local registry, for any established GENA
 * subscription to a remote service. If renewal failed, the subscription will be removed
 * from the registry and the
 * {@link org.jupnp.model.gena.RemoteGENASubscription#end(org.jupnp.model.gena.CancelReason, org.jupnp.model.message.UpnpResponse)}
 * method will be called. The <code>RENEWAL_FAILED</code> reason will be used, however,
 * the response might be <code>null</code> if no response was received from the remote host.
 * </p>
 * 
 * @author Christian Bauer
 */
public class SendingRenewal extends SendingSync<OutgoingRenewalRequestMessage, IncomingSubscribeResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(SendingRenewal.class);

    protected final RemoteGENASubscription subscription;

    public SendingRenewal(UpnpService upnpService, RemoteGENASubscription subscription) {
        super(upnpService, new OutgoingRenewalRequestMessage(subscription,
                upnpService.getConfiguration().getEventSubscriptionHeaders(subscription.getService())));
        this.subscription = subscription;
    }

    @Override
    protected IncomingSubscribeResponseMessage executeSync() throws RouterException {
        logger.trace("Sending subscription renewal request: {}", getInputMessage());

        StreamResponseMessage response;
        try {
            response = getUpnpService().getRouter().send(getInputMessage());
        } catch (RouterException e) {
            onRenewalFailure();
            throw e;
        }

        if (response == null) {
            onRenewalFailure();
            return null;
        }

        final IncomingSubscribeResponseMessage responseMessage = new IncomingSubscribeResponseMessage(response);

        if (response.getOperation().isFailed()) {
            logger.trace("Subscription renewal failed, response was: {}", response);
            getUpnpService().getRegistry().removeRemoteSubscription(subscription);
            getUpnpService().getConfiguration().getRegistryListenerExecutor()
                    .execute(() -> subscription.end(CancelReason.RENEWAL_FAILED, responseMessage.getOperation()));
        } else if (!responseMessage.isValidHeaders()) {
            logger.error("Subscription renewal failed, invalid or missing (SID, Timeout) response headers");
            getUpnpService().getConfiguration().getRegistryListenerExecutor()
                    .execute(() -> subscription.end(CancelReason.RENEWAL_FAILED, responseMessage.getOperation()));
        } else {
            logger.trace("Subscription renewed, updating in registry, response was: {}", response);
            subscription.setActualSubscriptionDurationSeconds(responseMessage.getSubscriptionDurationSeconds());
            getUpnpService().getRegistry().updateRemoteSubscription(subscription);
        }

        return responseMessage;
    }

    protected void onRenewalFailure() {
        logger.trace("Subscription renewal failed, removing subscription from registry");
        getUpnpService().getRegistry().removeRemoteSubscription(subscription);
        getUpnpService().getConfiguration().getRegistryListenerExecutor()
                .execute(() -> subscription.end(CancelReason.RENEWAL_FAILED, null));
    }
}
