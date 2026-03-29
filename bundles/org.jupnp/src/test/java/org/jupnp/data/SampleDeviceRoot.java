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
import java.net.URL;

import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.model.profile.DeviceDetailsProvider;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.resource.ServiceEventCallbackResource;
import org.jupnp.model.types.DLNACaps;
import org.jupnp.model.types.DLNADoc;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.util.URIUtil;

/**
 * @author Christian Bauer
 */
public class SampleDeviceRoot extends SampleDevice {

    public SampleDeviceRoot(DeviceIdentity identity, Service service, Device embeddedDevice) {
        super(identity, service, embeddedDevice);
    }

    @Override
    public DeviceType getDeviceType() {
        return new UDADeviceType("MY-DEVICE-TYPE", 1);
    }

    @Override
    public DeviceDetails getDeviceDetails() {
        return new DeviceDetails("My Testdevice", new ManufacturerDetails("4th Line", "http://www.4thline.org/"),
                new ModelDetails("MYMODEL", "TEST Device", "ONE", "http://www.4thline.org/foo"), "000da201238c",
                "100000000001", "http://www.4thline.org/some_user_interface/",
                new DLNADoc[] { new DLNADoc("DMS", DLNADoc.Version.V1_5), new DLNADoc("M-DMS", DLNADoc.Version.V1_5) },
                new DLNACaps(new String[] { "av-upload", "image-upload", "audio-upload" }));
    }

    @Override
    public DeviceDetailsProvider getDeviceDetailsProvider() {
        return info -> getDeviceDetails();
    }

    @Override
    public Icon[] getIcons() {
        return new Icon[] { new Icon("image/png", 32, 32, 8, URI.create("icon.png")),
                new Icon("image/png", 32, 32, 8, URI.create("icon2.png")) };
    }

    public static UDN getRootUDN() {
        return new UDN("MY-DEVICE-123");
    }

    public static URI getDeviceDescriptorURI() {
        return URI.create("/dev/MY-DEVICE-123/desc");
    }

