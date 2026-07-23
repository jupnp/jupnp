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

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Triggers transport discovery exactly once, as early as possible: on activation of this bundle, the same
 * way a real consumer bundle would when it constructs its {@code UpnpService}. This timing matters:
 * calling {@link org.jupnp.transport.TransportConfigurationProvider#getDefaultTransportConfiguration()}
 * from inside a JUnit {@code @Test} method instead would run too late (well after every bundle, including
 * the transport provider, has had time to start and register with the OSGi Service Loader Mediator) and
 * would find the provider via ServiceLoader regardless of whether the real, startup-time discovery won or
 * lost the race against the provider bundle's own start-up.
 *
 * @see TransportDiscoveryTest
 */
public class TransportDiscoveryActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) {
        new DefaultUpnpServiceConfiguration().createStreamClient();
    }

    @Override
    public void stop(BundleContext context) {
        // nothing to do
    }
}
