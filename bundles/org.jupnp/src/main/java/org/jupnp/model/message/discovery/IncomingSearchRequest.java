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
package org.jupnp.model.message.discovery;

import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.MANHeader;
import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.types.NotificationSubtype;

/**
 * @author Christian Bauer
 */
public class IncomingSearchRequest extends IncomingDatagramMessage<UpnpRequest> {

    public IncomingSearchRequest(IncomingDatagramMessage<UpnpRequest> source) {
        super(source);
    }

    public UpnpHeader getSearchTarget() {
        return getHeaders().getFirstHeader(UpnpHeader.Type.ST);
    }

    public Integer getMX() {
        MXHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.MX, MXHeader.class);
        if (header != null) {
            return header.getValue();
        }
        return null;
    }

    /**
     * @return <code>true</code> if this message has a MAN with
     *         value {@link org.jupnp.model.types.NotificationSubtype#DISCOVER}.
     */
    public boolean isMANSSDPDiscover() {
        MANHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.MAN, MANHeader.class);
        return header != null && header.getValue().equals(NotificationSubtype.DISCOVER.getHeaderString());
    }
}
