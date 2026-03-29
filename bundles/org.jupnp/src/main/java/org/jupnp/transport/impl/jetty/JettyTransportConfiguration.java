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
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;

/**
 * Implementation of {@link TransportConfiguration} for Jetty HTTP components.
 *
 * @author Victor Toni - initial contribution
 */
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
