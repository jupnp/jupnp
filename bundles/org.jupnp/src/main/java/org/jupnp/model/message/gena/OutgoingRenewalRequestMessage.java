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
package org.jupnp.model.message.gena;

import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;

/**
 * @author Christian Bauer
 */
public class OutgoingRenewalRequestMessage extends StreamRequestMessage {

    public OutgoingRenewalRequestMessage(RemoteGENASubscription subscription, UpnpHeaders extraHeaders) {

        super(UpnpRequest.Method.SUBSCRIBE, subscription.getEventSubscriptionURL());

        getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader(subscription.getSubscriptionId()));

        getHeaders().add(UpnpHeader.Type.TIMEOUT, new TimeoutHeader(subscription.getRequestedDurationSeconds()));

        if (extraHeaders != null) {
            getHeaders().putAll(extraHeaders);
        }
    }
}
