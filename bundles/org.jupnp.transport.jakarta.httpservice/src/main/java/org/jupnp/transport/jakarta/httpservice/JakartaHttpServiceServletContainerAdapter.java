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
package org.jupnp.transport.jakarta.httpservice;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jupnp.transport.jakarta.servlet.ServletContainerAdapter;
import org.jupnp.transport.spi.InitializationException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.context.ServletContextHelper;
import org.osgi.service.servlet.runtime.HttpServiceRuntime;
import org.osgi.service.servlet.runtime.HttpServiceRuntimeConstants;
import org.osgi.service.servlet.runtime.dto.FailedServletContextDTO;
import org.osgi.service.servlet.runtime.dto.FailedServletDTO;
import org.osgi.service.servlet.runtime.dto.RuntimeDTO;
import org.osgi.service.servlet.runtime.dto.ServletContextDTO;
import org.osgi.service.servlet.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Servlet;

/**
 * A servlet container adapter for the OSGi HTTP Whiteboard Service for Jakarta Servlet (OSGi Compendium R8,
 * {@code org.osgi.service.servlet}), the replacement for the classic {@code HttpService} used by
 * {@code org.jupnp.transport.javax.httpservice}'s {@code HttpServiceServletContainerAdapter}. Runtimes that dropped
 * {@code HttpService} for the Whiteboard (e.g. Pax Web 10+) are exactly the ones this adapter targets.
 * <p>
 * Unlike {@code HttpService.registerServlet()}, the Whiteboard pattern has no explicit registration call at
 * all: a {@link Servlet} becomes visible to the runtime purely by being published as an OSGi service with the
 * right {@code osgi.http.whiteboard.*} properties, which a Whiteboard implementation tracks and wires up on
 * its own. This adapter is a singleton for the same reason
 * {@code HttpServiceServletContainerAdapter} is -- there's only one shared HTTP runtime to register against.
 * </p>
 * <p>
 * The registered servlet is deliberately kept off the Whiteboard's default {@code ServletContextHelper}: this
 * adapter publishes its own, uniquely-named {@link ServletContextHelper} (see {@link #CONTEXT_NAME}), mounted
 * at the UPnP callback path itself rather than {@code /}, and selects it explicitly via
 * {@code osgi.http.whiteboard.context.select}. Two separate problems ruled out sharing a context with anything
 * else, both observed directly against a real openHAB Pax Web runtime: (1) a deployment can have more than one
 * {@code ServletContextHelper} registered under the name {@code "default"} at the same time -- openHAB's own
 * {@code HttpService} compatibility layer registers additional per-consumer default-like contexts alongside
 * Pax Web's own single true default -- so a name-based selector targeting {@code "default"} explicitly matched
 * all of them just as ambiguously as relying on implicit default-context selection did; and (2) even after
 * moving to a uniquely-named context, mounting it at the same path ({@code /}) as several other contexts
 * (Pax Web's own default, openHAB's compatibility default, the REST API's context, the UI's context) meant Pax
 * Web's own logs reported the servlet as successfully added, while the endpoint was still unreachable over
 * real HTTP -- the underlying Jetty server apparently only dispatches live traffic to one context handler per
 * identical context path, regardless of how many Whiteboard-level contexts nominally share it. Owning both a
 * unique name and a unique path -- one nothing else in the deployment would plausibly also use -- sidesteps
 * both problems at once. The context applies no authentication (the same default behavior the shared default
 * context has) -- if a deployment needs its own authentication on the UPnP callback path, that would need to
 * be layered onto this context specifically.
 * </p>
 *
 * @author Holger Friedrich - jakarta.servlet/OSGi Whiteboard counterpart of HttpServiceServletContainerAdapter
 */
public class JakartaHttpServiceServletContainerAdapter implements ServletContainerAdapter {

    private final Logger logger = LoggerFactory.getLogger(JakartaHttpServiceServletContainerAdapter.class);

    private static JakartaHttpServiceServletContainerAdapter instance;

    /**
     * How long to wait for the Whiteboard to pick up a registration, see
     * {@link #confirmWhiteboardPickedUpRegistration}. Generous on purpose: a Whiteboard implementation's own
     * bootstrap (binding its own {@code HttpService}, creating its default context, wiring that context into
     * a real servlet container handler) is a multi-step process that runs independently of, and can easily
     * outlast, this bundle's own startup -- observed directly against Pax Web 11.x via DEBUG-level logging,
     * and confirmed against a real openHAB startup where the full 20s of an earlier, smaller timeout was
     * consumed before falling back: a live deployment has many other Whiteboard resources (REST API, web UI,
     * addon UIs) registering at the same time, so jUPnP's callback servlet can end up queued behind all of
     * that.
     */
    private static final long REGISTRATION_CONFIRM_TIMEOUT_MILLIS = 60000;
    private static final long REGISTRATION_CONFIRM_POLL_MILLIS = 250;

