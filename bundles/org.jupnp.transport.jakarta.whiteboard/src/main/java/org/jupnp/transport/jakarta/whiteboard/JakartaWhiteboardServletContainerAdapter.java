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
package org.jupnp.transport.jakarta.whiteboard;

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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
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
public class JakartaWhiteboardServletContainerAdapter implements ServletContainerAdapter {

    private final Logger logger = LoggerFactory.getLogger(JakartaWhiteboardServletContainerAdapter.class);

    private static JakartaWhiteboardServletContainerAdapter instance;

    /**
     * How long to wait for the Whiteboard to pick up a registration, see
     * {@link #checkWhiteboardPickedUpRegistration}. Generous on purpose: a Whiteboard implementation's own
     * bootstrap (binding its own {@code HttpService}, creating its default context, wiring that context into
     * a real servlet container handler) is a multi-step process that runs independently of, and can easily
     * outlast, this bundle's own startup -- observed directly against Pax Web 11.x via DEBUG-level logging,
     * and confirmed against a real openHAB startup where the full 20s of an earlier, smaller timeout was
     * consumed before falling back: a live deployment has many other Whiteboard resources (REST API, web UI,
     * addon UIs) registering at the same time, so jUPnP's callback servlet can end up queued behind all of
     * that. The wait runs on a background thread (see {@link #registerServlet}), never on the caller's.
     */
    private static final long REGISTRATION_CONFIRM_TIMEOUT_MILLIS = 60000;
    private static final long REGISTRATION_CONFIRM_POLL_MILLIS = 250;

