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
package org.jupnp.osgi.tests.jetty9;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.transport.TransportConfiguration;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Mirrors {@link org.jupnp.OSGiUpnpServiceConfiguration}'s pattern: consumes the transport bundle's
 * {@link TransportConfiguration} service via a mandatory Declarative Services reference, instead of
 * {@link DefaultUpnpServiceConfiguration}'s default ServiceLoader-based
 * {@link org.jupnp.transport.TransportConfigurationProvider} lookup. DS defers activation of this
 * component until the transport bundle has actually registered, so discovery is deterministic and does not
 * depend on bundle start order.
 */
@Component(service = UpnpServiceConfiguration.class)
public class Jetty9UpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

    // Named distinctly (not transportConfiguration) so it doesn't shadow DefaultUpnpServiceConfiguration's
    // private field of the same name, which would be easy to trip over during future refactors. volatile
    // since it's written by the DS bind/unbind callbacks and read from getTransportConfiguration() callers
    // on other threads.
    @SuppressWarnings("rawtypes")
    private volatile TransportConfiguration injectedTransportConfiguration;

    @Override
    @SuppressWarnings("rawtypes")
    protected TransportConfiguration getTransportConfiguration() {
        return injectedTransportConfiguration;
    }

    @Reference
    @SuppressWarnings("rawtypes")
    public void setTransportConfiguration(TransportConfiguration transportConfiguration) {
        this.injectedTransportConfiguration = transportConfiguration;
    }

    public void unsetTransportConfiguration(TransportConfiguration transportConfiguration) {
        this.injectedTransportConfiguration = null;
    }
}
