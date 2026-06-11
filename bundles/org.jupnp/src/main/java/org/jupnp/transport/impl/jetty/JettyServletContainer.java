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
package org.jupnp.transport.impl.jetty;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.jupnp.transport.spi.ServletContainerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Servlet;

/**
 * A singleton wrapper of a <code>org.eclipse.jetty.server.Server</code>.
 * <p>
 * This {@link ServletContainerAdapter} starts a Jetty 12.x instance on its own and stops it.
 * Only one single context and servlet is registered, to handle UPnP requests.
 * </p>
 * <p>
 * This implementation might work on Android (not tested within JUPnP), dependencies are <code>jetty-server</code>
 * and <code>jetty-ee10-servlet</code> Maven modules.
 * </p>
 *
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - refactoring for JUPnP
 */
public class JettyServletContainer implements ServletContainerAdapter {

    private final Logger logger = LoggerFactory.getLogger(JettyServletContainer.class.getName());

    // Singleton
    public static final JettyServletContainer INSTANCE = new JettyServletContainer();

    protected Server server;

    private JettyServletContainer() {
        resetServer();
    }

    @Override
    public synchronized void setExecutorService(ExecutorService executorService) {
        // the Jetty server has its own QueuedThreadPool
    }

    @Override
    public synchronized int addConnector(String host, int port) throws IOException {
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);

        // Open immediately so we can get the assigned local port
        connector.open();

        // Only add if open() succeeded
        server.addConnector(connector);

        // starts the connector if the server is started (server starts all connectors when started)
        if (server.isStarted()) {
            try {
                connector.start();
            } catch (Exception e) {
                logger.warn("Couldn't start connector: {}", connector, e);
                throw new RuntimeException("Couldn't start connector", e);
            }
        }
        return connector.getLocalPort();
    }

    @Override
    public synchronized void registerServlet(String contextPath, Servlet servlet) {
        if (server.getHandler() != null) {
            logger.trace("Server handler is already set: {}", server.getHandler());
            return;
        }
        logger.info("Registering UPnP servlet under context path: {}", contextPath);
        ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        if (contextPath != null && !contextPath.isEmpty()) {
            servletHandler.setContextPath(contextPath);
        }
        final ServletHolder s = new ServletHolder(servlet);
        servletHandler.addServlet(s, "/*");
        server.setHandler(servletHandler);
    }

    @Override
    public synchronized void startIfNotRunning() {
        if (!server.isStarted() && !server.isStarting()) {
            logger.info("Starting Jetty server... ");
            try {
                server.start();
            } catch (Exception e) {
                logger.error("Couldn't start Jetty server", e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public synchronized void stopIfRunning() {
        if (!server.isStopped() && !server.isStopping()) {
            logger.info("Stopping Jetty server...");
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("Couldn't stop Jetty server", e);
                throw new RuntimeException(e);
            } finally {
                resetServer();
            }
        }
    }

    protected void resetServer() {
        server = new Server(); // Has its own QueuedThreadPool
    }
}
