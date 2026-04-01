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
package example.registry;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Collection;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleDeviceRoot;
import org.jupnp.data.SampleDeviceRootLocal;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.resource.DeviceDescriptorResource;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.RegistrationException;
import org.jupnp.registry.Registry;

class RegistryBrowseTest {

    @Test
    void findDevice() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        LocalDevice device = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(device);

        UDN udn = device.getIdentity().getUdn();

        Registry registry = upnpService.getRegistry();
        Device foundDevice = registry.getDevice(udn, true);

        assertEquals(udn, foundDevice.getIdentity().getUdn());

        LocalDevice localDevice = registry.getLocalDevice(udn, true);
        assertEquals(udn, localDevice.getIdentity().getUdn());

        SampleDeviceRootLocal
                .assertLocalResourcesMatch(upnpService.getConfiguration().getNamespace().getResources(device));
    }

    @Test
    void findDeviceByType() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        LocalDevice device = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(device);

        Registry registry = upnpService.getRegistry();

        DeviceType deviceType = new UDADeviceType("MY-DEVICE-TYPE", 1);
        Collection<Device> devices = registry.getDevices(deviceType);
        assertEquals(1, devices.size());

        ServiceType serviceType = new UDAServiceType("MY-SERVICE-TYPE-ONE", 1);
        devices = registry.getDevices(serviceType);
        assertEquals(1, devices.size());
    }

    @Test
    void findLocalDevice() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice deviceOne = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(deviceOne);

        DeviceDescriptorResource resource = upnpService.getRegistry().getResource(DeviceDescriptorResource.class,
                SampleDeviceRoot.getDeviceDescriptorURI());

        assertNotNull(resource);
    }

    @Test
    void findLocalDeviceInvalidRelativePath() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        LocalDevice deviceOne = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(deviceOne);

        assertThrows(IllegalArgumentException.class, () -> upnpService.getRegistry()
                .getResource(DeviceDescriptorResource.class, URI.create("http://host/invalid/absolute/URI")));
    }

    @Test
    @Disabled("TODO: For now just ignore duplicate devices because we need to test proxies")
    void registerDuplicateDevices() {
        MockUpnpService upnpService = new MockUpnpService();

        LocalDevice deviceOne = SampleData.createLocalDevice();
        upnpService.getRegistry().addDevice(deviceOne);

        LocalDevice deviceTwo = SampleData.createLocalDevice();
        assertThrows(RegistrationException.class, () -> upnpService.getRegistry().addDevice(deviceTwo));
    }

    @Test
    void cleanupRemoteDevice() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        RemoteDevice rd = SampleData.createRemoteDevice();

        upnpService.getRegistry().addDevice(rd);

        assertEquals(upnpService.getRegistry().getRemoteDevices().size(), 1);

        Resource resource = upnpService.getRegistry()
                .getResource(URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/event/cb"));
        assertNotNull(resource);

        upnpService.getRegistry().removeDevice(rd);

        assertEquals(0, upnpService.getRegistry().getRemoteDevices().size());

        resource = upnpService.getRegistry()
                .getResource(URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/event/cb"));
        assertNull(resource);
    }
}
