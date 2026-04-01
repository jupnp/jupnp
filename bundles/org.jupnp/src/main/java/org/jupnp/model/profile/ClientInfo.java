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
package org.jupnp.model.profile;

import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.message.header.UserAgentHeader;

/**
 * Encapsulates information about a (local) client.
 *
 * @author Christian Bauer
 */
public class ClientInfo {

    protected final UpnpHeaders requestHeaders;

    public ClientInfo() {
        this(new UpnpHeaders());
    }

    public ClientInfo(UpnpHeaders requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public UpnpHeaders getRequestHeaders() {
        return requestHeaders;
    }

    public String getRequestUserAgent() {
        return getRequestHeaders().getFirstHeaderString(UpnpHeader.Type.USER_AGENT);
    }

    public void setRequestUserAgent(String userAgent) {
        getRequestHeaders().add(UpnpHeader.Type.USER_AGENT, new UserAgentHeader(userAgent));
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") User-Agent: " + getRequestUserAgent();
    }
}
