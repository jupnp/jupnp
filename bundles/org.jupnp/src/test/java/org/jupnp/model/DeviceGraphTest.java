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
package org.jupnp.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleDeviceEmbeddedOne;
import org.jupnp.data.SampleDeviceEmbeddedTwo;
import org.jupnp.data.SampleDeviceRootLocal;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.ServiceType;

/**
 * @author Christian Bauer
 */
class DeviceGraphTest {

    @Test
    void findRoot() {
        LocalDevice ld = SampleData.createLocalDevice();

        LocalDevice root = ld.getEmbeddedDevices()[0].getRoot();
        assertEquals(SampleDeviceRootLocal.getRootUDN(), root.getIdentity().getUdn());

        root = ld.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getRoot();
        assertEquals(SampleDeviceRootLocal.getRootUDN(), root.getIdentity().getUdn());
    }

    @Test
    void findEmbeddedDevices() {
        LocalDevice ld = SampleData.createLocalDevice();

        LocalDevice[] embedded = ld.findEmbeddedDevices();
        assertEquals(2, embedded.length);

        boolean haveOne = false, haveTwo = false;

        for (LocalDevice em : embedded) {
            if (em.getIdentity().getUdn().equals(ld.getEmbeddedDevices()[0].getIdentity().getUdn())) {
                haveOne = true;
            }
            if (em.getIdentity().getUdn()
                    .equals(ld.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getIdentity().getUdn())) {
                haveTwo = true;
            }
        }

        assertTrue(haveOne);
        assertTrue(haveTwo);
    }

    @Test
    void findDevicesWithUDN() {
        LocalDevice ld = SampleData.createLocalDevice();

        LocalDevice ldOne = ld.findDevice(SampleDeviceRootLocal.getRootUDN());
        assertEquals(SampleDeviceRootLocal.getRootUDN(), ldOne.getIdentity().getUdn());

        LocalDevice ldTwo = ld.findDevice(SampleDeviceEmbeddedOne.getEmbeddedOneUDN());
        assertEquals(SampleDeviceEmbeddedOne.getEmbeddedOneUDN(), ldTwo.getIdentity().getUdn());

        LocalDevice ldThree = ld.findDevice(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN());
        assertEquals(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN(), ldThree.getIdentity().getUdn());

        RemoteDevice rd = SampleData.createRemoteDevice();

        RemoteDevice rdOne = rd.findDevice(SampleDeviceRootLocal.getRootUDN());
        assertEquals(SampleDeviceRootLocal.getRootUDN(), rdOne.getIdentity().getUdn());

        RemoteDevice rdTwo = rd.findDevice(SampleDeviceEmbeddedOne.getEmbeddedOneUDN());
        assertEquals(SampleDeviceEmbeddedOne.getEmbeddedOneUDN(), rdTwo.getIdentity().getUdn());

        RemoteDevice rdThree = rd.findDevice(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN());
        assertEquals(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN(), rdThree.getIdentity().getUdn());
    }

    @Test
    void findDevicesWithDeviceType() {
        LocalDevice ld = SampleData.createLocalDevice();

        LocalDevice[] ldOne = ld.findDevices(ld.getType());
        assertEquals(1, ldOne.length);
        assertEquals(SampleDeviceRootLocal.getRootUDN(), ldOne[0].getIdentity().getUdn());

        LocalDevice[] ldTwo = ld.findDevices(ld.getEmbeddedDevices()[0].getType());
        assertEquals(1, ldTwo.length);
        assertEquals(SampleDeviceEmbeddedOne.getEmbeddedOneUDN(), ldTwo[0].getIdentity().getUdn());

        LocalDevice[] ldThree = ld.findDevices(ld.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getType());
        assertEquals(1, ldThree.length);
        assertEquals(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN(), ldThree[0].getIdentity().getUdn());

        RemoteDevice rd = SampleData.createRemoteDevice();

        RemoteDevice[] rdOne = rd.findDevices(rd.getType());
        assertEquals(1, rdOne.length);
        assertEquals(SampleDeviceRootLocal.getRootUDN(), rdOne[0].getIdentity().getUdn());

        RemoteDevice[] rdTwo = rd.findDevices(rd.getEmbeddedDevices()[0].getType());
        assertEquals(1, rdTwo.length);
        assertEquals(SampleDeviceEmbeddedOne.getEmbeddedOneUDN(), rdTwo[0].getIdentity().getUdn());

        RemoteDevice[] rdThree = rd.findDevices(rd.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getType());
        assertEquals(1, rdThree.length);
        assertEquals(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN(), rdThree[0].getIdentity().getUdn());
    }

