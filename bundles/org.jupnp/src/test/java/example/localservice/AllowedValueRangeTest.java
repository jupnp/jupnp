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
package example.localservice;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.data.SampleData;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.DeviceType;

class AllowedValueRangeTest {

    static LocalDevice createTestDevice(Class serviceClass) throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(serviceClass);
        svc.setManager(new DefaultServiceManager(svc, serviceClass));

        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new DeviceType("mydomain", "CustomDevice", 1),
                new DeviceDetails("A Custom Device"), svc);
    }

    static Object[][] getDevices() {
        try {
            return new LocalDevice[][] { { createTestDevice(MyServiceWithAllowedValueRange.class) },
                    { createTestDevice(MyServiceWithAllowedValueRangeProvider.class) }, };
        } catch (Exception e) {
            e.printStackTrace(System.err);
            // Damn testng swallows exceptions in provider/factory methods
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void validateBinding(LocalDevice device) {
        LocalService svc = device.getServices()[0];
        assertEquals(1, svc.getStateVariables().length);
        assertEquals(Datatype.Builtin.I4, svc.getStateVariables()[0].getTypeDetails().getDatatype().getBuiltin());
        assertEquals(10, svc.getStateVariables()[0].getTypeDetails().getAllowedValueRange().getMinimum());
        assertEquals(100, svc.getStateVariables()[0].getTypeDetails().getAllowedValueRange().getMaximum());
        assertEquals(5, svc.getStateVariables()[0].getTypeDetails().getAllowedValueRange().getStep());
    }
}
