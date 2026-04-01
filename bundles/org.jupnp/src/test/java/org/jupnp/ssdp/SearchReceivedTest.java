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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpService;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleUSNHeaders;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.Constants;
import org.jupnp.model.DiscoveryOptions;
import org.jupnp.model.Namespace;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.discovery.IncomingSearchRequest;
import org.jupnp.model.message.header.DeviceTypeHeader;
import org.jupnp.model.message.header.DeviceUSNHeader;
import org.jupnp.model.message.header.EXTHeader;
import org.jupnp.model.message.header.HostHeader;
import org.jupnp.model.message.header.MANHeader;
import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.MaxAgeHeader;
import org.jupnp.model.message.header.RootDeviceHeader;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.message.header.ServiceTypeHeader;
import org.jupnp.model.message.header.ServiceUSNHeader;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.message.header.USNRootDeviceHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.protocol.async.ReceivingSearch;
import org.jupnp.util.URIUtil;

class SearchReceivedTest {

    @Test
    void receivedSearchAll() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice localDevice = SampleData.createLocalDevice();
        LocalDevice embeddedDevice = localDevice.getEmbeddedDevices()[0];
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new STAllHeader());
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(10, upnpService.getRouter().getOutgoingDatagramMessages().size());

        for (OutgoingDatagramMessage msg : upnpService.getRouter().getOutgoingDatagramMessages()) {
            // SampleData.debugMsg(msg);
            assertSearchResponseBasics(upnpService.getConfiguration().getNamespace(), msg, localDevice);
        }

        SampleUSNHeaders.assertUSNHeaders(upnpService.getRouter().getOutgoingDatagramMessages(), localDevice,
                embeddedDevice, UpnpHeader.Type.ST);
    }

    @Test
    void receivedSearchRoot() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new RootDeviceHeader());
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(1, upnpService.getRouter().getOutgoingDatagramMessages().size());

        assertSearchResponseBasics(upnpService.getConfiguration().getNamespace(),
                upnpService.getRouter().getOutgoingDatagramMessages().get(0), localDevice);
        assertEquals(new RootDeviceHeader().getString(), upnpService.getRouter().getOutgoingDatagramMessages().get(0)
                .getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString());
        assertEquals(localDevice.getIdentity().getUdn().toString() + USNRootDeviceHeader.ROOT_DEVICE_SUFFIX,
                upnpService.getRouter().getOutgoingDatagramMessages().get(0).getHeaders()
                        .getFirstHeader(UpnpHeader.Type.USN).getString());
    }

    @Test
    void receivedSearchUDN() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new UDNHeader(localDevice.getIdentity().getUdn()));
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(1, upnpService.getRouter().getOutgoingDatagramMessages().size());

        assertSearchResponseBasics(upnpService.getConfiguration().getNamespace(),
                upnpService.getRouter().getOutgoingDatagramMessages().get(0), localDevice);
        assertEquals(new UDNHeader(localDevice.getIdentity().getUdn()).getString(), upnpService.getRouter()
                .getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString());
        assertEquals(new UDNHeader(localDevice.getIdentity().getUdn()).getString(), upnpService.getRouter()
                .getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.USN).getString());
    }

    @Test
    void receivedSearchDeviceType() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new DeviceTypeHeader(localDevice.getType()));
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(1, upnpService.getRouter().getOutgoingDatagramMessages().size());

        assertSearchResponseBasics(upnpService.getConfiguration().getNamespace(),
                upnpService.getRouter().getOutgoingDatagramMessages().get(0), localDevice);
        assertEquals(new DeviceTypeHeader(localDevice.getType()).getString(), upnpService.getRouter()
                .getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString());
        assertEquals(new DeviceUSNHeader(localDevice.getIdentity().getUdn(), localDevice.getType()).getString(),
                upnpService.getRouter().getOutgoingDatagramMessages().get(0).getHeaders()
                        .getFirstHeader(UpnpHeader.Type.USN).getString());
    }

    @Test
    void receivedSearchServiceType() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice localDevice = SampleData.createLocalDevice();
        Service service = localDevice.getServices()[0];
        upnpService.getRegistry().addDevice(localDevice);

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new ServiceTypeHeader(service.getServiceType()));
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(1, upnpService.getRouter().getOutgoingDatagramMessages().size());

        assertSearchResponseBasics(upnpService.getConfiguration().getNamespace(),
                upnpService.getRouter().getOutgoingDatagramMessages().get(0), localDevice);
        assertEquals(new ServiceTypeHeader(service.getServiceType()).getString(), upnpService.getRouter()
                .getOutgoingDatagramMessages().get(0).getHeaders().getFirstHeader(UpnpHeader.Type.ST).getString());
        assertEquals(new ServiceUSNHeader(localDevice.getIdentity().getUdn(), service.getServiceType()).getString(),
                upnpService.getRouter().getOutgoingDatagramMessages().get(0).getHeaders()
                        .getFirstHeader(UpnpHeader.Type.USN).getString());
    }

    @Test
    void receivedInvalidST() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(0, upnpService.getRouter().getOutgoingDatagramMessages().size());
    }

    @Test
    void receivedInvalidMX() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new STAllHeader());
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(0, upnpService.getRouter().getOutgoingDatagramMessages().size());
    }

    @Test
    void receivedNonAdvertised() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice localDevice = SampleData.createLocalDevice();

        // Disable advertising
        upnpService.getRegistry().addDevice(localDevice, new DiscoveryOptions(false));

        IncomingSearchRequest searchMsg = createRequestMessage();
        searchMsg.getHeaders().add(UpnpHeader.Type.MAN, new MANHeader(NotificationSubtype.DISCOVER.getHeaderString()));
        searchMsg.getHeaders().add(UpnpHeader.Type.MX, new MXHeader(1));
        searchMsg.getHeaders().add(UpnpHeader.Type.ST, new STAllHeader());
        searchMsg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());

        ReceivingSearch prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(0, upnpService.getRouter().getOutgoingDatagramMessages().size());

        // Enable advertising
        upnpService.getRegistry().setDiscoveryOptions(localDevice.getIdentity().getUdn(), new DiscoveryOptions(true));

        prot = createProtocol(upnpService, searchMsg);
        prot.run();

        assertEquals(10, upnpService.getRouter().getOutgoingDatagramMessages().size());
    }

    protected ReceivingSearch createProtocol(UpnpService upnpService, IncomingSearchRequest searchMsg) {
        return new ReceivingSearch(upnpService, searchMsg);
    }

    protected void assertSearchResponseBasics(Namespace namespace, UpnpMessage msg, LocalDevice rootDevice) {
        assertEquals(new MaxAgeHeader(rootDevice.getIdentity().getMaxAgeSeconds()).getString(),
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE).getString());
        assertEquals(new EXTHeader().getString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.EXT).getString());
        assertEquals(URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), namespace.getDescriptorPath(rootDevice))
                .toString(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION).getString());
        assertNotNull(msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER).getString());
    }

    protected IncomingSearchRequest createRequestMessage() throws UnknownHostException {
        return new IncomingSearchRequest(new IncomingDatagramMessage<>(new UpnpRequest(UpnpRequest.Method.MSEARCH),
                InetAddress.getByName("127.0.0.1"), Constants.UPNP_MULTICAST_PORT, InetAddress.getByName("127.0.0.1")));
    }
}
