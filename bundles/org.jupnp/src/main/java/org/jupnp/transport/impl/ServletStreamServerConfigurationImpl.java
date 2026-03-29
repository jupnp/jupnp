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

import org.jupnp.transport.spi.ServletContainerAdapter;
import org.jupnp.transport.spi.StreamServerConfiguration;

/**
 * Settings for the async Servlet 2.4/3.0 implementation.
 * <p>
 * If you are trying to integrate jUPnP with an existing/running servlet container, implement
 * {@link org.jupnp.transport.spi.ServletContainerAdapter}.
 * </p>
 * 
 * @author Christian Bauer
 */
public class ServletStreamServerConfigurationImpl implements StreamServerConfiguration {

    protected ServletContainerAdapter servletContainerAdapter;
    protected int listenPort = 0;
    protected int asyncTimeoutSeconds = 60;

    /**
     * Defaults to port '0', ephemeral.
     */
    public ServletStreamServerConfigurationImpl(ServletContainerAdapter servletContainerAdapter) {
        this.servletContainerAdapter = servletContainerAdapter;
    }

    public ServletStreamServerConfigurationImpl(ServletContainerAdapter servletContainerAdapter, int listenPort) {
        this.servletContainerAdapter = servletContainerAdapter;
        this.listenPort = listenPort;
    }

    public ServletStreamServerConfigurationImpl(ServletContainerAdapter servletContainerAdapter, int listenPort,
            int asyncTimeoutSeconds) {
        this.servletContainerAdapter = servletContainerAdapter;
        this.listenPort = listenPort;
        this.asyncTimeoutSeconds = asyncTimeoutSeconds;
    }

    /**
     * @return Defaults to <code>0</code>.
     */
    @Override
    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    /**
     * The time in seconds this server wait for the {@link org.jupnp.transport.Router} to execute a
     * {@link org.jupnp.transport.spi.UpnpStream}.
     * 
     * @return The default of 60 seconds.
     */
    public int getAsyncTimeoutSeconds() {
        return asyncTimeoutSeconds;
    }

    public void setAsyncTimeoutSeconds(int asyncTimeoutSeconds) {
        this.asyncTimeoutSeconds = asyncTimeoutSeconds;
    }

    public ServletContainerAdapter getServletContainerAdapter() {
        return servletContainerAdapter;
    }

    public void setServletContainerAdapter(ServletContainerAdapter servletContainerAdapter) {
        this.servletContainerAdapter = servletContainerAdapter;
    }
}