    /**
     * How long to wait for {@link HttpServiceRuntime} to publish a usable endpoint, see
     * {@link #discoverPortFromRuntime(long, long)}. Deliberately the same budget as
     * {@link #REGISTRATION_CONFIRM_TIMEOUT_MILLIS}: both are waiting on the same underlying event -- the
     * Whiteboard implementation finishing its own startup -- so there's no reason for this one to give up
     * sooner and have {@link #addConnector(String, int)} throw before the Whiteboard genuinely had a chance
     * to start.
     */
    private static final long PORT_DISCOVERY_TIMEOUT_MILLIS = REGISTRATION_CONFIRM_TIMEOUT_MILLIS;
    private static final long PORT_DISCOVERY_POLL_MILLIS = REGISTRATION_CONFIRM_POLL_MILLIS;

    /**
     * Grace period to wait for confirmation after the fallback re-register in
     * {@link #confirmWhiteboardPickedUpRegistration}.
     */
    private static final long REREGISTER_CONFIRM_TIMEOUT_MILLIS = 5000;

    /**
     * {@code osgi.http.whiteboard.context.name} of the dedicated {@link ServletContextHelper} this adapter
     * publishes and selects, chosen to be one nothing else in a deployment would plausibly also register a
     * context under -- see the class Javadoc for why relying on the Whiteboard's shared default context isn't
     * safe.
     */
    private static final String CONTEXT_NAME = "org.jupnp";

    private final BundleContext context;
    private ServiceRegistration<Servlet> registration;
    private ServiceRegistration<ServletContextHelper> contextRegistration;

    // Package-private rather than private so a test can construct its own instance directly, bypassing the
    // getInstance() singleton, without needing a real BundleContext/HttpServiceRuntime.
    JakartaHttpServiceServletContainerAdapter(BundleContext context) {
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
        return addConnector(host, port, PORT_DISCOVERY_TIMEOUT_MILLIS, PORT_DISCOVERY_POLL_MILLIS);
    }

    /**
     * Package-private overload taking the discovery timeout/poll interval explicitly, so a test can exercise
     * the timeout-exceeded path (including the {@link IOException} below) without waiting out the real
     * {@link #PORT_DISCOVERY_TIMEOUT_MILLIS} budget. {@link #addConnector(String, int)} delegates here with
     * the real constants.
     *
     * @throws IOException if {@code port} is {@code -1} (bind to whatever the Whiteboard is actually
     *             listening on) and no usable endpoint could be discovered from {@link HttpServiceRuntime}
     *             within {@code timeoutMillis} -- returning the caller's original, unbound {@code -1} instead
     *             would violate {@code ServletContainerAdapter.addConnector()}'s "actual registered local
     *             port" contract silently, leaving the router to advertise a callback URL nothing is actually
     *             listening on
     */
    int addConnector(String host, int port, long timeoutMillis, long pollMillis) throws IOException {
        if (port == -1) {
            Integer discovered = discoverPortFromRuntime(timeoutMillis, pollMillis);
            if (discovered == null) {
                throw new IOException("Could not discover a usable HTTP endpoint from HttpServiceRuntime's "
                        + HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT + " property within " + timeoutMillis
                        + "ms -- check that a Jakarta HTTP Whiteboard implementation (e.g. Pax Web) is installed "
                        + "and has finished starting");
            }
            port = discovered;
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
     * Polls for up to {@code timeoutMillis}: the {@link HttpServiceRuntime} service can register slightly
     * before its implementation has actually finished binding a listener and updated this property, so a
     * single immediate read can race a runtime that's still starting up.
     * </p>
     *
     * @return the port from the runtime's first plain {@code http://} endpoint, or {@code null} if none could
     *         be determined within the timeout (e.g. the runtime only ever advertises a relative path
     *         because its scheme/authority aren't known, as can happen in a bridged Whiteboard implementation)
     */
    private Integer discoverPortFromRuntime(long timeoutMillis, long pollMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            Integer port = readPortFromRuntime();
            if (port != null || System.currentTimeMillis() >= deadline) {
                return port;
            }
            try {
                Thread.sleep(pollMillis);
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
            registerOwnServletContext(contextPath);
            logger.info("Registering UPnP callback servlet as {}", contextPath);
            Dictionary<String, Object> props = new Hashtable<>();
            // Relative to the dedicated context registered below, which already carries contextPath as its
            // own osgi.http.whiteboard.context.path -- "/*" alone matches both the context root (the exact
            // external URL contextPath, no trailing segment) and everything under it, equivalent to the
            // {contextPath, contextPath + "/*"} pair this used before moving off the shared default context.
            props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, new String[] { "/*" });
            props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED, Boolean.TRUE);
            props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT,
                    "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=" + CONTEXT_NAME + ")");
            registration = context.registerService(Servlet.class, servlet, props);
            confirmWhiteboardPickedUpRegistration(contextPath, props, servlet);
        }
    }

