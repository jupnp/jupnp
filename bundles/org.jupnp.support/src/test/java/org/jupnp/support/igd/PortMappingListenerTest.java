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
package org.jupnp.support.igd;

import static org.junit.jupiter.api.Assertions.assertSame;

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.support.model.PortMapping;

class PortMappingListenerTest {

    @Test
    void discoversVersionTwoInternetGatewayDevice() throws Exception {
        final RemoteService connectionService = new RemoteService(new UDAServiceType("WANIPConnection", 2),
                new UDAServiceId("WANIPConnection"), URI.create("service.xml"), URI.create("control"),
                URI.create("events"));
        final RemoteDevice connectionDevice = new RemoteDevice(identity("connection"),
                new UDADeviceType("WANConnectionDevice", 2), new DeviceDetails("WAN connection"),
                new RemoteService[] { connectionService });
        final RemoteDevice wanDevice = new RemoteDevice(identity("wan"), new UDADeviceType("WANDevice", 2),
                new DeviceDetails("WAN device"), new RemoteService[0], new RemoteDevice[] { connectionDevice });
        final RemoteDevice gateway = new RemoteDevice(identity("gateway"),
                new UDADeviceType("InternetGatewayDevice", 2), new DeviceDetails("Internet gateway"),
                new RemoteService[0], new RemoteDevice[] { wanDevice });

        final Service<?, ?> discovered = new PortMappingListener(new PortMapping()).discoverConnectionService(gateway);

        assertSame(connectionService, discovered);
    }

    private RemoteDeviceIdentity identity(final String name) throws Exception {
        final URL descriptorUrl = new URL("http://127.0.0.1/" + name + ".xml");
        final InetAddress localAddress = InetAddress.getLoopbackAddress();
        return new RemoteDeviceIdentity(new UDN("uuid:" + name), 1800, descriptorUrl, null, localAddress);
    }
}
