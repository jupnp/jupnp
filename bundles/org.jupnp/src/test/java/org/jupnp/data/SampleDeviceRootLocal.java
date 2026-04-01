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

import java.net.URI;

import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.resource.DeviceDescriptorResource;
import org.jupnp.model.resource.IconResource;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.resource.ServiceControlResource;
import org.jupnp.model.resource.ServiceDescriptorResource;
import org.jupnp.model.resource.ServiceEventSubscriptionResource;

/**
 * @author Christian Bauer
 */
public class SampleDeviceRootLocal extends SampleDeviceRoot {

    public SampleDeviceRootLocal(DeviceIdentity identity, LocalService service, Device embeddedDevice) {
        super(identity, service, embeddedDevice);
    }

    public static void assertLocalResourcesMatch(Resource[] resources) {
        assertEquals(DeviceDescriptorResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/desc")).getClass());
        assertEquals(IconResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/icon.png")).getClass());
        assertEquals(IconResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/icon2.png")).getClass());
        assertEquals(ServiceDescriptorResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/desc"))
                        .getClass());
        assertEquals(ServiceControlResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/action"))
                        .getClass());
        assertEquals(ServiceEventSubscriptionResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/event"))
                        .getClass());
        assertEquals(IconResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/icon3.png")).getClass());
        assertEquals(ServiceDescriptorResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/svc/upnp-org/MY-SERVICE-456/desc"))
                        .getClass());
        assertEquals(ServiceControlResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/svc/upnp-org/MY-SERVICE-456/action"))
                        .getClass());
        assertEquals(ServiceEventSubscriptionResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/svc/upnp-org/MY-SERVICE-456/event"))
                        .getClass());
        assertEquals(ServiceDescriptorResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-789/svc/upnp-org/MY-SERVICE-789/desc"))
                        .getClass());
        assertEquals(ServiceControlResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-789/svc/upnp-org/MY-SERVICE-789/action"))
                        .getClass());
        assertEquals(ServiceEventSubscriptionResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-789/svc/upnp-org/MY-SERVICE-789/event"))
                        .getClass());
    }
}