    public static URL getDeviceDescriptorURL() {
        return URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), getDeviceDescriptorURI());
    }

    public static URL getSecondDeviceDescriptorURL() {
        return URIUtil.createAbsoluteURL(SampleData.getSecondLocalBaseURL(), getDeviceDescriptorURI());
    }

    public static void assertMatch(Device a, Device b) {
        assertMatch(a, b, true);
    }

    public static void assertMatch(Device a, Device b, boolean checkType) {
        assertTrue(a.isRoot());
        assertDeviceMatch(a, b, checkType);
    }

    public static void assertLocalResourcesMatch(Resource[] resources) {
        assertEquals(ServiceEventCallbackResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-123/svc/upnp-org/MY-SERVICE-123/event/cb"))
                        .getClass());

        assertEquals(ServiceEventCallbackResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-456/svc/upnp-org/MY-SERVICE-456/event/cb"))
                        .getClass());
        assertEquals(ServiceEventCallbackResource.class,
                getLocalResource(resources, URI.create("/dev/MY-DEVICE-789/svc/upnp-org/MY-SERVICE-789/event/cb"))
                        .getClass());
    }

    protected static Resource getLocalResource(Resource[] resources, URI localPathQuery) {
        for (Resource localResource : resources) {
            if (localResource.matches(localPathQuery)) {
                return localResource;
            }
        }
        return null;
    }

    protected static void assertDeviceMatch(Device a, Device b) {
        assertDeviceMatch(a, b, true);
    }

    protected static void assertDeviceMatch(Device a, Device b, boolean checkType) {
        assertEquals(0, a.validate().size());
        assertEquals(0, b.validate().size());

        if (checkType) {
            assertEquals(a, b); // Checking equals() method
        }
        assertEquals(a.getIdentity().getUdn(), b.getIdentity().getUdn());
        assertEquals(a.getVersion().getMajor(), b.getVersion().getMajor());
        assertEquals(a.getVersion().getMinor(), b.getVersion().getMinor());
        assertEquals(a.getType(), b.getType());
        assertEquals(a.getDetails().getFriendlyName(), b.getDetails().getFriendlyName());
        assertEquals(a.getDetails().getManufacturerDetails().getManufacturer(),
                b.getDetails().getManufacturerDetails().getManufacturer());
        assertEquals(a.getDetails().getManufacturerDetails().getManufacturerURI(),
                b.getDetails().getManufacturerDetails().getManufacturerURI());
        assertEquals(a.getDetails().getModelDetails().getModelDescription(),
                b.getDetails().getModelDetails().getModelDescription());
        assertEquals(a.getDetails().getModelDetails().getModelName(), b.getDetails().getModelDetails().getModelName());
        assertEquals(a.getDetails().getModelDetails().getModelNumber(),
                b.getDetails().getModelDetails().getModelNumber());
        assertEquals(a.getDetails().getModelDetails().getModelURI(), b.getDetails().getModelDetails().getModelURI());
        assertEquals(a.getDetails().getSerialNumber(), b.getDetails().getSerialNumber());
        assertEquals(a.getDetails().getUpc(), b.getDetails().getUpc());
        assertEquals(a.getDetails().getPresentationURI(), b.getDetails().getPresentationURI());

        assertEquals(a.getDetails().getDlnaDocs().length, b.getDetails().getDlnaDocs().length);
        for (int i = 0; i < a.getDetails().getDlnaDocs().length; i++) {
            DLNADoc aDoc = a.getDetails().getDlnaDocs()[i];
            DLNADoc bDoc = b.getDetails().getDlnaDocs()[i];
            assertEquals(aDoc, bDoc);
        }
        assertEquals(a.getDetails().getDlnaCaps(), b.getDetails().getDlnaCaps());

        assertEquals(a.getIcons() != null, b.getIcons() != null);
        if (a.getIcons() != null) {
            assertEquals(a.getIcons().length, b.getIcons().length);
            for (int i = 0; i < a.getIcons().length; i++) {
                assertEquals(a.getIcons()[i].getDevice(), a);
                assertEquals(b.getIcons()[i].getDevice(), b);
                assertEquals(a.getIcons()[i].getUri(), b.getIcons()[i].getUri());
                assertEquals(a.getIcons()[i].getMimeType(), b.getIcons()[i].getMimeType());
                assertEquals(a.getIcons()[i].getWidth(), b.getIcons()[i].getWidth());
                assertEquals(a.getIcons()[i].getHeight(), b.getIcons()[i].getHeight());
                assertEquals(a.getIcons()[i].getDepth(), b.getIcons()[i].getDepth());
            }
        }

        assertEquals(a.hasServices(), b.hasServices());
        if (a.getServices() != null) {
            assertEquals(a.getServices().length, b.getServices().length);
            for (int i = 0; i < a.getServices().length; i++) {
                Service service = a.getServices()[i];
                assertEquals(service.getServiceType(), b.getServices()[i].getServiceType());
                assertEquals(service.getServiceId(), b.getServices()[i].getServiceId());
                if (a instanceof RemoteDevice && b instanceof RemoteDevice) {
                    RemoteService remoteServiceA = (RemoteService) service;
                    RemoteService remoteServiceB = (RemoteService) b.getServices()[i];

                    assertEquals(remoteServiceA.getEventSubscriptionURI(), remoteServiceB.getEventSubscriptionURI());
                    assertEquals(remoteServiceA.getControlURI(), remoteServiceB.getControlURI());
                    assertEquals(remoteServiceA.getDescriptorURI(), remoteServiceB.getDescriptorURI());
                }
            }
        }

        assertEquals(a.hasEmbeddedDevices(), b.hasEmbeddedDevices());
        if (a.getEmbeddedDevices() != null) {
            assertEquals(a.getEmbeddedDevices().length, b.getEmbeddedDevices().length);
            for (int i = 0; i < a.getEmbeddedDevices().length; i++) {
                Device aEmbedded = a.getEmbeddedDevices()[i];
                assertDeviceMatch(aEmbedded, b.getEmbeddedDevices()[i], checkType);
            }
        }
    }
}
