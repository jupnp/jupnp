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
package org.jupnp.transport.jetty12;

import java.net.InetAddress;

import org.jupnp.transport.Router;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link StreamServer} implementation based on the Jetty 12.x core API (no servlets required).
 * <p>
 * Every bind address gets its own instance of this class, but they all share one underlying Jetty
 * {@link org.eclipse.jetty.server.Server}/thread pool via {@link Jetty12ServerContainer}, adding one
 * {@link org.eclipse.jetty.server.ServerConnector} per address instead of running a separate Jetty server per
 * address -- mirroring how the Jetty 9 transport's <code>Jetty9ServletContainer</code> shares a single server
 * across bind addresses. Incoming requests are dispatched to the {@link Router} as {@link Jetty12UpnpStream}s
 * and processed asynchronously on the stream server executor of the UPnP service configuration.
 * </p>
 *
 * @author Holger Friedrich - initial contribution
 */
public class Jetty12StreamServerImpl implements StreamServer<Jetty12StreamServerConfigurationImpl> {

    private final Logger logger = LoggerFactory.getLogger(Jetty12StreamServerImpl.class);

    protected final Jetty12StreamServerConfigurationImpl configuration;
    protected int localPort;

    public Jetty12StreamServerImpl(Jetty12StreamServerConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    @Override
    public Jetty12StreamServerConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public synchronized void init(InetAddress bindAddress, Router router) throws InitializationException {
        try {
            logger.debug("Adding connector: {}:{}", bindAddress, getConfiguration().getListenPort());
            localPort = Jetty12ServerContainer.INSTANCE.addConnector(bindAddress.getHostAddress(),
                    getConfiguration().getListenPort(), router);
        } catch (Exception e) {
            throw new InitializationException("Could not initialize " + getClass().getSimpleName(), e);
        }
    }

    @Override
    public synchronized int getPort() {
        return localPort;
    }

    @Override
    public synchronized void stop() {
        Jetty12ServerContainer.INSTANCE.stopIfRunning();
    }

    @Override
    public void run() {
        Jetty12ServerContainer.INSTANCE.startIfNotRunning();
    }
}
