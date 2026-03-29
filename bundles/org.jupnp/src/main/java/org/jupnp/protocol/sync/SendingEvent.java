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
 * when the state of a local service changes. However, a remote subscriber might require event
 * notification messages on more than one callback URL, so this protocol potentially sends
 * many messages. What is returned is always the last response, that is, the response for the
 * message sent to the last callback URL in the list of the subscriber.
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

        }

        // It's not really used, so just return the last one - we have only one callback URL most of the
        // time anyway
        return lastResponse;
    }
}
