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
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.impl.jetty.JettyServletContainer;
import org.jupnp.transport.impl.jetty.JettyStreamClientImpl;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;
import org.osgi.service.component.annotations.Component;

@Component(service = UpnpServiceConfiguration.class)
public class JettyUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

    @Override
    public StreamClient<?> createStreamClient() {
        return new JettyStreamClientImpl(new StreamClientConfigurationImpl(getSyncProtocolExecutorService()));
    }

    @Override
    public StreamServer<?> createStreamServer(NetworkAddressFactory networkAddressFactory) {
        return new ServletStreamServerImpl(new ServletStreamServerConfigurationImpl(JettyServletContainer.INSTANCE,
                networkAddressFactory.getStreamListenPort()));
    }
}
