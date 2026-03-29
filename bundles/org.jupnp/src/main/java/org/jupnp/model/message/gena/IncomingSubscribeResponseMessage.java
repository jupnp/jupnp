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

import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;

/**
 * @author Christian Bauer
 */
public class IncomingSubscribeResponseMessage extends StreamResponseMessage {

    public IncomingSubscribeResponseMessage(StreamResponseMessage source) {
        super(source);
    }

    /**
     * @return <code>true</code> if this message has an SID and TIMEOUT header value.
     */
    public boolean isValidHeaders() {
        return getHeaders().getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class) != null
                && getHeaders().getFirstHeader(UpnpHeader.Type.TIMEOUT, TimeoutHeader.class) != null;
    }

    public String getSubscriptionId() {
        return getHeaders().getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();
    }

    public int getSubscriptionDurationSeconds() {
        return getHeaders().getFirstHeader(UpnpHeader.Type.TIMEOUT, TimeoutHeader.class).getValue();
    }
}
