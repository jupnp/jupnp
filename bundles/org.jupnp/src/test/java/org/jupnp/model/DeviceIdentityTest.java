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
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.UDA10DeviceDescriptorBinderImpl;
import org.jupnp.binding.xml.UDA10DeviceDescriptorBinderSAXImpl;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleDeviceRoot;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.types.UDN;
import org.jupnp.util.io.IO;

class DeviceIdentityTest {

    @Test
    void validateUDNLength() {
        assertTrue(new DeviceIdentity(new UDN("a".repeat(DeviceIdentity.MAX_UDN_LENGTH))).validate().isEmpty());
        assertFalse(new DeviceIdentity(new UDN("a".repeat(DeviceIdentity.MAX_UDN_LENGTH + 1))).validate().isEmpty());
    }

    @Test
    void rejectDescriptorWithTooLongUDNDOM() throws Exception {
        assertTooLongUDNRejected(new UDA10DeviceDescriptorBinderImpl());
    }

    @Test
    void rejectDescriptorWithTooLongUDNSAX() throws Exception {
        assertTooLongUDNRejected(new UDA10DeviceDescriptorBinderSAXImpl());
    }

    protected void assertTooLongUDNRejected(DeviceDescriptorBinder binder) throws Exception {
        String rootUDN = SampleDeviceRoot.getRootUDN().toString();
        String descriptorXml = IO.readLines(getClass().getResourceAsStream("/descriptors/device/uda10.xml"));
        assertTrue(descriptorXml.contains(rootUDN));

        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        assertNotNull(binder.describe(device, descriptorXml));

        String tooLongUDN = UDN.PREFIX + "a".repeat(DeviceIdentity.MAX_UDN_LENGTH + 1);
        assertThrows(ValidationException.class,
                () -> binder.describe(device, descriptorXml.replace(rootUDN, tooLongUDN)));
    }
}
