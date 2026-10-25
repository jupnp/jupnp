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
package org.jupnp.transport.javax.httpservice;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.jupnp.transport.javax.servlet.ServletContainerAdapter;
import org.jupnp.transport.javax.servlet.async.AsyncServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a servlet container adapter for an OSGi http service.
 * An instance is bound to one {@link HttpService}; when the http service is replaced (e.g. the http runtime is
 * restarted), a new adapter has to be created for the new service.
 *
 * @author Kai Kreuzer
 * @author Ivan Iliev - No longer a singleton
 * @author Holger Friedrich - Bind to a single HttpService instead of a static singleton
 *
 */
public class HttpServiceServletContainerAdapter implements ServletContainerAdapter {

    private final Logger logger = LoggerFactory.getLogger(HttpServiceServletContainerAdapter.class);

    private final HttpService httpService;
    private final BundleContext context;
    private String contextPath;

    HttpServiceServletContainerAdapter(HttpService httpService, BundleContext context) {
        this.httpService = httpService;
        this.context = context;
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
    }

    @Override
    public int addConnector(String host, int port) throws IOException {
        if (port == -1) {
            try {
                port = Integer.parseInt(context.getProperty("org.osgi.service.http.port"));
            } catch (NumberFormatException e) {
            }
        }
        return port;
    }

    @Override
    public synchronized void registerServlet(String contextPath, Servlet servlet) {
        if (this.contextPath == null) {
            Dictionary<Object, Object> params = new Properties();
            try {
                logger.info("Registering UPnP callback servlet as {}", contextPath);
                if (servlet instanceof AsyncServlet) {
                    params.put("async-supported", "true");
                }
                httpService.registerServlet(contextPath, servlet, params, new DisableAuthenticationHttpContext());
                this.contextPath = contextPath;
            } catch (ServletException | NamespaceException | IllegalStateException e) {
                logger.error("Failed to register UPnP servlet!", e);
            }
        }
    }

    @Override
    public void startIfNotRunning() {
    }

    @Override
    public synchronized void stopIfRunning() {
        if (this.contextPath != null) {
            try {
                httpService.unregister(this.contextPath);
            } finally {
                // e.g. an already stopping http runtime might refuse the unregistration, don't try again
                this.contextPath = null;
            }
        }
    }
}
