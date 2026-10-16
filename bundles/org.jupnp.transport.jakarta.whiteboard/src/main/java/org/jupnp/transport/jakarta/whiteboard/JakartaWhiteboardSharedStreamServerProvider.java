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

import org.jupnp.transport.jakarta.servlet.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.jakarta.servlet.ServletStreamServerImpl;
import org.jupnp.transport.spi.SharedStreamServerProvider;
import org.jupnp.transport.spi.StreamServer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.servlet.runtime.HttpServiceRuntime;
import org.osgi.service.servlet.runtime.HttpServiceRuntimeConstants;

/**
 * Bridges {@link org.jupnp.OSGiUpnpServiceConfiguration} to the OSGi HTTP Whiteboard Service for Jakarta
 * Servlet (OSGi Compendium R8, {@code org.osgi.service.servlet}) -- the jakarta.servlet counterpart of
 * {@code org.jupnp.transport.javax.httpservice}'s {@code HttpServiceSharedStreamServerProvider}, which bridges to
 * the older, javax.servlet-only, classic {@code HttpService}.
 * <p>
 * Registered as a {@link SharedStreamServerProvider} service only while an {@link HttpServiceRuntime} is
 * actually bound -- that's the Whiteboard specification's own marker service for "an implementation is
 * genuinely running", the direct equivalent of depending on {@code HttpService} itself in the classic bridge.
 * Without this gate, the bundle would resolve and register just as happily on a runtime with no Whiteboard
 * implementation at all (the {@code org.osgi.service.servlet} dependency is a plain API jar, always
 * resolvable), but registering a servlet as a plain OSGi service with nobody consuming it would silently
 * accept jUPnP's callback registration and then never receive any HTTP traffic.
 * </p>
 * <p>
 * The reference additionally requires the runtime to already advertise a plain {@code http://} endpoint (see
 * {@link #HTTP_ENDPOINT_FILTER}), so {@link JakartaWhiteboardServletContainerAdapter#addConnector} can read the
 * port immediately instead of waiting for it. Waiting would block the thread activating jUPnP, and during a
 * bundle refresh (e.g. an add-on installation in openHAB) that same thread is the one that restarts Pax Web
 * afterwards, so the endpoint could never appear while it waited. With the filter, DS simply activates this
 * component (and, through {@code OSGiUpnpServiceConfiguration}'s greedy reference, restarts jUPnP on the shared
 * server) once Pax Web has published its endpoint, and deactivates it again if Pax Web goes away.
 * </p>
 * <p>
 * This bundle and {@code org.jupnp.transport.javax.httpservice} can both be installed at once without conflict: at
 * most one of them ever activates, since a given OSGi HTTP runtime implementation registers either
 * {@code HttpService}, {@code HttpServiceRuntime}, or (commonly, e.g. Pax Web 8-10) both.
 * </p>
 *
 * @author Holger Friedrich - jakarta.servlet/OSGi Whiteboard counterpart of HttpServiceSharedStreamServerProvider
 */
@Component(service = SharedStreamServerProvider.class)
public class JakartaWhiteboardSharedStreamServerProvider implements SharedStreamServerProvider {

    /**
     * Only matches an {@link HttpServiceRuntime} that already advertises at least one plain {@code http://}
     * endpoint -- the same thing {@link JakartaWhiteboardServletContainerAdapter#addConnector} reads the port
     * from. A multi-valued {@code String[]} property matches if any of its values does.
     */
    static final String HTTP_ENDPOINT_FILTER = "(" + HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT + "=http://*)";

    private BundleContext context;

    @Activate
    protected void activate(BundleContext context) {
        this.context = context;
    }

    @Reference(target = HTTP_ENDPOINT_FILTER)
    protected void setHttpServiceRuntime(HttpServiceRuntime httpServiceRuntime) {
        // Only used as an activation gate (see class Javadoc); JakartaWhiteboardServletContainerAdapter
        // looks up the HttpServiceRuntime service reference itself (via BundleContext) when it needs to
        // discover Pax Web's actual bound port, rather than being handed this particular binding.
    }

    protected void unsetHttpServiceRuntime(HttpServiceRuntime httpServiceRuntime) {
    }

    @Override
    public StreamServer<?> createStreamServer(int proxyPort) {
        return new ServletStreamServerImpl(new ServletStreamServerConfigurationImpl(
                JakartaWhiteboardServletContainerAdapter.getInstance(context), proxyPort));
    }
}
