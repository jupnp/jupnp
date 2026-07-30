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
package org.jupnp.transport.impl.osgi.jakarta;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jupnp.transport.impl.servlet.jakarta.ServletContainerAdapter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.runtime.HttpServiceRuntime;
import org.osgi.service.servlet.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Servlet;

/**
 * A servlet container adapter for the OSGi HTTP Whiteboard Service for Jakarta Servlet (OSGi Compendium R8,
 * {@code org.osgi.service.servlet}), the replacement for the classic {@code HttpService} used by
 * {@code org.jupnp.transport.httpservice}'s {@code HttpServiceServletContainerAdapter}. Runtimes that dropped
 * {@code HttpService} for the Whiteboard (e.g. Pax Web 10+) are exactly the ones this adapter targets.
 * <p>
 * Unlike {@code HttpService.registerServlet()}, the Whiteboard pattern has no explicit registration call at
 * all: a {@link Servlet} becomes visible to the runtime purely by being published as an OSGi service with the
 * right {@code osgi.http.whiteboard.*} properties, which a Whiteboard implementation tracks and wires up on
 * its own. This adapter is a singleton for the same reason
 * {@code HttpServiceServletContainerAdapter} is -- there's only one shared HTTP runtime to register against.
 * </p>
 * <p>
 * The registered servlet is deliberately left on the Whiteboard's default {@code ServletContextHelper}
 * (selected implicitly, no {@code osgi.http.whiteboard.context.select} property set), which by default applies
 * no authentication -- if a deployment layers its own security onto that default context, jUPnP's callback
 * servlet would need to select a dedicated, unauthenticated context instead, which this adapter does not
 * currently do.
 * </p>
 *
 * @author Holger Friedrich - jakarta.servlet/OSGi Whiteboard counterpart of HttpServiceServletContainerAdapter
 */
public class JakartaHttpServiceServletContainerAdapter implements ServletContainerAdapter {

    private final Logger logger = LoggerFactory.getLogger(JakartaHttpServiceServletContainerAdapter.class);

    private static JakartaHttpServiceServletContainerAdapter instance;

    /**
     * How long to wait for {@link HttpServiceRuntime} to publish a usable endpoint, see
     * {@link #discoverPortFromRuntime()}.
     */
    private static final long PORT_DISCOVERY_TIMEOUT_MILLIS = 5000;
    private static final long PORT_DISCOVERY_POLL_MILLIS = 100;

    /**
     * How long to wait for the Whiteboard to pick up a registration, see
     * {@link #confirmWhiteboardPickedUpRegistration}. Generous on purpose: a Whiteboard implementation's own
     * bootstrap (binding its own {@code HttpService}, creating its default context, wiring that context into
     * a real servlet container handler) is a multi-step process that runs independently of, and can easily
     * outlast, this bundle's own startup -- observed directly against Pax Web 11.x via DEBUG-level logging.
     */
    private static final long REGISTRATION_CONFIRM_TIMEOUT_MILLIS = 20000;
    private static final long REGISTRATION_CONFIRM_POLL_MILLIS = 100;

    private final BundleContext context;
    private ServiceRegistration<Servlet> registration;

    private JakartaHttpServiceServletContainerAdapter(BundleContext context) {
        this.context = context;
    }

    public static synchronized JakartaHttpServiceServletContainerAdapter getInstance(BundleContext context) {
        if (instance == null) {
            instance = new JakartaHttpServiceServletContainerAdapter(context);
        }
        return instance;
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
    }

    @Override
    public int addConnector(String host, int port) throws IOException {
        if (port == -1) {
            Integer discovered = discoverPortFromRuntime();
            if (discovered != null) {
                port = discovered;
            }
        }
        return port;
    }