    /**
     * Grace period to wait for confirmation after the fallback re-register in
     * {@link #checkWhiteboardPickedUpRegistration}.
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
    private Thread registrationCheck;

    // Package-private rather than private so a test can construct its own instance directly, bypassing the
    // getInstance() singleton, without needing a real BundleContext/HttpServiceRuntime.
    JakartaWhiteboardServletContainerAdapter(BundleContext context) {
        this.context = context;
    }

    public static synchronized JakartaWhiteboardServletContainerAdapter getInstance(BundleContext context) {
        if (instance == null) {
            instance = new JakartaWhiteboardServletContainerAdapter(context);
        }
        return instance;
    }

    @Override
    public void setExecutorService(ExecutorService executorService) {
    }

    /**
     * Reads the port once and never waits for it. {@link JakartaWhiteboardSharedStreamServerProvider} is only
     * active while the runtime already advertises an {@code http://} endpoint, so the port is normally there.
     * Polling here would block the thread activating jUPnP, and during a bundle refresh that thread may be the
     * one that has to restart Pax Web before the endpoint can appear at all.
     *
     * @throws IOException if {@code port} is {@code -1} (bind to whatever the Whiteboard is actually
     *             listening on) and no usable endpoint can be read from {@link HttpServiceRuntime}, e.g. because
     *             the runtime went away concurrently -- returning the caller's original, unbound {@code -1}
     *             instead would violate {@code ServletContainerAdapter.addConnector()}'s "actual registered local
     *             port" contract silently, leaving the router to advertise a callback URL nothing is actually
     *             listening on
     */
    @Override
    public int addConnector(String host, int port) throws IOException {
        if (port == -1) {
            Integer discovered = readPortFromRuntime();
            if (discovered == null) {
                throw new IOException("Could not read a usable http:// endpoint from HttpServiceRuntime's "
                        + HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT + " property -- check that a Jakarta "
                        + "HTTP Whiteboard implementation (e.g. Pax Web) is running");
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
     *
     * <p>
     * Looks up the runtimes with the same {@link JakartaWhiteboardSharedStreamServerProvider#HTTP_ENDPOINT_FILTER}
     * the provider's activation gate uses, so with more than one {@link HttpServiceRuntime} registered this reads
     * the one that actually satisfied the gate, not just whichever ranks highest.
     * </p>
     *
     * @return the port from the first plain {@code http://} endpoint of a matching runtime, or {@code null} if
     *         there is none (e.g. the runtime only advertises a relative path because its scheme/authority aren't
     *         known, as can happen in a bridged Whiteboard implementation)
     */
    private Integer readPortFromRuntime() {
        Collection<ServiceReference<HttpServiceRuntime>> references;
        try {
            references = context.getServiceReferences(HttpServiceRuntime.class,
                    JakartaWhiteboardSharedStreamServerProvider.HTTP_ENDPOINT_FILTER);
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Invalid HttpServiceRuntime endpoint filter", e);
        }
        for (ServiceReference<HttpServiceRuntime> reference : references) {
            Integer port = readPort(reference);
            if (port != null) {
                return port;
            }
        }
        return null;
    }

    private Integer readPort(ServiceReference<HttpServiceRuntime> reference) {
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
            startRegistrationCheck(contextPath, props, servlet);
        }
    }

    /**
     * Runs {@link #checkWhiteboardPickedUpRegistration} on its own thread instead of the caller's: the caller is
     * usually jUPnP's DS activation, and during a bundle refresh that same thread may still have to restart the
     * Whiteboard implementation (or its extender) before it can pick anything up -- waiting here would only end
     * once the timeout expired. Must be called holding this adapter's lock.
     */
    private void startRegistrationCheck(String contextPath, Dictionary<String, Object> props, Servlet servlet) {
        Thread check = new Thread(() -> checkWhiteboardPickedUpRegistration(contextPath, props, servlet),
                "jupnp-whiteboard-registration-check");
        check.setDaemon(true);
        registrationCheck = check;
        check.start();
    }

    // Package-private so a test can wait for this specific adapter's check to end.
    synchronized Thread getRegistrationCheck() {
        return registrationCheck;
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
     * <p>
     * Runs on the thread started by {@link #startRegistrationCheck}, so it can no longer fail the router's
     * initialization: if the Whiteboard still hasn't picked up the registration after the re-register and the
     * {@link #REREGISTER_CONFIRM_TIMEOUT_MILLIS} grace period, it logs an error plus the Whiteboard's own
     * diagnostics, and leaves the registration in place in case the Whiteboard picks it up later.
     * {@link #stopIfRunning} cancels it by interrupting the thread.
     * </p>
     */
    private void checkWhiteboardPickedUpRegistration(String contextPath, Dictionary<String, Object> props,
            Servlet servlet) {
        try {
            if (waitUntilPickedUpByWhiteboard(contextPath, REGISTRATION_CONFIRM_TIMEOUT_MILLIS)) {
                logger.debug("Whiteboard picked up the servlet registration for {}", contextPath);
                return;
            }
            synchronized (this) {
                if (!isCurrentRegistrationCheck()) {
                    return;
                }
                logger.warn(
                        "Whiteboard hasn't picked up the servlet registration for {} within {}ms, re-registering as a last resort",
                        contextPath, REGISTRATION_CONFIRM_TIMEOUT_MILLIS);
                // Clear the field before unregistering, so if registerService() below throws (e.g. this bundle
                // is stopping), it doesn't keep pointing at an already-unregistered registration that
                // stopIfRunning() would then try to unregister a second time.
                ServiceRegistration<Servlet> previous = registration;
                registration = null;
                previous.unregister();
                registration = context.registerService(Servlet.class, servlet, props);
            }
            if (waitUntilPickedUpByWhiteboard(contextPath, REREGISTER_CONFIRM_TIMEOUT_MILLIS)) {
                logger.info("Whiteboard picked up the re-registered servlet for {}", contextPath);
                return;
            }
            Long servletServiceId;
            Long contextServiceId;
            synchronized (this) {
                if (!isCurrentRegistrationCheck()) {
                    return;
                }
                servletServiceId = serviceId(registration);
                contextServiceId = serviceId(contextRegistration);
            }
            logger.error("Whiteboard still hasn't picked up the UPnP callback servlet registration for {} after "
                    + "re-registering -- jUPnP will not receive any HTTP callbacks (e.g. GENA events) until it does",
                    contextPath);
            logRegistrationDiagnostics(contextPath, servletServiceId, contextServiceId);
        } catch (InterruptedException e) {
            // Cancelled by stopIfRunning()
            Thread.currentThread().interrupt();
        } catch (IllegalStateException e) {
            // This bundle's context became invalid, i.e. the bundle is stopping
            logger.debug("Stopped checking the Whiteboard registration for {}", contextPath, e);
        } catch (RuntimeException e) {
            // E.g. the Whiteboard failing while it's being restarted -- log it here, since an exception escaping
            // this thread would only reach stderr
            logger.warn("Failed to check whether the Whiteboard picked up the servlet registration for {}", contextPath,
                    e);
        } finally {
            synchronized (this) {
                if (registrationCheck == Thread.currentThread()) {
                    registrationCheck = null;
                }
            }
        }
    }

    private boolean waitUntilPickedUpByWhiteboard(String contextPath, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!isPickedUpByWhiteboard(contextPath)) {
            if (System.currentTimeMillis() >= deadline) {
                return false;
            }
            Thread.sleep(REGISTRATION_CONFIRM_POLL_MILLIS);
        }
        return true;
    }

    /**
     * @return whether the calling thread is still the active {@link #registrationCheck}, i.e. hasn't been
     *         superseded or cancelled by {@link #stopIfRunning} -- must be called holding this adapter's lock
     */
    private boolean isCurrentRegistrationCheck() {
        return registrationCheck == Thread.currentThread() && registration != null;
    }

    /**
     * Dumps the Whiteboard's own view of what happened to the registration when
     * {@link #checkWhiteboardPickedUpRegistration} gives up: {@code calculateRequestInfoDTO} only tells us the servlet
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
    private void logRegistrationDiagnostics(String contextPath, Long servletServiceId, Long contextServiceId) {
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
            for (FailedServletDTO failed : runtimeDTO.failedServletDTOs) {
                if (servletServiceId != null && failed.serviceId == servletServiceId) {
                    logger.warn(
                            "Whiteboard reports the {} servlet (service.id {}, patterns {}) as a FAILED "
                                    + "registration, reason code {}",
                            contextPath, failed.serviceId, Arrays.asList(failed.patterns), failed.failureReason);
                }
            }
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
        if (registrationCheck != null) {
            registrationCheck.interrupt();
            registrationCheck = null;
        }
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
