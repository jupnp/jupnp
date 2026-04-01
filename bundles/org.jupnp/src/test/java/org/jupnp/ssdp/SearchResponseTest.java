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
import org.jupnp.data.SampleDeviceRoot;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.Constants;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.discovery.IncomingSearchResponse;
import org.jupnp.model.message.header.EXTHeader;
import org.jupnp.model.message.header.HostHeader;
import org.jupnp.model.message.header.LocationHeader;
import org.jupnp.model.message.header.MaxAgeHeader;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.message.header.USNRootDeviceHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;

/**
 * @author Christian Bauer
 */
class SearchResponseTest {

    @Test
    void receivedValidResponse() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        RemoteDevice rd = SampleData.createRemoteDevice();

        IncomingSearchResponse msg = createResponseMessage(new STAllHeader());
        msg.getHeaders().add(UpnpHeader.Type.USN, new USNRootDeviceHeader(rd.getIdentity().getUdn()));
        msg.getHeaders().add(UpnpHeader.Type.LOCATION, new LocationHeader(SampleDeviceRoot.getDeviceDescriptorURL()));
        msg.getHeaders().add(UpnpHeader.Type.MAX_AGE, new MaxAgeHeader(rd.getIdentity().getMaxAgeSeconds()));

        upnpService.getProtocolFactory().createReceivingAsync(msg).run();
        Thread.sleep(100);
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    @Test
    void receivedInvalidSearchResponses() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        RemoteDevice rd = SampleData.createRemoteDevice();

        // Missing USN header
        IncomingSearchResponse msg = createResponseMessage(new STAllHeader());
        upnpService.getProtocolFactory().createReceivingAsync(msg).run();
        Thread.sleep(100);
        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());

        // Missing location header
        msg = createResponseMessage(new STAllHeader());
        msg.getHeaders().add(UpnpHeader.Type.USN, new USNRootDeviceHeader(rd.getIdentity().getUdn()));
        upnpService.getProtocolFactory().createReceivingAsync(msg).run();
        Thread.sleep(100);
        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());

        // Missing max age header
        msg = createResponseMessage(new STAllHeader());
        msg.getHeaders().add(UpnpHeader.Type.USN, new USNRootDeviceHeader(rd.getIdentity().getUdn()));
        msg.getHeaders().add(UpnpHeader.Type.LOCATION, new LocationHeader(SampleDeviceRoot.getDeviceDescriptorURL()));
        upnpService.getProtocolFactory().createReceivingAsync(msg).run();
        Thread.sleep(100);
        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    @Test
    void receivedAlreadyKnownLocalUDN() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        RemoteDevice rd = SampleData.createRemoteDevice();

        IncomingSearchResponse msg = createResponseMessage(new STAllHeader());
        msg.getHeaders().add(UpnpHeader.Type.USN, new USNRootDeviceHeader(rd.getIdentity().getUdn()));
        msg.getHeaders().add(UpnpHeader.Type.LOCATION, new LocationHeader(SampleDeviceRoot.getDeviceDescriptorURL()));
        msg.getHeaders().add(UpnpHeader.Type.MAX_AGE, new MaxAgeHeader(rd.getIdentity().getMaxAgeSeconds()));

        upnpService.getProtocolFactory().createReceivingAsync(msg).run();
        Thread.sleep(100);
        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    @Test
    void receiveEmbeddedTriggersUpdate() throws Exception {
        UpnpService upnpService = new MockUpnpService(false, true);
        upnpService.startup();

        RemoteDevice rd = SampleData.createRemoteDevice();
        RemoteDevice embedded = rd.getEmbeddedDevices()[0];

        upnpService.getRegistry().addDevice(rd);

        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        IncomingSearchResponse msg = createResponseMessage(new STAllHeader());
        msg.getHeaders().add(UpnpHeader.Type.USN, new UDNHeader(embedded.getIdentity().getUdn()));
        msg.getHeaders().add(UpnpHeader.Type.LOCATION, new LocationHeader(SampleDeviceRoot.getDeviceDescriptorURL()));
        msg.getHeaders().add(UpnpHeader.Type.MAX_AGE, new MaxAgeHeader(rd.getIdentity().getMaxAgeSeconds()));

        Thread.sleep(1000);
        upnpService.getProtocolFactory().createReceivingAsync(msg).run();

        Thread.sleep(1000);
        upnpService.getProtocolFactory().createReceivingAsync(msg).run();

        Thread.sleep(1000);
        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        upnpService.shutdown();
    }

    protected IncomingSearchResponse createResponseMessage(UpnpHeader stHeader) throws UnknownHostException {
        IncomingSearchResponse msg = new IncomingSearchResponse(new IncomingDatagramMessage<>(
                new UpnpResponse(UpnpResponse.Status.OK), InetAddress.getByName("127.0.0.1"),
                Constants.UPNP_MULTICAST_PORT, InetAddress.getByName("127.0.0.1")));

        msg.getHeaders().add(UpnpHeader.Type.ST, stHeader);
        msg.getHeaders().add(UpnpHeader.Type.EXT, new EXTHeader());
        msg.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader());
        return msg;
    }
}
