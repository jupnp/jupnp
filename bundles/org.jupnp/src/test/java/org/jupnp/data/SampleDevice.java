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

import java.lang.reflect.Constructor;

import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.Service;
import org.jupnp.model.profile.DeviceDetailsProvider;
import org.jupnp.model.types.DeviceType;

/**
 * @author Christian Bauer
 */
public abstract class SampleDevice {

    public DeviceIdentity identity;
    public Service service;
    public Device embeddedDevice;

    protected SampleDevice(DeviceIdentity identity, Service service, Device embeddedDevice) {
        this.identity = identity;
        this.service = service;
        this.embeddedDevice = embeddedDevice;
    }

    public DeviceIdentity getIdentity() {
        return identity;
    }

    public Service getService() {
        return service;
    }

    public Device getEmbeddedDevice() {
        return embeddedDevice;
    }

    public abstract DeviceType getDeviceType();

    public abstract DeviceDetails getDeviceDetails();

    public abstract DeviceDetailsProvider getDeviceDetailsProvider();

    public abstract Icon[] getIcons();

    public <D extends Device> D newInstance(Constructor<D> deviceConstructor) {
        return newInstance(deviceConstructor, false);
    }

    public <D extends Device> D newInstance(Constructor<D> deviceConstructor, boolean useProvider) {
        try {
            if (useProvider) {
                return deviceConstructor.newInstance(getIdentity(), getDeviceType(), getDeviceDetailsProvider(),
                        getIcons(), getService(), getEmbeddedDevice());
            }
            return deviceConstructor.newInstance(getIdentity(), getDeviceType(), getDeviceDetails(), getIcons(),
                    getService(), getEmbeddedDevice());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
