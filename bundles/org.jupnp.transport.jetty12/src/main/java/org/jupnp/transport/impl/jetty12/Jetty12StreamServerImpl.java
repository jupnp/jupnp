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
package org.jupnp.transport.impl.jetty12;

import java.net.InetAddress;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link StreamServer} implementation based on the Jetty 12.x core API (no servlets required).
 * <p>
 * Every instance starts its own Jetty server, listening on a single address. Incoming requests are dispatched to
 * the {@link Router} as {@link Jetty12UpnpStream}s and processed asynchronously on the stream server executor of
 * the UPnP service configuration.
 * </p>
 *
 * @author Holger Friedrich - initial contribution
 */
public class Jetty12StreamServerImpl implements StreamServer<Jetty12StreamServerConfigurationImpl> {

    private final Logger logger = LoggerFactory.getLogger(Jetty12StreamServerImpl.class);

    protected final Jetty12StreamServerConfigurationImpl configuration;
    protected Server server;
    protected ServerConnector connector;
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
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setName("jupnp-jetty-server");
            threadPool.setDaemon(true);

            server = new Server(threadPool);

            connector = new ServerConnector(server);
            connector.setHost(bindAddress.getHostAddress());
            connector.setPort(getConfiguration().getListenPort());

            logger.debug("Adding connector: {}:{}", bindAddress, getConfiguration().getListenPort());

            // Open immediately so we can get the assigned local port
            connector.open();
            localPort = connector.getLocalPort();

            server.addConnector(connector);
            server.setHandler(new UpnpHandler(router));
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
        if (server != null && !server.isStopped() && !server.isStopping()) {
            logger.info("Stopping Jetty server...");
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("Couldn't stop Jetty server", e);
            }
        }
    }

    @Override
    public void run() {
        if (!server.isStarted() && !server.isStarting()) {
            logger.info("Starting Jetty server...");
            try {
                server.start();
            } catch (Exception e) {
                logger.error("Couldn't start Jetty server", e);
                throw new RuntimeException("Couldn't start Jetty server", e);
            }
        }
    }

    protected static class UpnpHandler extends Handler.Abstract {

        private final Router router;

        public UpnpHandler(Router router) {
            super();
            this.router = router;
        }

        @Override
        public boolean handle(Request request, Response response, Callback callback) throws Exception {
            // The stream is executed asynchronously on the router's stream server executor,
            // the callback is completed when the response has been written
            router.received(new Jetty12UpnpStream(router.getProtocolFactory(), request, response, callback));
            return true;
        }
    }
}
