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

import java.net.URL;
import java.util.List;

import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.CallbackHeader;
import org.jupnp.model.message.header.NTEventHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;

/**
 * @author Christian Bauer
 */
public class OutgoingSubscribeRequestMessage extends StreamRequestMessage {

    public OutgoingSubscribeRequestMessage(RemoteGENASubscription subscription, List<URL> callbackURLs,
            UpnpHeaders extraHeaders) {

        super(UpnpRequest.Method.SUBSCRIBE, subscription.getEventSubscriptionURL());

        getHeaders().add(UpnpHeader.Type.CALLBACK, new CallbackHeader(callbackURLs));

        getHeaders().add(UpnpHeader.Type.NT, new NTEventHeader());

        getHeaders().add(UpnpHeader.Type.TIMEOUT, new TimeoutHeader(subscription.getRequestedDurationSeconds()));

        if (extraHeaders != null) {
            getHeaders().putAll(extraHeaders);
        }
    }

    public boolean hasCallbackURLs() {
        CallbackHeader callbackHeader = getHeaders().getFirstHeader(UpnpHeader.Type.CALLBACK, CallbackHeader.class);
        return !callbackHeader.getValue().isEmpty();
    }
}
