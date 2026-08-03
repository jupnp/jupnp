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
package org.jupnp.transport.impl.jetty12;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.mock.MockProtocolFactory;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpServiceConfiguration;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.transport.spi.UpnpStream;

/**
 * Verifies that {@link Jetty12StreamServerImpl} instances for multiple bind addresses share one underlying
 * {@link Jetty12ServerContainer}/Jetty server instead of each starting its own, mirroring the Jetty 9 transport's
 * shared-server behaviour.
 *
 * @author Holger Friedrich - initial contribution
 */
class Jetty12ServerContainerTest {

    @Test
    void multipleBindAddressesShareOneServer() throws Exception {
        UpnpServiceConfiguration configuration = new MockUpnpServiceConfiguration(false, true);
        MockRouter router = new MockRouter(configuration, new MockProtocolFactory()) {
            @Override
            public void received(UpnpStream stream) {
                stream.run();
            }
        };

        StreamServer<Jetty12StreamServerConfigurationImpl> serverA = new Jetty12StreamServerImpl(
                new Jetty12StreamServerConfigurationImpl(0));
        StreamServer<Jetty12StreamServerConfigurationImpl> serverB = new Jetty12StreamServerImpl(
                new Jetty12StreamServerConfigurationImpl(0));

        try {
            InetAddress loopback = InetAddress.getByName("127.0.0.1");
            serverA.init(loopback, router);
            serverB.init(loopback, router);

            // Each address gets its own connector, on its own assigned port...
            assertTrue(serverA.getPort() > 0);
            assertTrue(serverB.getPort() > 0);
            assertNotEquals(serverA.getPort(), serverB.getPort());

            // ...but both connectors were added to the same shared Jetty server, not one server each.
            assertEquals(2, Jetty12ServerContainer.INSTANCE.server.getConnectors().length);

            configuration.getStreamServerExecutorService().execute(serverA);
            configuration.getStreamServerExecutorService().execute(serverB);
            Thread.sleep(500);

            assertTrue(Jetty12ServerContainer.INSTANCE.server.isStarted());
        } finally {
            serverA.stop();
            serverB.stop();
            Thread.sleep(200);
        }

        assertTrue(Jetty12ServerContainer.INSTANCE.server.isStopped());
    }

    @Test
    void stopIfRunningClosesConnectorOpenedBeforeServerStarted() throws Exception {
        UpnpServiceConfiguration configuration = new MockUpnpServiceConfiguration(false, true);
        MockRouter router = new MockRouter(configuration, new MockProtocolFactory());

        StreamServer<Jetty12StreamServerConfigurationImpl> server = new Jetty12StreamServerImpl(
                new Jetty12StreamServerConfigurationImpl(0));

        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        server.init(loopback, router);
        int port = server.getPort();
        assertTrue(port > 0);

        // Never calling server.run() mirrors a router whose initialization fails after init() opened a
        // connector but before the executor actually starts the shared Jetty server.
        assertTrue(Jetty12ServerContainer.INSTANCE.server.isStopped());
        assertEquals(1, Jetty12ServerContainer.INSTANCE.server.getConnectors().length);

        server.stop();

        assertEquals(0, Jetty12ServerContainer.INSTANCE.server.getConnectors().length);

        // The port must be free again -- a fresh connector can rebind it.
        StreamServer<Jetty12StreamServerConfigurationImpl> rebound = new Jetty12StreamServerImpl(
                new Jetty12StreamServerConfigurationImpl(port));
        try {
            rebound.init(loopback, router);
            assertEquals(port, rebound.getPort());
        } finally {
            rebound.stop();
        }
    }
}
