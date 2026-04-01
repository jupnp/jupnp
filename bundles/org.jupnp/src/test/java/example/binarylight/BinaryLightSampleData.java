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
package example.binarylight;

import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.data.SampleData;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.UDADeviceType;

/**
 * @author Christian Bauer
 */
public class BinaryLightSampleData {

    public static LocalDevice createDevice(Class<?> serviceClass) throws Exception {
        return createDevice(SampleData.readService(new AnnotationLocalServiceBinder(), serviceClass));
    }

    public static LocalDevice createDevice(LocalService service) throws Exception {
        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("BinaryLight", 1),
                new DeviceDetails("Example Binary Light"), service);
    }
}
