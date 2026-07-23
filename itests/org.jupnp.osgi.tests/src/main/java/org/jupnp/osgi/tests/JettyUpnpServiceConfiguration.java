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
import org.osgi.service.component.annotations.Component;

/**
 * Relies on {@link DefaultUpnpServiceConfiguration#getTransportConfiguration()} to discover the transport
 * implementation via {@link org.jupnp.transport.TransportConfigurationProvider} (ServiceLoader, bridged by
 * an OSGi Service Loader Mediator, with a reflective fallback), instead of hard-wiring a specific transport
 * implementation. Note that this is the path taken by non-DS consumers of jUPnP; the actual shipped OSGi
 * component, {@link org.jupnp.OSGiUpnpServiceConfiguration}, instead consumes the transport via a mandatory
 * Declarative Services reference and does not go through {@code TransportConfigurationProvider} at all.
 */
@Component(service = UpnpServiceConfiguration.class)
public class JettyUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {
}
