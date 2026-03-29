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

import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.data.SampleData;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.UDADeviceType;

/**
 * @author Christian Bauer
 */
class LocalServiceBindingDatatypesTest {

    static LocalDevice createTestDevice(LocalService service) throws Exception {
        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("TestDevice", 1),
                new DeviceDetails("Test Device"), service);
    }

    static Object[][] getDevices() throws Exception {
        // This is what we are actually testing
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();

        return new LocalDevice[][] { { createTestDevice(binder.read(TestServiceOne.class)) }, };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void validateBinding(LocalDevice device) {
        LocalService svc = SampleData.getFirstService(device);

        assertEquals(1, svc.getStateVariables().length);
        assertEquals(Datatype.Builtin.BIN_BASE64,
                svc.getStateVariable("Data").getTypeDetails().getDatatype().getBuiltin());
        assertFalse(svc.getStateVariable("Data").getEventDetails().isSendEvents());

        assertEquals(1, svc.getActions().length);

        assertEquals("GetData", svc.getAction("GetData").getName());
        assertEquals(1, svc.getAction("GetData").getArguments().length);
        assertEquals("RandomData", svc.getAction("GetData").getArguments()[0].getName());
        assertEquals(ActionArgument.Direction.OUT, svc.getAction("GetData").getArguments()[0].getDirection());
        assertEquals("Data", svc.getAction("GetData").getArguments()[0].getRelatedStateVariableName());
        assertTrue(svc.getAction("GetData").getArguments()[0].isReturnValue());
    }

    /* ####################################################################################################### */

    @UpnpService(serviceId = @UpnpServiceId("SomeService"), serviceType = @UpnpServiceType(value = "SomeService", version = 1), supportsQueryStateVariables = false)
    public static class TestServiceOne {

        public TestServiceOne() {
            data = new byte[8];
            new Random().nextBytes(data);
        }

        @UpnpStateVariable(sendEvents = false)
        private byte[] data;

        @UpnpAction(out = @UpnpOutputArgument(name = "RandomData"))
        public byte[] getData() {
            return data;
        }
    }
}
