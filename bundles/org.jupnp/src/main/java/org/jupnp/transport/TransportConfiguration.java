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

import java.util.concurrent.ExecutorService;

import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.transport.spi.StreamServerConfiguration;

/**
 * Interface to abstract a transport implementation.
 *
 * @author Victor Toni - initial contribution
 *
 * @param <SCC> StreamClientConfiguration
 * @param <SSC> StreamServerConfiguration
 */
public interface TransportConfiguration<SCC extends StreamClientConfiguration, SSC extends StreamServerConfiguration> {

    /**
     * Creates a {@link StreamClient} using the given {@link ExecutorService} for async calls.
     *
     * @param executorService used to dispatch request/response processing.
     * @return created {@link StreamClient}
     */
    StreamClient<SCC> createStreamClient(final ExecutorService executorService,
            final StreamClientConfiguration configuration);

    /**
     * Creates a {@link StreamServer} using the given {@code listenerPort}.
     *
     * @param listenerPort port to listen on
     * @return created {@link StreamServer}
     */
    StreamServer<SSC> createStreamServer(final int listenerPort);
}
