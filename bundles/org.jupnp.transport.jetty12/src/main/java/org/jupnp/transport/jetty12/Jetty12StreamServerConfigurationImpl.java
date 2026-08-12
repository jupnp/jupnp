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
package org.jupnp.transport.jetty12;

import org.jupnp.transport.spi.StreamServerConfiguration;

/**
 * Settings for the Jetty 12.x {@link org.jupnp.transport.spi.StreamServer} implementation.
 *
 * @author Holger Friedrich - initial contribution
 */
public class Jetty12StreamServerConfigurationImpl implements StreamServerConfiguration {

    private int listenPort;

    public Jetty12StreamServerConfigurationImpl(int listenPort) {
        this.listenPort = listenPort;
    }

    /**
     * @return Defaults to <code>0</code>, which means an ephemeral port is used.
     */
    @Override
    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }
}
