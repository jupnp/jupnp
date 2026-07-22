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
 * one transport bundle should be available at runtime. It is discovered via the {@link ServiceLoader} mechanism
 * and, as a fallback (e.g. in environments where <code>META-INF/services</code> resolution is not supported),
 * by looking up well-known implementation classes on the class path.
 * </p>
 *
 * @author Victor Toni - initial contribution
 * @author Holger Friedrich - discover transport implementations instead of hard-wiring Jetty
 */
public final class TransportConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransportConfigurationProvider.class);

    private static final String[] KNOWN_TRANSPORT_CONFIGURATIONS = { //
            "org.jupnp.transport.impl.jetty.JettyTransportConfiguration", //
            "org.jupnp.transport.impl.jetty12.Jetty12TransportConfiguration" };

    private TransportConfigurationProvider() {
        // no instance of this class
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <SCC extends StreamClientConfiguration, SSC extends StreamServerConfiguration> TransportConfiguration<SCC, SSC> getDefaultTransportConfiguration() {
        // Discover implementations announced via META-INF/services
        TransportConfiguration<SCC, SSC> transportConfiguration = null;
        int found = 0;
        try {
            Iterator<TransportConfiguration> iterator = ServiceLoader
                    .load(TransportConfiguration.class, TransportConfigurationProvider.class.getClassLoader())
                    .iterator();
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
                        "Multiple transport implementations found on the class path, using '{}'. "
                                + "Make sure only one jUPnP transport implementation is available.",
                        transportConfiguration.getClass().getName());
            } else {
                LOGGER.debug("Using transport implementation '{}'", transportConfiguration.getClass().getName());
            }
            return transportConfiguration;
        }

        // Fallback for environments where the ServiceLoader mechanism is not available (e.g. OSGi)
        for (String className : KNOWN_TRANSPORT_CONFIGURATIONS) {
            try {
                Class<?> clazz = Class.forName(className);
                LOGGER.debug("Using transport implementation '{}'", className);
                return (TransportConfiguration<SCC, SSC>) clazz.getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | NoClassDefFoundError | UnsupportedClassVersionError e) {
                LOGGER.trace("Transport implementation '{}' is not available", className);
            } catch (ReflectiveOperationException e) {
                throw new InitializationException("Failed to instantiate transport implementation " + className, e);
            }
        }

        throw new InitializationException("No transport implementation found on the class path. "
                + "Add a jUPnP transport bundle (e.g. org.jupnp:org.jupnp.transport.jetty9) as dependency.");
    }
}
