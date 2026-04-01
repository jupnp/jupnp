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
package org.jupnp.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpOperation;
import org.jupnp.model.message.header.DeviceTypeHeader;
import org.jupnp.model.message.header.DeviceUSNHeader;
import org.jupnp.model.message.header.RootDeviceHeader;
import org.jupnp.model.message.header.ServiceTypeHeader;
import org.jupnp.model.message.header.ServiceUSNHeader;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.message.header.USNRootDeviceHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;

/**
 * @author Christian Bauer
 */
public class SampleUSNHeaders {

    public static void assertUSNHeaders(List<OutgoingDatagramMessage> msgs, LocalDevice rootDevice,
            LocalDevice embeddedDevice, UpnpHeader.Type ntstHeaderType) {

        // See the tables in UDA 1.0 section 1.1.2

        boolean gotRootDeviceFirstMsg = false;
        boolean gotRootDeviceSecondMsg = false;
        boolean gotRootDeviceThirdMsg = false;

        boolean gotEmbeddedDeviceFirstMsg = false;
        boolean gotEmbeddedDeviceSecondMsg = false;

        boolean gotFirstServiceMsg = false;
        boolean gotSecondServiceMsg = false;

        for (UpnpMessage<UpnpOperation> msg : msgs) {

            if (msg.getHeaders().getFirstHeader(ntstHeaderType, RootDeviceHeader.class) != null) {
                assertEquals(new USNRootDeviceHeader(rootDevice.getIdentity().getUdn()).getString(),
                        msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN, USNRootDeviceHeader.class).getString());
                gotRootDeviceFirstMsg = true;
            }

            UDNHeader foundUDN = msg.getHeaders().getFirstHeader(ntstHeaderType, UDNHeader.class);
            if (foundUDN != null
                    && foundUDN.getString().equals(new UDNHeader(rootDevice.getIdentity().getUdn()).getString())) {
                assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString(),
                        msg.getHeaders().getFirstHeader(ntstHeaderType).getString());
                gotRootDeviceSecondMsg = true;
            }

            if (foundUDN != null
                    && foundUDN.getString().equals(new UDNHeader(embeddedDevice.getIdentity().getUdn()).getString())) {
                assertEquals(msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString(),
                        msg.getHeaders().getFirstHeader(ntstHeaderType).getString());
                gotEmbeddedDeviceFirstMsg = true;

            }

            DeviceTypeHeader foundDeviceNTST = msg.getHeaders().getFirstHeader(ntstHeaderType, DeviceTypeHeader.class);
            if (foundDeviceNTST != null
                    && foundDeviceNTST.getString().equals(new DeviceTypeHeader(rootDevice.getType()).getString())) {
                assertEquals(new DeviceUSNHeader(rootDevice.getIdentity().getUdn(), rootDevice.getType()).getString(),
                        msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN, DeviceUSNHeader.class).getString());
                gotRootDeviceThirdMsg = true;
            }

            if (foundDeviceNTST != null
                    && foundDeviceNTST.getString().equals(new DeviceTypeHeader(embeddedDevice.getType()).getString())) {
                assertEquals(
                        new DeviceUSNHeader(embeddedDevice.getIdentity().getUdn(), embeddedDevice.getType())
                                .getString(),
                        msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN, DeviceUSNHeader.class).getString());
                gotEmbeddedDeviceSecondMsg = true;
            }

            ServiceTypeHeader foundServiceNTST = msg.getHeaders().getFirstHeader(ntstHeaderType,
                    ServiceTypeHeader.class);
            if (foundServiceNTST != null && foundServiceNTST.getString()
                    .equals(new ServiceTypeHeader(SampleServiceOne.getThisServiceType()).getString())) {
                assertEquals(
                        new ServiceUSNHeader(rootDevice.getIdentity().getUdn(), SampleServiceOne.getThisServiceType())
                                .getString(),
                        msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN, ServiceUSNHeader.class).getString());
                gotFirstServiceMsg = true;
            }

            if (foundServiceNTST != null && foundServiceNTST.getString()
                    .equals(new ServiceTypeHeader(SampleServiceTwo.getThisServiceType()).getString())) {
                assertEquals(
                        new ServiceUSNHeader(rootDevice.getIdentity().getUdn(), SampleServiceTwo.getThisServiceType())
                                .getString(),
                        msg.getHeaders().getFirstHeader(UpnpHeader.Type.USN, ServiceUSNHeader.class).getString());
                gotSecondServiceMsg = true;
            }
        }

        assertTrue(gotRootDeviceFirstMsg);
        assertTrue(gotRootDeviceSecondMsg);
        assertTrue(gotRootDeviceThirdMsg);

        assertTrue(gotEmbeddedDeviceFirstMsg);
        assertTrue(gotEmbeddedDeviceSecondMsg);

        assertTrue(gotFirstServiceMsg);
        assertTrue(gotSecondServiceMsg);
    }
}
