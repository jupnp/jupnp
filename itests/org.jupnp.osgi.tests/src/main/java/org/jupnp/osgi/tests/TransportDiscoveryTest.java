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
package org.jupnp.osgi.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.jupnp.transport.TransportConfigurationProvider;
import org.jupnp.transport.TransportConfigurationProvider.DiscoveryMechanism;

/**
 * Verifies that the transport bundle was actually discovered via {@link java.util.ServiceLoader}, bridged
 * by the OSGi Service Loader Mediator (Apache Aries SPI Fly, see itest.bndrun startlevels), rather than
 * silently taking the reflective fallback.
 * <p>
 * This intentionally does <em>not</em> call {@link TransportConfigurationProvider#getDefaultTransportConfiguration()}
 * itself: a fresh call made from a test method runs late, well after every bundle has had time to start
 * and register with the mediator, so it would find the provider via ServiceLoader regardless of whether
 * the real, production-relevant discovery (triggered eagerly once, when the {@code UpnpServiceConfiguration}
 * OSGi component first activates) won or lost the startup race. Extending {@link BaseIntegration} instead
 * forces that real, singleton discovery to have already happened via {@code setup()}, and this test then
 * only reads back {@link TransportConfigurationProvider#getLastDiscoveryMechanism()} without disturbing it.
 */
public class TransportDiscoveryTest extends BaseIntegration {

    @Test
    void discoversJettyTransportViaServiceLoader() {
        assertNotNull(getUpnpService(), "UpnpService should have been resolved by BaseIntegration#setup()");
        assertEquals(DiscoveryMechanism.SERVICE_LOADER, TransportConfigurationProvider.getLastDiscoveryMechanism(),
                "Expected the OSGi Service Loader Mediator (Apache Aries SPI Fly) to bridge ServiceLoader-based "
                        + "discovery of the transport bundle; got the reflective fallback instead. Check bundle "
                        + "start ordering (see itest.bndrun startlevels) and the Provide-/Require-Capability "
                        + "headers on org.jupnp and org.jupnp.transport.jetty9.");
    }
}
