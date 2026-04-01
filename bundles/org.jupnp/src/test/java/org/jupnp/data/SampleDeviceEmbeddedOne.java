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

import java.net.URI;

import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.Service;
import org.jupnp.model.profile.DeviceDetailsProvider;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDN;

/**
 * @author Christian Bauer
 */
public class SampleDeviceEmbeddedOne extends SampleDevice {

    public SampleDeviceEmbeddedOne(DeviceIdentity identity, Service service, Device embeddedDevice) {
        super(identity, service, embeddedDevice);
    }

    @Override
    public DeviceType getDeviceType() {
        return new UDADeviceType("MY-DEVICE-TYPE-TWO", 2);
    }

    @Override
    public DeviceDetails getDeviceDetails() {
        return new DeviceDetails("My Testdevice Second", new ManufacturerDetails("4th Line", "http://www.4thline.org/"),
                new ModelDetails("MYMODEL", "TEST Device", "ONE", "http://www.4thline.org/this_is_the_embedded_model"),
                "000da201238d", "100000000002", "http://www.4thline.org/some_other_user_interface");
    }

    @Override
    public DeviceDetailsProvider getDeviceDetailsProvider() {
        return info -> getDeviceDetails();
    }

    @Override
    public Icon[] getIcons() {
        return new Icon[] { new Icon("image/png", 32, 32, 8, URI.create("icon3.png")) };
    }

    public static UDN getEmbeddedOneUDN() {
        return new UDN("MY-DEVICE-456");
    }
}
