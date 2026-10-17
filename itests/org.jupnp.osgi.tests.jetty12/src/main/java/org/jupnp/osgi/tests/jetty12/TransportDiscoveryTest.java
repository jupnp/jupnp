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
package org.jupnp.osgi.tests.jetty12;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifies that the Jetty 12 transport bundle was discovered: {@link TransportDiscoveryComponent} declares a
 * mandatory Declarative Services reference to {@link org.jupnp.transport.TransportConfiguration}, so
 * Declarative Services itself will not activate it until the transport bundle has registered that service.
 */
public class TransportDiscoveryTest {

    @Test
    void discoversJetty12TransportViaDeclarativeServices() {
        assertTrue(TransportDiscoveryComponent.isActivated(),
                "TransportDiscoveryComponent should have activated once the transport bundle registered its "
                        + "TransportConfiguration service. Check that org.jupnp.transport.jetty12 is present and "
                        + "declares a TransportConfiguration service component.");
    }
}
