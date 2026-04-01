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
package org.jupnp.ssdp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleDeviceRoot;
import org.jupnp.data.SampleUSNHeaders;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.protocol.async.SendingNotificationAlive;
import org.jupnp.protocol.async.SendingNotificationByebye;

class AdvertisementTest {

    @Test
    void sendAliveMessages() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice rootDevice = SampleData.createLocalDevice();
        LocalDevice embeddedDevice = rootDevice.getEmbeddedDevices()[0];

        SendingNotificationAlive prot = new SendingNotificationAlive(upnpService, rootDevice);
        prot.run();

        for (OutgoingDatagramMessage msg : upnpService.getRouter().getOutgoingDatagramMessages()) {
            assertAliveMsgBasics(msg);
            // SampleData.debugMsg(msg);
        }

        SampleUSNHeaders.assertUSNHeaders(upnpService.getRouter().getOutgoingDatagramMessages(), rootDevice,
                embeddedDevice, UpnpHeader.Type.NT);
    }

    @Test
    void sendByebyeMessages() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice rootDevice = SampleData.createLocalDevice();
        LocalDevice embeddedDevice = rootDevice.getEmbeddedDevices()[0];

        SendingNotificationByebye prot = new SendingNotificationByebye(upnpService, rootDevice);
        prot.run();

        for (OutgoingDatagramMessage msg : upnpService.getRouter().getOutgoingDatagramMessages()) {
            assertByebyeMsgBasics(msg);
            // SampleData.debugMsg(msg);
        }

        SampleUSNHeaders.assertUSNHeaders(upnpService.getRouter().getOutgoingDatagramMessages(), rootDevice,
                embeddedDevice, UpnpHeader.Type.NT);
    }

    protected void assertAliveMsgBasics(UpnpMessage msg) {
        assertEquals(NotificationSubtype.ALIVE, msg.getHeaders().getFirstHeader(UpnpHeader.Type.NTS).getValue());
        assertEquals(SampleDeviceRoot.getDeviceDescriptorURL().toString(),
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION).getValue().toString());
        assertEquals(1800, msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE).getValue());
        assertEquals(new ServerClientTokens(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER).getValue());
    }

    protected void assertByebyeMsgBasics(UpnpMessage msg) {
        assertEquals(NotificationSubtype.BYEBYE, msg.getHeaders().getFirstHeader(UpnpHeader.Type.NTS).getValue());
    }
}
