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
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.IncomingUnsubscribeRequestMessage;
import org.jupnp.model.resource.ServiceEventSubscriptionResource;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles reception of GENA event unsubscribe messages.
 *
 * @author Christian Bauer
 */
public class ReceivingUnsubscribe extends ReceivingSync<StreamRequestMessage, StreamResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(ReceivingUnsubscribe.class);

    public ReceivingUnsubscribe(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    @Override
    protected StreamResponseMessage executeSync() throws RouterException {

        ServiceEventSubscriptionResource resource = getUpnpService().getRegistry()
                .getResource(ServiceEventSubscriptionResource.class, getInputMessage().getUri());

        if (resource == null) {
            logger.trace("No local resource found: {}", getInputMessage());
            return null;
        }

        logger.trace("Found local event subscription matching relative request URI: {}", getInputMessage().getUri());

        IncomingUnsubscribeRequestMessage requestMessage = new IncomingUnsubscribeRequestMessage(getInputMessage(),
                resource.getModel());

        // Error conditions UDA 1.0 section 4.1.3
        if (requestMessage.getSubscriptionId() != null
                && (requestMessage.hasNotificationHeader() || requestMessage.hasCallbackHeader())) {
            logger.trace("Subscription ID and NT or Callback in unsubcribe request: {}", getInputMessage());
            return new StreamResponseMessage(UpnpResponse.Status.BAD_REQUEST);
        }

        LocalGENASubscription subscription = getUpnpService().getRegistry()
                .getLocalSubscription(requestMessage.getSubscriptionId());

        if (subscription == null) {
            logger.trace("Invalid subscription ID for unsubscribe request: {}", getInputMessage());
            return new StreamResponseMessage(UpnpResponse.Status.PRECONDITION_FAILED);
        }

        logger.trace("Unregistering subscription: {}", subscription);
        if (getUpnpService().getRegistry().removeLocalSubscription(subscription)) {
            subscription.end(null); // No reason, just an unsubscribe
        } else {
            logger.trace("Subscription was already removed from registry");
        }

        return new StreamResponseMessage(UpnpResponse.Status.OK);
    }
}
