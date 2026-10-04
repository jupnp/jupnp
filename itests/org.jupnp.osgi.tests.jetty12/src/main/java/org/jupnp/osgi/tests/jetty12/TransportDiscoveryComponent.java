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

import org.jupnp.transport.TransportConfiguration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Declares a mandatory reference to the transport bundle's {@link TransportConfiguration} service, the same
 * way {@link org.jupnp.OSGiUpnpServiceConfiguration} does. This component provides no service of its own, so
 * per the Declarative Services default it activates immediately once its bundle starts and the reference is
 * satisfied; it never activates at all if the reference is never satisfied. Either way, Declarative Services
 * handles that outcome safely (unlike a raw {@code BundleActivator} throwing from
 * {@code start(BundleContext)}, which can leave the OSGi framework in a bad state), so discovery is
 * deterministic and does not depend on bundle start order.
 *
 * @see TransportDiscoveryTest
 */
@Component
public class TransportDiscoveryComponent {

    private static volatile boolean activated;

    @Reference
    @SuppressWarnings("rawtypes")
    void setTransportConfiguration(TransportConfiguration transportConfiguration) {
        // presence of the mandatory reference above is what matters for this test
    }

    void unsetTransportConfiguration(@SuppressWarnings("rawtypes") TransportConfiguration transportConfiguration) {
        // no-op
    }

    @Activate
    void activate() {
        activated = true;
    }

    static boolean isActivated() {
        return activated;
    }
}
