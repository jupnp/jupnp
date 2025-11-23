/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.protocol;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.jupnp.transport.Router;

/**
 * Tests for RetrieveRemoteDescriptors when configuration is null.
 * This simulates the race condition that can occur when OSGi unbinds
 * the configuration while async tasks are still running.
 *
 * @author GitHub Copilot
 */
class RetrieveRemoteDescriptorsNullConfigTest {

    /**
     * Test that RetrieveRemoteDescriptors handles null configuration gracefully
     * without throwing NullPointerException.
     */
    @Test
    void testDescribeWithNullConfiguration() throws Exception {
        // Create a mock UpnpService that returns null configuration
        UpnpService upnpService = new UpnpService() {
            private Registry registry = new org.jupnp.registry.RegistryImpl(this) {
                @Override
                protected org.jupnp.registry.RegistryMaintainer createRegistryMaintainer() {
                    return null; // No maintainer needed for this test
                }
            };
            private Router router = new org.jupnp.mock.MockRouter(null, null);

            @Override
            public UpnpServiceConfiguration getConfiguration() {
                return null; // Simulate the race condition
            }

            @Override
            public ControlPoint getControlPoint() {
                return null;
            }

            @Override
            public ProtocolFactory getProtocolFactory() {
                return null;
            }

            @Override
            public Registry getRegistry() {
                return registry;
            }

            @Override
            public Router getRouter() {
                return router;
            }

            @Override
            public void shutdown() {
            }

            @Override
            public void startup() {
            }
        };

        // Create a RemoteDevice with a valid identity
        RemoteDeviceIdentity identity = new RemoteDeviceIdentity(UDN.uniqueSystemIdentifier("test-device"), 1800,
                URI.create("http://localhost:8080/device.xml").toURL(), null, null);

        RemoteDevice device = new RemoteDevice(identity);

        // Create the protocol instance
        RetrieveRemoteDescriptors protocol = new RetrieveRemoteDescriptors(upnpService, device);

        // This should not throw NullPointerException
        assertDoesNotThrow(() -> protocol.run());
    }
}
