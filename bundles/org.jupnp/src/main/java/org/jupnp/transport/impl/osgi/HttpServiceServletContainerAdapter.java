/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.transport.impl.osgi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import org.jupnp.transport.impl.async.AsyncServlet;
import org.jupnp.transport.spi.ServletContainerAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Servlet;

/**
 * This is a servlet container adapter for an OSGi http service.
 * It is a singleton as there will be only a single OSGi http service available to register servlets on
 * 
 * @author Kai Kreuzer
 * @author Ivan Iliev - No longer a singleton
 *
 */
public class HttpServiceServletContainerAdapter implements ServletContainerAdapter {

    private final Logger logger = LoggerFactory.getLogger(HttpServiceServletContainerAdapter.class);

    private static HttpServiceServletContainerAdapter instance = null;

    protected HttpService httpService;
    private BundleContext context;
    private String contextPath;

    private HttpServiceServletContainerAdapter(HttpService httpService, BundleContext context) {
        this.httpService = httpService;
        this.context = context;
    }

    public static synchronized HttpServiceServletContainerAdapter getInstance(HttpService httpService,
            BundleContext context) {
        if (instance == null) {
            instance = new HttpServiceServletContainerAdapter(httpService, context);
        }
        return instance;
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
    public void registerServlet(String contextPath, Servlet servlet) {
        if (this.contextPath == null) {
            Dictionary<Object, Object> params = new Properties();
            try {
                logger.info("Registering UPnP callback servlet as {}", contextPath);
                if (servlet instanceof AsyncServlet) {
                    params.put("async-supported", "true");
                }
                Object httpContext = DisableAuthenticationHttpContext.create();
                ClassLoader classLoader = httpService.getClass().getClassLoader();
                try {
                    Class<?> jakartaServlet = Class.forName("jakarta.servlet.Servlet", false, classLoader);
                    httpService.getClass()
                            .getMethod("registerServlet", String.class, jakartaServlet, Dictionary.class,
                                    org.osgi.service.http.HttpContext.class)
                            .invoke(httpService, contextPath, servlet, params, httpContext);
                } catch (NoSuchMethodException e) {
                    Class<?> javaxServlet = Class.forName("javax.servlet.Servlet", false, classLoader);
                    if (!javaxServlet.isInstance(servlet)) {
                        throw new IllegalStateException(
                                "Detected javax HttpService.registerServlet overload but servlet is jakarta: "
                                        + servlet.getClass().getName(),
                                e);
                    }
                    httpService.getClass()
                            .getMethod("registerServlet", String.class, javaxServlet, Dictionary.class,
                                    org.osgi.service.http.HttpContext.class)
                            .invoke(httpService, contextPath, servlet, params, httpContext);
                }
                this.contextPath = contextPath;
            } catch (InvocationTargetException e) {
                logger.error("Failed to register UPnP servlet!", e.getTargetException());
            } catch (ReflectiveOperationException | IllegalStateException e) {
                logger.error("Failed to register UPnP servlet!", e);
            }
        }
    }

    @Override
    public void startIfNotRunning() {
    }

    @Override
    public void stopIfRunning() {
        if (this.contextPath != null) {
            httpService.unregister(this.contextPath);
            this.contextPath = null;
        }
    }
}
