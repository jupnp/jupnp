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

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import org.jupnp.UpnpService;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.IncomingSubscribeRequestMessage;
import org.jupnp.model.message.gena.OutgoingSubscribeResponseMessage;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.resource.ServiceEventSubscriptionResource;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles reception of GENA event subscription (initial and renewal) messages.
 * <p>
 * This protocol tries to find a local event subscription URI matching the requested URI,
 * then creates a new {@link org.jupnp.model.gena.LocalGENASubscription} if no
 * subscription identifer was supplied.
 * </p>
 * <p>
 * The subscription is however only registered with the local service, and monitoring
 * of state changes is established, if the response of this protocol was successfully
 * delivered to the client which requested the subscription.
 * </p>
 * <p>
 * Once registration and monitoring is active, an initial event with the current
 * state of the service is send to the subscriber. This will only happen after the
 * subscription response message was successfully delivered to the subscriber.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingSubscribe extends ReceivingSync<StreamRequestMessage, OutgoingSubscribeResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(ReceivingSubscribe.class);

    protected LocalGENASubscription subscription;

    public ReceivingSubscribe(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    @Override
    protected OutgoingSubscribeResponseMessage executeSync() throws RouterException {

        ServiceEventSubscriptionResource resource = getUpnpService().getRegistry()
                .getResource(ServiceEventSubscriptionResource.class, getInputMessage().getUri());

        if (resource == null) {
            logger.trace("No local resource found: {}", getInputMessage());
            return null;
        }

        logger.trace("Found local event subscription matching relative request URI: {}", getInputMessage().getUri());

        IncomingSubscribeRequestMessage requestMessage = new IncomingSubscribeRequestMessage(getInputMessage(),
                resource.getModel());

        /// UDA 2.0, section 4.1.1: ensure callback url is in private network range
        if (requestMessage.getCallbackURLs() != null) {
            for (URL callbackUrl : requestMessage.getCallbackURLs()) {
                try {
                    InetAddress callbackAddress = InetAddress.getByName(callbackUrl.getHost());
                    if (!(callbackAddress.isLoopbackAddress() || callbackAddress.isLinkLocalAddress()
                            || callbackAddress.isSiteLocalAddress())) {
                        logger.trace("Callback URL not on accepted address range: {}", getInputMessage());
                        return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
                    }
                } catch (UnknownHostException e) {
                    logger.trace("Unknown host for callback URL: {}", getInputMessage());
                    return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
                }
            }
        }

        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (requestMessage.getSubscriptionId() != null
                && (requestMessage.hasNotificationHeader() || requestMessage.getCallbackURLs() != null)) {
            logger.trace("Subscription ID and NT or Callback in subscribe request: {}", getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.BAD_REQUEST);
        }

        if (requestMessage.getSubscriptionId() != null) {
            return processRenewal(resource.getModel(), requestMessage);
        } else if (requestMessage.hasNotificationHeader() && requestMessage.getCallbackURLs() != null) {
            return processNewSubscription(resource.getModel(), requestMessage);
        } else {
            logger.trace("No subscription ID, no NT or Callback, neither subscription or renewal: {}",
                    getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }
    }

    protected OutgoingSubscribeResponseMessage processRenewal(LocalService service,
            IncomingSubscribeRequestMessage requestMessage) {

        subscription = getUpnpService().getRegistry().getLocalSubscription(requestMessage.getSubscriptionId());

        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (subscription == null) {
            logger.trace("Invalid subscription ID for renewal request: {}", getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        logger.trace("Renewing subscription: {}", subscription);
        subscription.setSubscriptionDuration(requestMessage.getRequestedTimeoutSeconds());
        if (getUpnpService().getRegistry().updateLocalSubscription(subscription)) {
            return new OutgoingSubscribeResponseMessage(subscription);
        } else {
            logger.trace("Subscription went away before it could be renewed: {}", getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }
    }

    protected OutgoingSubscribeResponseMessage processNewSubscription(LocalService service,
            IncomingSubscribeRequestMessage requestMessage) {
        List<URL> callbackURLs = requestMessage.getCallbackURLs();

        // Error conditions UDA 1.0 section 4.1.1 and 4.1.2
        if (callbackURLs == null || callbackURLs.isEmpty()) {
            logger.trace("Missing or invalid Callback URLs in subscribe request: {}", getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        if (!requestMessage.hasNotificationHeader()) {
            logger.trace("Missing or invalid NT header in subscribe request: {}", getInputMessage());
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        Integer timeoutSeconds;
        if (getUpnpService().getConfiguration().isReceivedSubscriptionTimeoutIgnored()) {
            timeoutSeconds = null; // Use default value
        } else {
            timeoutSeconds = requestMessage.getRequestedTimeoutSeconds();
        }

        try {
            subscription = new LocalGENASubscription(service, timeoutSeconds, callbackURLs) {
                @Override
                public void established() {
                }

                @Override
                public void ended(CancelReason reason) {
                }

                @Override
                public void eventReceived() {
                    // The only thing we are interested in, sending an event when the state changes
                    getUpnpService().getConfiguration().getSyncProtocolExecutorService()
                            .execute(getUpnpService().getProtocolFactory().createSendingEvent(this));
                }
            };
        } catch (Exception e) {
            logger.warn("Couldn't create local subscription to service", e);
            return new OutgoingSubscribeResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
        }

        logger.trace("Adding subscription to registry: {}", subscription);
        getUpnpService().getRegistry().addLocalSubscription(subscription);

        logger.trace("Returning subscription response, waiting to send initial event");
        return new OutgoingSubscribeResponseMessage(subscription);
    }

    @Override
    public void responseSent(StreamResponseMessage responseMessage) {
        if (subscription == null) {
            return; // Preconditions failed very early on
        }
        if (responseMessage != null && !responseMessage.getOperation().isFailed()
                && subscription.getCurrentSequence().getValue() == 0) { // Note that renewals should not have 0

            // This is a minor concurrency issue: If we now register on the service and henceforth send a new
            // event message whenever the state of the service changes, there is still a chance that the initial
            // event message arrives later than the first on-change event message. Shouldn't be a problem as the
            // subscriber is supposed to figure out what to do with out-of-sequence messages. I would be
            // surprised though if actual implementations won't crash!
            logger.trace("Establishing subscription");
            subscription.registerOnService();
            subscription.establish();

            logger.trace("Response to subscription sent successfully, now sending initial event asynchronously");
            getUpnpService().getConfiguration().getAsyncProtocolExecutor()
                    .execute(getUpnpService().getProtocolFactory().createSendingEvent(subscription));

        } else if (subscription.getCurrentSequence().getValue() == 0) {
            logger.trace("Subscription request's response aborted, not sending initial event");
            if (responseMessage == null) {
                logger.trace("Reason: No response at all from subscriber");
            } else {
                logger.trace("Reason: {}", responseMessage.getOperation());
            }
            logger.trace("Removing subscription from registry: {}", subscription);
            getUpnpService().getRegistry().removeLocalSubscription(subscription);
        }
    }

    @Override
    public void responseException(Throwable t) {
        if (subscription == null) {
            return; // Nothing to do, we didn't get that far
        }
        logger.trace("Response could not be send to subscriber, removing local GENA subscription: {}", subscription);
        getUpnpService().getRegistry().removeLocalSubscription(subscription);
    }
}
