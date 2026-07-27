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
package org.jupnp.transport;

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the central place to discover the transport implementation to be used.
 * <p>
 * Transport implementations are no longer part of the core library, they are provided by separate transport
 * bundles (e.g. <code>org.jupnp.transport.jetty9</code> or <code>org.jupnp.transport.jetty12</code>). Exactly
 * one transport bundle should be available at runtime, discovered via the {@link ServiceLoader} mechanism. In
 * OSGi, this relies on an OSGi Service Loader Mediator (e.g. Apache Aries SPI Fly) being installed to bridge
 * discovery across bundles; this bundle's manifest declares that requirement.
 * </p>
 * <p>
 * There is deliberately no reflective fallback: {@link org.jupnp.OSGiUpnpServiceConfiguration}, the shipped
 * OSGi component, does not use this class at all (it consumes {@link TransportConfiguration} via a mandatory
 * Declarative Services reference instead). A missing or misconfigured Service Loader Mediator should
 * therefore surface as a clear configuration error here, not be silently papered over.
 * </p>
 *
 * @author Victor Toni - initial contribution
 * @author Holger Friedrich - discover transport implementations instead of hard-wiring Jetty
 */
public final class TransportConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportConfigurationProvider.class);

    // In OSGi, an OSGi Service Loader Mediator (e.g. Apache Aries SPI Fly) may still be registering the
    // provider bundle when this class is first used (e.g. on eager bundle activation): jUPnP's own bundle
    // and the mediator/provider both start concurrently, with no guaranteed ordering between them. This
    // brief retry only covers millisecond-scale jitter, not the general case: the gap can span however
    // long the framework takes to work through the other bundles in its startup sequence first, which a
    // short bounded retry cannot reliably close without imposing that same delay on every deployment that
    // has no mediator at all. Consumers that need deterministic behavior in OSGi should prefer
    // org.jupnp.OSGiUpnpServiceConfiguration, which consumes TransportConfiguration via a mandatory
    // Declarative Services reference instead of this class, so activation is deferred until the transport
    // bundle has actually registered.
    private static final int SERVICE_LOADER_RETRY_ATTEMPTS = 5;
    private static final long SERVICE_LOADER_RETRY_DELAY_MILLIS = 50;

    private TransportConfigurationProvider() {
        // no instance of this class
    }

    public static <SCC extends StreamClientConfiguration, SSC extends StreamServerConfiguration> TransportConfiguration<SCC, SSC> getDefaultTransportConfiguration() {
        for (int attempt = 1; attempt <= SERVICE_LOADER_RETRY_ATTEMPTS; attempt++) {
            TransportConfiguration<SCC, SSC> transportConfiguration = discoverViaServiceLoader();
            if (transportConfiguration != null) {
                return transportConfiguration;
            }
            if (attempt < SERVICE_LOADER_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(SERVICE_LOADER_RETRY_DELAY_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        throw new InitializationException("No transport implementation found via ServiceLoader. "
                + "Add a jUPnP transport bundle (e.g. org.jupnp:org.jupnp.transport.jetty9) as a dependency. "
                + "In OSGi, also ensure an OSGi Service Loader Mediator (e.g. Apache Aries SPI Fly) is "
                + "installed and started.");
    }

    // Discover implementations announced via META-INF/services
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static <SCC extends StreamClientConfiguration, SSC extends StreamServerConfiguration> TransportConfiguration<SCC, SSC> discoverViaServiceLoader() {
        TransportConfiguration<SCC, SSC> transportConfiguration = null;
        int found = 0;
        try {
            // Deliberately the single-argument ServiceLoader.load(Class) overload, not
            // load(Class, ClassLoader): an OSGi Service Loader Mediator (e.g. Apache Aries SPI Fly)
            // bridges cross-bundle discovery by weaving the thread context classloader around calls
            // to this overload. Passing an explicit classloader bypasses that weaving entirely and
            // silently defeats ServiceLoader-based discovery in OSGi, even with correct
            // Provide-/Require-Capability headers in place.
            Iterator<TransportConfiguration> iterator = ServiceLoader.load(TransportConfiguration.class).iterator();
            while (iterator.hasNext()) {
                try {
                    TransportConfiguration<SCC, SSC> candidate = iterator.next();
                    found++;
                    if (transportConfiguration == null) {
                        transportConfiguration = candidate;
                    }
                } catch (ServiceConfigurationError | LinkageError e) {
                    LOGGER.debug("Ignoring a transport implementation that could not be loaded via ServiceLoader", e);
                }
            }
        } catch (ServiceConfigurationError | LinkageError e) {
            LOGGER.debug("ServiceLoader discovery of transport implementations failed", e);
        }
        if (transportConfiguration != null) {
            if (found > 1) {
                LOGGER.warn(
                        "Multiple transport implementations found via ServiceLoader, using '{}'. "
                                + "Make sure only one jUPnP transport implementation is available.",
                        transportConfiguration.getClass().getName());
            } else {
                LOGGER.debug("Using transport implementation '{}' found via ServiceLoader",
                        transportConfiguration.getClass().getName());
            }
        }
        return transportConfiguration;
    }
}
