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
import org.jupnp.binding.xml.UDA10DeviceDescriptorBinderImpl;
import org.jupnp.binding.xml.UDA10DeviceDescriptorBinderSAXImpl;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleDeviceRoot;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.util.io.IO;

class UDA10DeviceDescriptorParsingTest {

    @Test
    void readUDA10DescriptorDOM() throws Exception {
        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderImpl();

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device, IO.readLines(getClass().getResourceAsStream("/descriptors/device/uda10.xml")));

        SampleDeviceRoot.assertLocalResourcesMatch(
                new MockUpnpService().getConfiguration().getNamespace().getResources(device));
        SampleDeviceRoot.assertMatch(device, SampleData.createRemoteDevice());
    }

    @Test
    void readUDA10DescriptorSAX() throws Exception {
        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderSAXImpl();

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device, IO.readLines(getClass().getResourceAsStream("/descriptors/device/uda10.xml")));

        SampleDeviceRoot.assertLocalResourcesMatch(
                new MockUpnpService().getConfiguration().getNamespace().getResources(device));
        SampleDeviceRoot.assertMatch(device, SampleData.createRemoteDevice());
    }

    @Test
    void writeUDA10Descriptor() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderImpl();

        RemoteDevice device = SampleData.createRemoteDevice();
        String descriptorXml = binder.generate(device, new RemoteClientInfo(),
                upnpService.getConfiguration().getNamespace());

        RemoteDevice hydratedDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        hydratedDevice = binder.describe(hydratedDevice, descriptorXml);

        SampleDeviceRoot
                .assertLocalResourcesMatch(upnpService.getConfiguration().getNamespace().getResources(hydratedDevice)

                );
        SampleDeviceRoot.assertMatch(hydratedDevice, device);
    }

    @Test
    void writeUDA10DescriptorWithProvider() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderImpl();

        LocalDevice device = SampleData.createLocalDevice(true);
        String descriptorXml = binder.generate(device, new RemoteClientInfo(),
                upnpService.getConfiguration().getNamespace());

        RemoteDevice hydratedDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        hydratedDevice = binder.describe(hydratedDevice, descriptorXml);

        SampleDeviceRoot
                .assertLocalResourcesMatch(upnpService.getConfiguration().getNamespace().getResources(hydratedDevice)

                );
        // SampleDeviceRoot.assertMatch(hydratedDevice, device, false);
    }

    @Test
    void readUDA10DescriptorWithURLBase() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        DeviceDescriptorBinder binder = upnpService.getConfiguration().getDeviceDescriptorBinderUDA10();

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device,
                IO.readLines(getClass().getResourceAsStream("/descriptors/device/uda10_withbase.xml")));

        assertEquals(SampleData.getLocalBaseURL() + "mfc.html",
                device.normalizeURI(device.getDetails().getManufacturerDetails().getManufacturerURI()).toString());
        assertEquals(SampleData.getLocalBaseURL() + "someotherbase/MY-DEVICE-123/model.html",
                device.normalizeURI(device.getDetails().getModelDetails().getModelURI()).toString());
        assertEquals("http://www.4thline.org/some_ui",
                device.normalizeURI(device.getDetails().getPresentationURI()).toString());

        assertEquals(SampleData.getLocalBaseURL() + "someotherbase/MY-DEVICE-123/icon.png",
                device.normalizeURI(device.getIcons()[0].getUri()).toString());

        assertEquals(SampleData.getLocalBaseURL() + "someotherbase/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/desc.xml",
                device.normalizeURI(device.getServices()[0].getDescriptorURI()).toString());
        assertEquals(SampleData.getLocalBaseURL() + "someotherbase/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/control",
                device.normalizeURI(device.getServices()[0].getControlURI()).toString());
        assertEquals(SampleData.getLocalBaseURL() + "someotherbase/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/events",
                device.normalizeURI(device.getServices()[0].getEventSubscriptionURI()).toString());

        assertTrue(device.isRoot());
    }

    @Test
    void readUDA10DescriptorWithURLBase2() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        DeviceDescriptorBinder binder = upnpService.getConfiguration().getDeviceDescriptorBinderUDA10();

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device,
                IO.readLines(getClass().getResourceAsStream("/descriptors/device/uda10_withbase2.xml")));

        assertEquals(SampleData.getLocalBaseURL() + "mfc.html",
                device.normalizeURI(device.getDetails().getManufacturerDetails().getManufacturerURI()).toString());

        assertEquals(SampleData.getLocalBaseURL() + "model.html",
                device.normalizeURI(device.getDetails().getModelDetails().getModelURI()).toString());
        assertEquals("http://www.4thline.org/some_ui",
                device.normalizeURI(device.getDetails().getPresentationURI()).toString());

        assertEquals(SampleData.getLocalBaseURL() + "icon.png",
                device.normalizeURI(device.getIcons()[0].getUri()).toString());

        assertEquals(SampleData.getLocalBaseURL() + "svc.xml",
                device.normalizeURI(device.getServices()[0].getDescriptorURI()).toString());
        assertEquals(SampleData.getLocalBaseURL() + "control",
                device.normalizeURI(device.getServices()[0].getControlURI()).toString());
        assertEquals(SampleData.getLocalBaseURL() + "events",
                device.normalizeURI(device.getServices()[0].getEventSubscriptionURI()).toString());

        assertTrue(device.isRoot());
    }

    @Test
    void readUDA10DescriptorWithEmptyURLBase() throws Exception {
        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderImpl();

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device,
                IO.readLines(getClass().getResourceAsStream("/descriptors/device/uda10_emptybase.xml")));

        SampleDeviceRoot.assertLocalResourcesMatch(
                new MockUpnpService().getConfiguration().getNamespace().getResources(device));
        SampleDeviceRoot.assertMatch(device, SampleData.createRemoteDevice());
    }
}
