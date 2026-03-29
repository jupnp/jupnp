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

import java.util.List;

import org.jupnp.UpnpService;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.gena.IncomingSubscribeResponseMessage;
import org.jupnp.model.message.gena.OutgoingSubscribeRequestMessage;
import org.jupnp.protocol.SendingSync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Establishing a GENA event subscription with a remote host.
 * <p>
 * Calls the {@link org.jupnp.model.gena.RemoteGENASubscription#establish()} method
 * if the subscription request was responded to correctly.
 * </p>
 * <p>
 * The {@link org.jupnp.model.gena.RemoteGENASubscription#fail(org.jupnp.model.message.UpnpResponse)}
 * method will be called if the request failed. No response from the remote host is indicated with
 * a <code>null</code> argument value. Note that this is also the response if the subscription has
 * to be aborted early, when no local stream server for callback URL creation is available. This is
 * the case when the local network transport layer is switched off, subscriptions will fail
 * immediately with no response.
 * </p>
 *
 * @author Christian Bauer
 */
public class SendingSubscribe extends SendingSync<OutgoingSubscribeRequestMessage, IncomingSubscribeResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(SendingSubscribe.class);

    protected final RemoteGENASubscription subscription;

    public SendingSubscribe(UpnpService upnpService, RemoteGENASubscription subscription,
            List<NetworkAddress> activeStreamServers) {
        super(upnpService,
                new OutgoingSubscribeRequestMessage(subscription,
                        subscription.getEventCallbackURLs(activeStreamServers,
                                upnpService.getConfiguration().getNamespace()),
                        upnpService.getConfiguration().getEventSubscriptionHeaders(subscription.getService())));

        this.subscription = subscription;
    }

    @Override
    protected IncomingSubscribeResponseMessage executeSync() throws RouterException {

        if (!getInputMessage().hasCallbackURLs()) {
            logger.trace("Subscription failed, no active local callback URLs available (network disabled?)");
            getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(() -> subscription.fail(null));
            return null;
        }

        logger.trace("Sending subscription request: {}", getInputMessage());

        try {
            // register this pending Subscription to bloc if the notification is received before the
            // registration result.
            getUpnpService().getRegistry().registerPendingRemoteSubscription(subscription);

            StreamResponseMessage response;
            try {
                response = getUpnpService().getRouter().send(getInputMessage());
            } catch (RouterException e) {
                onSubscriptionFailure();
                return null;
            }

            if (response == null) {
                onSubscriptionFailure();
                return null;
            }

            final IncomingSubscribeResponseMessage responseMessage = new IncomingSubscribeResponseMessage(response);

            if (response.getOperation().isFailed()) {
                logger.trace("Subscription failed, response was: {}", responseMessage);
                getUpnpService().getConfiguration().getRegistryListenerExecutor()
                        .execute(() -> subscription.fail(responseMessage.getOperation()));
            } else if (!responseMessage.isValidHeaders()) {
                logger.error("Subscription failed, invalid or missing (SID, Timeout) response headers");
                getUpnpService().getConfiguration().getRegistryListenerExecutor()
                        .execute(() -> subscription.fail(responseMessage.getOperation()));
            } else {

                logger.trace("Subscription established, adding to registry, response was: {}", response);
                subscription.setSubscriptionId(responseMessage.getSubscriptionId());
                subscription.setActualSubscriptionDurationSeconds(responseMessage.getSubscriptionDurationSeconds());

                getUpnpService().getRegistry().addRemoteSubscription(subscription);

                getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(subscription::establish);

            }
            return responseMessage;
        } finally {
            getUpnpService().getRegistry().unregisterPendingRemoteSubscription(subscription);
        }
    }

    protected void onSubscriptionFailure() {
        logger.trace("Subscription failed");
        getUpnpService().getConfiguration().getRegistryListenerExecutor().execute(() -> subscription.fail(null));
    }
}
