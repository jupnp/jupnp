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
package org.jupnp.transport.jetty9;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Jetty9ServletContainer}.
 *
 * @author Holger Friedrich - Initial contribution
 */
class Jetty9ServletContainerTest {

    /**
     * addConnector() binds the port immediately. If the shared server is never started (e.g. router
     * initialization failing before the stream servers are executed), stopIfRunning() still has to release it.
     */
    @Test
    void stopIfRunningClosesConnectorOpenedBeforeServerStarted() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        int port = Jetty9ServletContainer.INSTANCE.addConnector(loopback.getHostAddress(), 0);
        assertTrue(port > 0);

        Jetty9ServletContainer.INSTANCE.stopIfRunning();

        // the port must be free again
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(loopback, port));
        }
    }
}
