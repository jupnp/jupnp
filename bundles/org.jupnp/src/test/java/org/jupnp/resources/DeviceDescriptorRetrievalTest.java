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
package org.jupnp.resources;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleDeviceRoot;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.HostHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.protocol.sync.ReceivingRetrieval;

class DeviceDescriptorRetrievalTest {

    @Test
    void registerAndRetrieveDescriptor() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Register a device
        LocalDevice localDevice = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(localDevice);

        // Retrieve the descriptor
        StreamRequestMessage descRetrievalMessage = new StreamRequestMessage(UpnpRequest.Method.GET,
                SampleDeviceRoot.getDeviceDescriptorURI());
        descRetrievalMessage.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader("localhost", 1234));
        ReceivingRetrieval prot = new ReceivingRetrieval(upnpService, descRetrievalMessage);
        prot.run();
        StreamResponseMessage descriptorMessage = prot.getOutputMessage();

        // UDA 1.0 spec days this must be 'text/xml'
        assertEquals(ContentTypeHeader.DEFAULT_CONTENT_TYPE,
                descriptorMessage.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE).getValue());

        // Read the response and compare the returned device descriptor (test with LocalDevice for correct assertions)
        DeviceDescriptorBinder binder = upnpService.getConfiguration().getDeviceDescriptorBinderUDA10();

        RemoteDevice returnedDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        returnedDevice = binder.describe(returnedDevice, descriptorMessage.getBodyString());

        SampleDeviceRoot
                .assertLocalResourcesMatch(upnpService.getConfiguration().getNamespace().getResources(returnedDevice));
    }

    @Test
    void retrieveNonExistentDescriptor() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Retrieve the descriptor
        StreamRequestMessage descRetrievalMessage = new StreamRequestMessage(UpnpRequest.Method.GET,
                SampleDeviceRoot.getDeviceDescriptorURI());
        descRetrievalMessage.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader("localhost", 1234));
        ReceivingRetrieval prot = new ReceivingRetrieval(upnpService, descRetrievalMessage);
        prot.run();
        StreamResponseMessage descriptorMessage = prot.getOutputMessage();

        assertNull(descriptorMessage);
    }
}
