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

import org.jupnp.transport.javax.servlet.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.javax.servlet.ServletStreamServerImpl;
import org.jupnp.transport.spi.SharedStreamServerProvider;
import org.jupnp.transport.spi.StreamServer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

/**
 * Bridges {@link org.jupnp.OSGiUpnpServiceConfiguration} to the classic OSGi HttpService: registered as a
 * {@link SharedStreamServerProvider} service only while a {@link HttpService} is actually bound, so on
 * runtimes without one (e.g. pax-web 10 and newer, which dropped it for the Jakarta Servlet Whiteboard) this
 * bundle simply never activates, and {@code OSGiUpnpServiceConfiguration} falls through to the discovered
 * transport's own standalone {@link StreamServer} instead.
 * <p>
 * This whole bundle only makes sense where the OSGi HttpService is genuinely available -- unlike core's
 * former hard-coded HttpService path, its manifest imports {@code javax.servlet} and
 * {@code org.osgi.service.http} as regular, mandatory imports, so it simply fails to resolve (and Declarative
 * Services never introspects any of its classes) on a runtime that doesn't have them, rather than needing the
 * {@code Object}-typed-reference workaround core previously had to use for the same optional dependency.
 * </p>
 *
 * @author Holger Friedrich - extracted from OSGiUpnpServiceConfiguration's former hard-coded HttpService path
 */
@Component(service = SharedStreamServerProvider.class)
public class HttpServiceSharedStreamServerProvider implements SharedStreamServerProvider {

    private HttpService httpService;
    private BundleContext context;

    @Activate
    protected void activate(BundleContext context) {
        this.context = context;
    }

    @Reference
    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    protected void unsetHttpService(HttpService httpService) {
        this.httpService = null;
    }

    @Override
    public StreamServer<?> createStreamServer(int proxyPort) {
        return new ServletStreamServerImpl(new ServletStreamServerConfigurationImpl(
                HttpServiceServletContainerAdapter.getInstance(httpService, context), proxyPort));
    }
}
