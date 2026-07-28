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

import java.util.concurrent.ExecutorService;

import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.servlet.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.servlet.ServletStreamServerImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;
import org.osgi.service.component.annotations.Component;

/**
 * Implementation of {@link TransportConfiguration} for Jetty HTTP components.
 * <p>
 * Also registered as a {@link TransportConfiguration} OSGi service via Declarative Services, so that
 * {@link org.jupnp.OSGiUpnpServiceConfiguration}'s mandatory reference to it defers activation until this
 * bundle has actually started, rather than racing ServiceLoader-based discovery against bundle start order.
 * </p>
 *
 * @author Victor Toni - initial contribution
 */
@Component(service = TransportConfiguration.class)
public class JettyTransportConfiguration implements TransportConfiguration {

    public static final TransportConfiguration INSTANCE = new JettyTransportConfiguration();

    @Override
    public StreamClient createStreamClient(final ExecutorService executorService,
            final StreamClientConfiguration configuration) {
        StreamClientConfigurationImpl clientConfiguration = new StreamClientConfigurationImpl(executorService,
                configuration.getTimeoutSeconds(), configuration.getLogWarningSeconds(),
                configuration.getRetryAfterSeconds(), configuration.getRetryIterations());

        return new JettyStreamClientImpl(clientConfiguration);
    }

    @Override
    public StreamServer createStreamServer(final int listenerPort) {
        return new ServletStreamServerImpl(
                new ServletStreamServerConfigurationImpl(JettyServletContainer.INSTANCE, listenerPort));
    }
}