    @Test
    void findServicesAll() {
        LocalDevice ld = SampleData.createLocalDevice();

        Service one = ld.getServices()[0];
        Service two = ld.getEmbeddedDevices()[0].getServices()[0];
        Service three = ld.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getServices()[0];

        Service[] services = ld.findServices();

        boolean haveOne = false, haveTwo = false, haveThree = false;
        for (Service service : services) {
            if (service.getServiceId().equals(one.getServiceId())) {
                haveOne = true;
            }
            if (service.getServiceId().equals(two.getServiceId())) {
                haveTwo = true;
            }
            if (service.getServiceId().equals(three.getServiceId())) {
                haveThree = true;
            }
        }
        assertTrue(haveOne);
        assertTrue(haveTwo);
        assertTrue(haveThree);
    }

    @Test
    void findServicesType() {
        LocalDevice ld = SampleData.createLocalDevice();

        Service one = ld.getServices()[0];
        Service two = ld.getEmbeddedDevices()[0].getServices()[0];
        Service three = ld.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getServices()[0];

        Service[] services = ld.findServices(one.getServiceType());
        assertEquals(1, services.length);
        assertEquals(one.getServiceId(), services[0].getServiceId());

        services = ld.findServices(two.getServiceType());
        assertEquals(1, services.length);
        assertEquals(two.getServiceId(), services[0].getServiceId());

        services = ld.findServices(three.getServiceType());
        assertEquals(1, services.length);
        assertEquals(three.getServiceId(), services[0].getServiceId());
    }

    @Test
    void findServicesId() {
        LocalDevice ld = SampleData.createLocalDevice();

        Service one = ld.getServices()[0];
        Service two = ld.getEmbeddedDevices()[0].getServices()[0];
        Service three = ld.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getServices()[0];

        Service service = ld.findService(one.getServiceId());
        assertEquals(one.getServiceId(), service.getServiceId());

        service = ld.findService(two.getServiceId());
        assertEquals(two.getServiceId(), service.getServiceId());

        service = ld.findService(three.getServiceId());
        assertEquals(three.getServiceId(), service.getServiceId());
    }

    @Test
    void findServicesFirst() {
        LocalDevice ld = SampleData.createLocalDevice();

        Service one = ld.getServices()[0];
        Service two = ld.getEmbeddedDevices()[0].getServices()[0];
        Service three = ld.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getServices()[0];

        Service service = ld.findService(one.getServiceType());
        assertEquals(one.getServiceId(), service.getServiceId());

        service = ld.findService(two.getServiceType());
        assertEquals(two.getServiceId(), service.getServiceId());

        service = ld.findService(three.getServiceType());
        assertEquals(three.getServiceId(), service.getServiceId());
    }

    @Test
    void findServiceTypes() {
        LocalDevice ld = SampleData.createLocalDevice();

        ServiceType[] svcTypes = ld.findServiceTypes();
        assertEquals(3, svcTypes.length);

        boolean haveOne = false, haveTwo = false, haveThree = false;

        for (ServiceType svcType : svcTypes) {
            if (svcType.equals(ld.getServices()[0].getServiceType())) {
                haveOne = true;
            }
            if (svcType.equals(ld.getEmbeddedDevices()[0].getServices()[0].getServiceType())) {
                haveTwo = true;
            }
            if (svcType.equals(ld.getEmbeddedDevices()[0].getEmbeddedDevices()[0].getServices()[0].getServiceType())) {
                haveThree = true;
            }
        }

        assertTrue(haveOne);
        assertTrue(haveTwo);
        assertTrue(haveThree);
    }
}
