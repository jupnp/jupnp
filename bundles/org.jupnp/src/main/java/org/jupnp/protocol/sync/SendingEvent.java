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

import java.net.URL;

import org.jupnp.UpnpService;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.OutgoingEventRequestMessage;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.protocol.SendingSync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sending GENA event messages to remote subscribers.
 * <p>
 * Any {@link org.jupnp.model.gena.LocalGENASubscription} instantiates and executes this protocol
 * when the state of a local service changes. A remote subscriber might offer more than one
 * callback URL. These are alternatives, they are tried in order until the event notification
 * message has been delivered to one of them, so this protocol potentially sends many messages.
 * What is returned is always the last response, that is, the response for the message sent to
 * the last callback URL which has been tried.
 * </p>
 * <p>
 * If a subscriber answers an event message with <code>412 Precondition Failed</code>, it doesn't know
 * the subscription. The subscription is then removed from the registry and ended, no further event
 * messages are sent.
 * </p>
 *
 * @author Christian Bauer
 */
public class SendingEvent extends SendingSync<OutgoingEventRequestMessage, StreamResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(SendingEvent.class);

    protected final String subscriptionId;
    protected final OutgoingEventRequestMessage[] requestMessages;
    protected final UnsignedIntegerFourBytes currentSequence;

    public SendingEvent(UpnpService upnpService, LocalGENASubscription subscription) {
        super(upnpService, null); // Special case, we actually need to send several messages to each callback URL

        // TODO: Ugly design! It is critical (concurrency) that we prepare the event messages here, in the constructor
        // thread!

        subscriptionId = subscription.getSubscriptionId();

        // This is the monitor held by LocalGENASubscription#propertyChange() while it replaces the current values and
        // then calls us. Whoever else prepares an event (the initial one) must not read or clear them in between.
        synchronized (subscription) {
            requestMessages = new OutgoingEventRequestMessage[subscription.getCallbackURLs().size()];
            int i = 0;
            for (URL url : subscription.getCallbackURLs()) {
                requestMessages[i] = new OutgoingEventRequestMessage(subscription, url);
                getUpnpService().getConfiguration().getGenaEventProcessor().writeBody(requestMessages[i]);
                i++;
            }

            currentSequence = subscription.getCurrentSequence();

            // Always increment sequence now, as (its value) has already been set on the headers and the
            // next event will use the incremented value
            subscription.incrementSequence();

            // The values are in the message bodies now and nobody reads them again, so don't let the subscription
            // hold on to them until the next event of the service (which might never come). The values of an initial
            // event are read for this subscription alone and can be large, e.g. a "LastChange" document.
            subscription.getCurrentValues().clear();
        }
    }

    @Override
    protected StreamResponseMessage executeSync() throws RouterException {

        logger.trace("Sending event for subscription: {}", subscriptionId);

        StreamResponseMessage lastResponse = null;

        for (OutgoingEventRequestMessage requestMessage : requestMessages) {

            if (currentSequence.getValue() == 0) {
                logger.trace("Sending initial event message to callback URL: {}", requestMessage.getUri());
            } else {
                logger.trace("Sending event message '{}' to callback URL: {}", currentSequence,
                        requestMessage.getUri());
            }

            // Send request
            lastResponse = getUpnpService().getRouter().send(requestMessage);
            logger.trace("Received event callback response: {}", lastResponse);

            // Error conditions UDA 1.0 section 4.2.1: the subscriber doesn't know this subscription, the service
            // must terminate it. Any other failure is not a reason, the subscription then stays until it expires.
            if (lastResponse != null && lastResponse.getOperation()
                    .getStatusCode() == UpnpResponse.Status.PRECONDITION_FAILED.getStatusCode()) {
                logger.trace("Subscriber rejected the subscription ID of the event, unregistering subscription: {}",
                        subscriptionId);
                LocalGENASubscription subscription = getUpnpService().getRegistry()
                        .getLocalSubscription(subscriptionId);
                if (subscription != null && getUpnpService().getRegistry().removeLocalSubscription(subscription)) {
                    subscription.end(null); // No reason, same as an unsubscribe
                } else {
                    logger.trace("Subscription was already removed from registry");
                }
                break;
            }

            // UDA 1.0 section 4.1.1: "If there is more than 1 URL, when the service sends events, it will try
            // these URLs in order until one succeeds."
            if (lastResponse != null && !lastResponse.getOperation().isFailed()) {
                break;
            }
        }

        // It's not really used, so just return the last one - we have only one callback URL most of the
        // time anyway
        return lastResponse;
    }
}
