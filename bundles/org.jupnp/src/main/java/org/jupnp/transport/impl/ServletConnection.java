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
package org.jupnp.transport.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jupnp.model.message.Connection;

import jakarta.servlet.http.HttpServletRequest;

/**
 * UPNP Connection implementation using a {@link HttpServletRequest}.
 * 
 * @author Christian Bauer
 * 
 */
public class ServletConnection implements Connection {

    protected HttpServletRequest request;

    public ServletConnection(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public boolean isOpen() {
        return isConnectionOpen(getRequest());
    }

    @Override
    public InetAddress getRemoteAddress() {
        try {
            return InetAddress.getByName(getRequest().getRemoteAddr());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InetAddress getLocalAddress() {
        try {
            return InetAddress.getByName(getRequest().getLocalAddr());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Override this method if you can check, at a low level, if the client
     * connection is still open for the given request. This will likely require
     * access to proprietary APIs of your servlet container to obtain the
     * socket/channel for the given request.
     * 
     * @return By default <code>true</code>.
     */
    protected boolean isConnectionOpen(HttpServletRequest request) {
        return true;
    }
}
