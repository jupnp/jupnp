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
public class JettyUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

    @SuppressWarnings("rawtypes")
    private TransportConfiguration transportConfiguration;

    @Override
    @SuppressWarnings("rawtypes")
    protected TransportConfiguration getTransportConfiguration() {
        return transportConfiguration;
    }

    @Reference
    @SuppressWarnings("rawtypes")
    public void setTransportConfiguration(TransportConfiguration transportConfiguration) {
        this.transportConfiguration = transportConfiguration;
    }

    public void unsetTransportConfiguration(TransportConfiguration transportConfiguration) {
        this.transportConfiguration = null;
    }
}
