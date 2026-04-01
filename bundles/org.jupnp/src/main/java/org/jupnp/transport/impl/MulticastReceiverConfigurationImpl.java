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
package org.jupnp.transport.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jupnp.transport.spi.MulticastReceiverConfiguration;

/**
 * Settings for the default implementation.
 * 
 * @author Christian Bauer
 */
public class MulticastReceiverConfigurationImpl implements MulticastReceiverConfiguration {

    private InetAddress group;
    private int port;
    private int maxDatagramBytes;

    public MulticastReceiverConfigurationImpl(InetAddress group, int port, int maxDatagramBytes) {
        this.group = group;
        this.port = port;
        this.maxDatagramBytes = maxDatagramBytes;
    }

    /**
     * Defaults to maximum datagram size of 640 bytes (512 per UDA 1.0, 128 byte header).
     */
    public MulticastReceiverConfigurationImpl(InetAddress group, int port) {
        this(group, port, 640);
    }

    public MulticastReceiverConfigurationImpl(String group, int port, int maxDatagramBytes)
            throws UnknownHostException {
        this(InetAddress.getByName(group), port, maxDatagramBytes);
    }

    /**
     * Defaults to maximum datagram size of 640 bytes (512 per UDA 1.0, 128 byte header).
     */
    public MulticastReceiverConfigurationImpl(String group, int port) throws UnknownHostException {
        this(InetAddress.getByName(group), port, 640);
    }

    @Override
    public InetAddress getGroup() {
        return group;
    }

    public void setGroup(InetAddress group) {
        this.group = group;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public int getMaxDatagramBytes() {
        return maxDatagramBytes;
    }

    public void setMaxDatagramBytes(int maxDatagramBytes) {
        this.maxDatagramBytes = maxDatagramBytes;
    }
}
