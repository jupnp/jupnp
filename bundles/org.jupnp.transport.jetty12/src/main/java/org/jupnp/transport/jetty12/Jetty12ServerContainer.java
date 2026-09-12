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

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jupnp.transport.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton wrapper of a <code>org.eclipse.jetty.server.Server</code>, built on the Jetty 12.x core API (no
 * servlets required).
 * <p>
 * Mirrors <code>org.jupnp.transport.jetty9.Jetty9ServletContainer</code>'s pattern: one shared
 * {@link Server}/thread pool with one {@link ServerConnector} added per bind address, instead of a separate
 * Jetty server and thread pool per address. Only one context/handler is registered, to handle UPnP requests.
 * </p>
 *
 * @author Holger Friedrich - initial contribution
 */
public class Jetty12ServerContainer {

    private final Logger logger = LoggerFactory.getLogger(Jetty12ServerContainer.class);

    // Singleton
    public static final Jetty12ServerContainer INSTANCE = new Jetty12ServerContainer();

    protected Server server;

    private Jetty12ServerContainer() {
        resetServer();
    }

    /**
     * Opens and adds a connector for the given bind address, registering the shared UPnP handler on first use.
     *
     * @return the local port the connector was bound to
     */
    public synchronized int addConnector(String host, int port, Router router) throws Exception {
        if (server.getHandler() == null) {
            server.setHandler(new UpnpHandler(router));
        }

        ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);

        try {
            // Open immediately so we can get the assigned local port
            connector.open();

            // Only add if open() succeeded
            server.addConnector(connector);

            // starts the connector if the server is started (server starts all connectors when started)
            if (server.isStarted()) {
                connector.start();
            }
        } catch (Exception e) {
            // connector.open() may have already bound the server socket; if anything after that point
            // fails, close it explicitly so the port isn't leaked.
            connector.close();
            throw e;
        }
        return connector.getLocalPort();
    }

    public synchronized void startIfNotRunning() {
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

    public synchronized void stopIfRunning() {
        if (!server.isStopped() && !server.isStopping()) {
            logger.info("Stopping Jetty server...");
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("Couldn't stop Jetty server", e);
            } finally {
                resetServer();
            }
        } else if (server.getConnectors().length > 0) {
            // addConnector() binds a connector's socket immediately via NetworkConnector.open(), independent
            // of the shared server's own start/stop lifecycle. If the server itself never started -- e.g.
            // router initialization failing after one or more stream servers called init() but before the
            // executor actually runs them -- server.isStopped() is already true here, so the branch above
            // never runs and never asks the connector to close, leaking the bound port. Close any such
            // connectors directly instead.
            for (Connector connector : server.getConnectors()) {
                if (connector instanceof NetworkConnector networkConnector) {
                    networkConnector.close();
                }
            }
            resetServer();
        }
    }

    protected void resetServer() {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setName("jupnp-jetty-server");
        threadPool.setDaemon(true);
        server = new Server(threadPool);
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
