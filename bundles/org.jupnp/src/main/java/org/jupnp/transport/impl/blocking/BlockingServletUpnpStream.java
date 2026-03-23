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
package org.jupnp.transport.impl.blocking;

import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.impl.ServletUpnpStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Implementation based on Servlet 2.4 API.
 * <p>
 * Concrete implementations must provide a connection wrapper, as this wrapper most likely has to access proprietary
 * APIs to implement connection checking.
 * </p>
 * 
 * @author Ivan Iliev - Initial Contribution and API
 */
public abstract class BlockingServletUpnpStream extends ServletUpnpStream {

    protected final FauxAsyncContext asyncContext;

    protected StreamResponseMessage responseMessage;

    protected BlockingServletUpnpStream(ProtocolFactory protocolFactory, FauxAsyncContext asyncContext) {
        super(protocolFactory);
        this.asyncContext = asyncContext;
    }

    @Override
    protected HttpServletRequest getRequest() {
        return asyncContext.getRequest();
    }

    @Override
    protected HttpServletResponse getResponse() {
        return asyncContext.getResponse();
    }

    @Override
    protected void complete() {
        try {
            asyncContext.complete();
        } catch (Exception e) {
            // If Jetty's connection, for whatever reason, is in an illegal state, this will be thrown
            // and we can "probably" ignore it. The request is complete, no matter how it ended.
            logger.info("Error calling servlet container's AsyncContext#complete() method", e);
        }
    }
}