    /**
     * Publishes the dedicated {@link ServletContextHelper} the callback servlet is registered against, at
     * {@code contextPath} itself rather than {@code "/"} -- see the class Javadoc for why sharing a context
     * path with other contexts isn't safe either, even with a uniquely-named context. The anonymous subclass
     * inherits every method's documented default behavior (no authentication, resource lookups against this
     * bundle), identical to what the default context itself does.
     */
    private void registerOwnServletContext(String contextPath) {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME, CONTEXT_NAME);
        props.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH, contextPath);
        contextRegistration = context.registerService(ServletContextHelper.class, new ServletContextHelper() {
        }, props);
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
     *
     * @throws InitializationException if the Whiteboard still hasn't picked up the registration after the
     *             re-register and {@link #REREGISTER_CONFIRM_TIMEOUT_MILLIS} grace period -- see
     *             {@link #logFinalRegistrationOutcome} -- or if interrupted while waiting: an unconfirmed
     *             registration must not be allowed to look like success just because the wait was cut short
     */
    private void confirmWhiteboardPickedUpRegistration(String contextPath, Dictionary<String, Object> props,
            Servlet servlet) {
        long deadline = System.currentTimeMillis() + REGISTRATION_CONFIRM_TIMEOUT_MILLIS;
        while (!isPickedUpByWhiteboard(contextPath) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(REGISTRATION_CONFIRM_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                unregisterServletAndContext();
                throw new InitializationException(
                        "Interrupted while waiting for the Whiteboard to pick up the servlet registration for "
                                + contextPath,
                        e);
            }
        }
        if (!isPickedUpByWhiteboard(contextPath)) {
            logger.warn(
                    "Whiteboard hasn't picked up the servlet registration for {} within {}ms, re-registering as a last resort",
                    contextPath, REGISTRATION_CONFIRM_TIMEOUT_MILLIS);
            registration.unregister();
            registration = context.registerService(Servlet.class, servlet, props);
            logFinalRegistrationOutcome(contextPath);
        }
    }

    /**
     * After the last-resort re-register, either confirms success or gives up for good: at this point the
     * Whiteboard has had the full {@link #REGISTRATION_CONFIRM_TIMEOUT_MILLIS} budget plus another
     * {@link #REREGISTER_CONFIRM_TIMEOUT_MILLIS} to react to an unambiguous, freshly re-registered service --
     * continuing to run with the callback servlet unconfirmed would leave the router enabled and advertising a
     * callback endpoint that isn't actually reachable, turning a clear startup problem into GENA callbacks
     * silently going missing later. Unregisters the never-confirmed servlet/context first, so a later retry
     * (e.g. the router restarting this stream server) doesn't find {@link #registration} already non-null and
     * silently skip re-registering in {@link #registerServlet}.
     *
     * @throws InitializationException if the Whiteboard still hasn't picked up the registration, or if
     *             interrupted while waiting -- see {@link #confirmWhiteboardPickedUpRegistration}
     */
    private void logFinalRegistrationOutcome(String contextPath) {
        long deadline = System.currentTimeMillis() + REREGISTER_CONFIRM_TIMEOUT_MILLIS;
        while (!isPickedUpByWhiteboard(contextPath) && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(REGISTRATION_CONFIRM_POLL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                unregisterServletAndContext();
                throw new InitializationException(
                        "Interrupted while waiting for the Whiteboard to pick up the re-registered servlet for "
                                + contextPath,
                        e);
            }
        }
        if (isPickedUpByWhiteboard(contextPath)) {
            logger.info("Whiteboard picked up the re-registered servlet for {}", contextPath);
            return;
        }
        logger.warn("Whiteboard still hasn't picked up the servlet registration for {} after re-registering -- "
                + "failing initialization", contextPath);
        logRegistrationDiagnostics(contextPath);
        unregisterServletAndContext();
        throw new InitializationException("Whiteboard never picked up the UPnP callback servlet registration for "
                + contextPath + " -- see the Whiteboard diagnostics logged above");
    }

    /**
     * Dumps the Whiteboard's own view of what happened to the registration when
     * {@link #logFinalRegistrationOutcome} gives up: {@code calculateRequestInfoDTO} only tells us the servlet
     * wasn't matched, not why -- a pattern conflict with another webapp, a missing default context, or similar
     * is reported by the Whiteboard as a {@code FailedServletDTO} (or, if the dedicated
     * {@link ServletContextHelper} itself was the one rejected, a {@code FailedServletContextDTO}) instead,
     * which this surfaces explicitly so a deployment-specific conflict doesn't look identical to a merely slow
     * Whiteboard.
     * <p>
     * Correlated by {@code service.id} rather than by pattern/path: the servlet's own registered pattern is
     * just {@code "/*"} (see {@link #registerServlet}), relative to the dedicated context rather than an
     * absolute {@code contextPath}-prefixed pattern, so a failed registration for this servlet wouldn't have a
     * pattern starting with {@code contextPath} to match against. The context's own {@code service.id} is
     * checked separately (matched with the context's own {@code service.id} property first, its
     * {@link #CONTEXT_NAME} as a fallback) since a context failing to register is a distinct failure mode from
     * its servlet failing.
     * </p>
     */
    private void logRegistrationDiagnostics(String contextPath) {
        ServiceReference<HttpServiceRuntime> reference = context.getServiceReference(HttpServiceRuntime.class);
        if (reference == null) {
            logger.warn("No HttpServiceRuntime service available to diagnose the {} registration", contextPath);
            return;
        }
        HttpServiceRuntime runtime = context.getService(reference);
        if (runtime == null) {
            logger.warn("Could not obtain the HttpServiceRuntime service to diagnose the {} registration", contextPath);
            return;
        }
        try {
            RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
            Long servletServiceId = serviceId(registration);
            for (FailedServletDTO failed : runtimeDTO.failedServletDTOs) {
                if (servletServiceId != null && failed.serviceId == servletServiceId) {
                    logger.warn(
                            "Whiteboard reports the {} servlet (service.id {}, patterns {}) as a FAILED "
                                    + "registration, reason code {}",
                            contextPath, failed.serviceId, Arrays.asList(failed.patterns), failed.failureReason);
                }
            }
            Long contextServiceId = serviceId(contextRegistration);
            for (FailedServletContextDTO failed : runtimeDTO.failedServletContextDTOs) {
                if ((contextServiceId != null && failed.serviceId == contextServiceId)
                        || CONTEXT_NAME.equals(failed.name)) {
                    logger.warn(
                            "Whiteboard reports the {} servlet context (name {}, service.id {}) as a FAILED "
                                    + "registration, reason code {}",
                            contextPath, failed.name, failed.serviceId, failed.failureReason);
                }
            }
            StringBuilder contexts = new StringBuilder();
            for (ServletContextDTO contextDTO : runtimeDTO.servletContextDTOs) {
                contexts.append(contextDTO.name).append('=').append(contextDTO.contextPath).append(' ');
            }
            logger.warn("Whiteboard's currently known servlet contexts: [{}]", contexts.toString().trim());
        } finally {
            context.ungetService(reference);
        }
    }

    /**
     * @return the {@code service.id} of {@code registration}'s underlying service, or {@code null} if
     *         {@code registration} is itself {@code null} -- lets {@link #logRegistrationDiagnostics} correlate
     *         a {@code FailedServletDTO}/{@code FailedServletContextDTO} back to the specific service this
     *         adapter registered, rather than matching on a pattern or path that may not even be present on the
     *         failed DTO.
     */
    private static Long serviceId(ServiceRegistration<?> registration) {
        if (registration == null) {
            return null;
        }
        Object value = registration.getReference().getProperty(Constants.SERVICE_ID);
        return value instanceof Long id ? id : null;
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
        unregisterServletAndContext();
    }

    private void unregisterServletAndContext() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
        if (contextRegistration != null) {
            contextRegistration.unregister();
            contextRegistration = null;
        }
    }
}