    /**
     * The Whiteboard Service specification requires an {@link HttpServiceRuntime} to publish the endpoint(s)
     * it's actually listening on as its own {@link HttpServiceRuntimeConstants#HTTP_SERVICE_ENDPOINT} service
     * property -- unlike a framework property such as {@code org.osgi.service.http.port} (what the classic
     * javax.servlet {@code HttpServiceServletContainerAdapter} reads), this always reflects Pax Web's real
     * configuration, however it got configured, rather than depending on a deployment happening to also
     * mirror it as a framework property.
     * <p>
     * Polls for up to {@link #PORT_DISCOVERY_TIMEOUT_MILLIS}: the {@link HttpServiceRuntime} service can
     * register slightly before its implementation has actually finished binding a listener and updated this
     * property, so a single immediate read can race a runtime that's still starting up.
     * </p>
     *
     * @return the port from the runtime's first plain {@code http://} endpoint, or {@code null} if none could
     *         be determined within the timeout (e.g. the runtime only ever advertises a relative path
     *         because its scheme/authority aren't known, as can happen in a bridged Whiteboard implementation)
     */
    private Integer discoverPortFromRuntime() {
        long deadline = System.currentTimeMillis() + PORT_DISCOVERY_TIMEOUT_MILLIS;
        while (true) {
            Integer port = readPortFromRuntime();
            if (port != null || System.currentTimeMillis() >= deadline) {
                return port;
            }
            try {
                Thread.sleep(PORT_DISCOVERY_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
    }

    private Integer readPortFromRuntime() {
        ServiceReference<HttpServiceRuntime> reference = context.getServiceReference(HttpServiceRuntime.class);
        if (reference == null) {
            return null;
        }
        Object endpoints = reference.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT);
        for (String endpoint : asStrings(endpoints)) {
            try {
                URI uri = new URI(endpoint);
                if ("http".equalsIgnoreCase(uri.getScheme()) && uri.getPort() != -1) {
                    return uri.getPort();
                }
            } catch (URISyntaxException e) {
                logger.debug("Not a parseable HttpServiceRuntime endpoint: {}", endpoint, e);
            }
        }
        return null;
    }

    private static Iterable<String> asStrings(Object value) {
        if (value instanceof String[] array) {
            return Arrays.asList(array);
        } else if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        } else if (value instanceof String string) {
            return List.of(string);
        }
        return List.of();
    }

    @Override
    public synchronized void registerServlet(String contextPath, Servlet servlet) {
        if (registration == null) {
            logger.info("Registering UPnP callback servlet as {}", contextPath);
            Dictionary<String, Object> props = new Hashtable<>();
            props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN,
                    new String[] { contextPath, contextPath + "/*" });
            props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, Boolean.TRUE);
            registration = context.registerService(Servlet.class, servlet, props);
            confirmWhiteboardPickedUpRegistration(contextPath, props, servlet);
        }
    }

    /**
     * Most of {@link #REGISTRATION_CONFIRM_TIMEOUT_MILLIS} is spent simply waiting: a Whiteboard
     * implementation's own bootstrap (binding its own {@code HttpService}, creating its default context,
     * wiring that context into a real servlet container handler) has no package-level dependency on this
     * bundle, so nothing orders one before the other, and it can still be in progress well after this
     * bundle's own startup. As a last resort, if the deadline is reached with still no confirmation,
     * re-registering (a fresh unregister + register) gives the Whiteboard's {@code ServiceTracker} another,
     * unambiguous event to react to, in case its initial scan for already-registered services raced this
     * registration and missed it.
     */
    private void confirmWhiteboardPickedUpRegistration(String contextPath, Dictionary<String, Object> props,
            Servlet servlet) {
        long deadline = System.currentTimeMillis() + REGISTRATION_CONFIRM_TIMEOUT_MILLIS;
        while (!isPickedUpByWhiteboard(contextPath) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(REGISTRATION_CONFIRM_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (!isPickedUpByWhiteboard(contextPath)) {
            logger.debug("Whiteboard hasn't picked up the servlet registration for {} yet, re-registering",
                    contextPath);
            registration.unregister();
            registration = context.registerService(Servlet.class, servlet, props);
        }
    }

    private boolean isPickedUpByWhiteboard(String contextPath) {
        ServiceReference<HttpServiceRuntime> reference = context.getServiceReference(HttpServiceRuntime.class);
        if (reference == null) {
            return false;
        }
        HttpServiceRuntime runtime = context.getService(reference);
        if (runtime == null) {
            return false;
        }
        try {
            return runtime.calculateRequestInfoDTO(contextPath).servletDTO != null;
        } finally {
            context.ungetService(reference);
        }
    }

    @Override
    public synchronized void startIfNotRunning() {
    }

    @Override
    public synchronized void stopIfRunning() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}
