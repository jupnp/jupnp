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

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleServiceOne;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.HostHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.protocol.sync.ReceivingRetrieval;

class ServiceDescriptorRetrievalTest {

    @Test
    void registerAndRetrieveDescriptor() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Register a device
        LocalDevice localDevice = SampleData.createLocalDevice();
        LocalService service = SampleData.getFirstService(localDevice);
        upnpService.getRegistry().addDevice(localDevice);

        // Retrieve the descriptor
        URI descriptorURI = upnpService.getConfiguration().getNamespace().getDescriptorPath(service);
        StreamRequestMessage descRetrievalMessage = new StreamRequestMessage(UpnpRequest.Method.GET, descriptorURI);
        descRetrievalMessage.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader("localhost", 1234));
        ReceivingRetrieval prot = new ReceivingRetrieval(upnpService, descRetrievalMessage);
        prot.run();
        StreamResponseMessage descriptorMessage = prot.getOutputMessage();

        // UDA 1.0 spec days this musst be 'text/xml'
        assertEquals(ContentTypeHeader.DEFAULT_CONTENT_TYPE,
                descriptorMessage.getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE).getValue());

        // Read the response and compare the returned device descriptor
        ServiceDescriptorBinder binder = upnpService.getConfiguration().getServiceDescriptorBinderUDA10();

        RemoteService remoteService = SampleData.createUndescribedRemoteService();

        remoteService = binder.describe(remoteService, descriptorMessage.getBodyString());
        SampleServiceOne.assertMatch(remoteService, service);
    }

    @Test
    void retrieveNonExistentDescriptor() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Retrieve the descriptor
        LocalDevice localDevice = SampleData.createLocalDevice();
        Service service = SampleData.getFirstService(localDevice);

        URI descriptorURI = upnpService.getConfiguration().getNamespace().getDescriptorPath(service);
        StreamRequestMessage descRetrievalMessage = new StreamRequestMessage(UpnpRequest.Method.GET, descriptorURI);
        descRetrievalMessage.getHeaders().add(UpnpHeader.Type.HOST, new HostHeader("localhost", 1234));
        ReceivingRetrieval prot = new ReceivingRetrieval(upnpService, descRetrievalMessage);
        prot.run();
        StreamResponseMessage descriptorMessage = prot.getOutputMessage();

        // Should be null because it can't be found
        assertNull(descriptorMessage);
    }
}
