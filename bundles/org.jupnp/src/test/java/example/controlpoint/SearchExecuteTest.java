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
package example.controlpoint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.header.DeviceTypeHeader;
import org.jupnp.model.message.header.HostHeader;
import org.jupnp.model.message.header.MANHeader;
import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.RootDeviceHeader;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.message.header.ServiceTypeHeader;
import org.jupnp.model.message.header.UDADeviceTypeHeader;
import org.jupnp.model.message.header.UDAServiceTypeHeader;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.protocol.async.SendingSearch;

class SearchExecuteTest {

    @Test
    void searchAll() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        upnpService.getControlPoint().search(new STAllHeader());

        assertMessages(upnpService, new STAllHeader());
    }

    @Test
    void searchUDN() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        UDN udn = new UDN(UUID.randomUUID());
        upnpService.getControlPoint().search(new UDNHeader(udn));

        assertMessages(upnpService, new UDNHeader(udn));
    }

    @Test
    void searchDeviceType() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        UDADeviceType udaType = new UDADeviceType("BinaryLight");
        upnpService.getControlPoint().search(new UDADeviceTypeHeader(udaType));

        assertMessages(upnpService, new UDADeviceTypeHeader(udaType));

        upnpService.getRouter().getOutgoingDatagramMessages().clear();

        DeviceType type = new DeviceType("org-mydomain", "MyDeviceType", 1);
        upnpService.getControlPoint().search(new DeviceTypeHeader(type));

        assertMessages(upnpService, new DeviceTypeHeader(type));
    }

    @Test
    void searchServiceType() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        UDAServiceType udaType = new UDAServiceType("SwitchPower");
        upnpService.getControlPoint().search(new UDAServiceTypeHeader(udaType));

        assertMessages(upnpService, new UDAServiceTypeHeader(udaType));

        upnpService.getRouter().getOutgoingDatagramMessages().clear();

        ServiceType type = new ServiceType("org-mydomain", "MyServiceType", 1);
        upnpService.getControlPoint().search(new ServiceTypeHeader(type));

        assertMessages(upnpService, new ServiceTypeHeader(type));
    }

    @Test
    void searchRoot() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        upnpService.getControlPoint().search(new RootDeviceHeader());
        assertMessages(upnpService, new RootDeviceHeader());
    }

    @Test
    void searchDefaults() {
        SendingSearch search = new SendingSearch(new MockUpnpService());
        assertEquals(new STAllHeader().getString(), search.getSearchTarget().getString());
    }

    @Test
    void searchInvalidST() {
        assertThrows(IllegalArgumentException.class, () -> new SendingSearch(new MockUpnpService(), new MXHeader()));
    }

    protected void assertMessages(MockUpnpService upnpService, UpnpHeader header) {
        assertEquals(3, upnpService.getRouter().getOutgoingDatagramMessages().size());
        for (UpnpMessage msg : upnpService.getRouter().getOutgoingDatagramMessages()) {
            assertSearchMessage(msg, header);
        }
    }

    protected void assertSearchMessage(UpnpMessage msg, UpnpHeader searchTarget) {
        assertEquals(new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()).getString(),
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAN).getString());
        assertEquals(new MXHeader().getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.MX).getString());
        assertEquals(searchTarget.getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString());
        assertEquals(new HostHeader().getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.HOST).getString());
    }
}
