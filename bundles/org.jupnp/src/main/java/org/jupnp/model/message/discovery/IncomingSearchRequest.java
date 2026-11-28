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

import java.net.InetAddress;
import java.util.regex.Pattern;

import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.HostHeader;
import org.jupnp.model.message.header.MANHeader;
import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.types.NotificationSubtype;

/**
 * @author Christian Bauer
 */
public class IncomingSearchRequest extends IncomingDatagramMessage<UpnpRequest> {

    private static final Pattern IPV4_MULTICAST_HOST = Pattern.compile("(?:22[4-9]|23[0-9])(?:[.][0-9]{1,3}){3}");

    public IncomingSearchRequest(IncomingDatagramMessage<UpnpRequest> source) {
        super(source);
    }

    public IncomingSearchRequest(IncomingDatagramMessage<UpnpRequest> source, InetAddress localAddress) {
        super(source, localAddress);
    }

    /**
     * Returns the target named by HOST on a unicast search. Multicast searches and legacy requests
     * without a usable HOST keep the address of the receiving multicast interface.
     */
    public String getUnicastHost() {
        HostHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.HOST, HostHeader.class);
        if (header == null) {
            return null;
        }
        String host = header.getValue().getHost();
        if (host == null || host.isEmpty() || host.regionMatches(true, 0, "[ff", 0, 3)
                || IPV4_MULTICAST_HOST.matcher(host).matches()) {
            return null;
        }
        return host;
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
