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

import org.junit.jupiter.api.BeforeAll;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.transport.impl.jetty.JettyTransportConfiguration;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;

/**
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - adapted to JUPnP
 */
class JettyServerJettyClientTest extends StreamServerClientTest {

    private static final TransportConfiguration jettyTransportConfiguration = JettyTransportConfiguration.INSTANCE;
    private static final StreamClientConfiguration sccConfiguration = new StreamClientConfigurationImpl(null, 3, 0, 0,
            0);

    @BeforeAll
    static void start() throws Exception {
        start(JettyServerJettyClientTest::createStreamServer, JettyServerJettyClientTest::createStreamClient);
    }

    public static StreamServer createStreamServer(final int port) {
        return jettyTransportConfiguration.createStreamServer(port);
    }

    public static StreamClient createStreamClient(UpnpServiceConfiguration configuration) {
        return jettyTransportConfiguration.createStreamClient(configuration.getSyncProtocolExecutorService(),
                sccConfiguration);
    }
}
