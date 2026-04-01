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
package org.jupnp.local;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.data.SampleData;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.UDADeviceType;

/**
 * @author Christian Bauer
 */
class LocalActionInvocationDatatypesTest {

    @Test
    void invokeActions() throws Exception {

        LocalDevice device = new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("SomeDevice", 1),
                new DeviceDetails("Some Device"), SampleData.readService(LocalTestServiceOne.class));
        LocalService svc = SampleData.getFirstService(device);

        ActionInvocation getDataInvocation = new ActionInvocation(svc.getAction("GetData"));
        svc.getExecutor(getDataInvocation.getAction()).execute(getDataInvocation);
        assertNull(getDataInvocation.getFailure());
        assertEquals(1, getDataInvocation.getOutput().length);
        assertEquals(512, ((byte[]) getDataInvocation.getOutput()[0].getValue()).length);

        ActionInvocation invocation = new ActionInvocation(svc.getAction("GetStrings"));
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertNull(invocation.getFailure());
        assertEquals(2, invocation.getOutput().length);
        assertEquals("foo", invocation.getOutput("One").toString());
        assertEquals("bar", invocation.getOutput("Two").toString());

        invocation = new ActionInvocation(svc.getAction("GetThree"));
        assertEquals(svc.getAction("GetThree").getOutputArguments()[0].getDatatype().getBuiltin().getDescriptorName(),
                "i2");
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertNull(invocation.getFailure());
        assertEquals(1, invocation.getOutput().length);
        assertEquals("123", invocation.getOutput("three").toString());

        invocation = new ActionInvocation(svc.getAction("GetFour"));
        assertEquals(svc.getAction("GetFour").getOutputArguments()[0].getDatatype().getBuiltin().getDescriptorName(),
                "int");
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertNull(invocation.getFailure());
        assertEquals(1, invocation.getOutput().length);
        assertEquals("456", invocation.getOutput("four").toString());

        invocation = new ActionInvocation(svc.getAction("GetFive"));
        assertEquals(svc.getAction("GetFive").getOutputArguments()[0].getDatatype().getBuiltin().getDescriptorName(),
                "int");
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertNull(invocation.getFailure());
        assertEquals(1, invocation.getOutput().length);
        assertEquals("456", invocation.getOutput("five").toString());
    }

    @UpnpService(serviceId = @UpnpServiceId("SomeService"), serviceType = @UpnpServiceType(value = "SomeService", version = 1), supportsQueryStateVariables = false)
    public static class LocalTestServiceOne {

        @UpnpStateVariable(sendEvents = false)
        private byte[] data;

        @UpnpStateVariable(sendEvents = false, datatype = "string")
        private String dataString;

        @UpnpStateVariable(sendEvents = false)
        private String one;

        @UpnpStateVariable(sendEvents = false)
        private String two;

        @UpnpStateVariable(sendEvents = false)
        private short three;

        @UpnpStateVariable(sendEvents = false, name = "four", datatype = "int")
        private int four;

        public LocalTestServiceOne() {
            data = new byte[512];
            new Random().nextBytes(data);

            try {
                dataString = new String(data, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        // This works and the byte[] should not interfere with any Object[] handling in the executors
        @UpnpAction(out = @UpnpOutputArgument(name = "RandomData"))
        public byte[] getData() {
            return data;
        }

        // This fails, we can't just put random data into a string
        @UpnpAction(out = @UpnpOutputArgument(name = "RandomDataString"))
        public String getDataString() {
            return dataString;
        }

        // We are testing _several_ output arguments returned in a bean, access through getters
        @UpnpAction(out = { @UpnpOutputArgument(name = "One", getterName = "getOne"),
                @UpnpOutputArgument(name = "Two", getterName = "getTwo") })
        public StringsHolder getStrings() {
            return new StringsHolder();
        }

        // Conversion of short into integer/UPnP "i2" datatype
        @UpnpAction(out = @UpnpOutputArgument(name = "three"))
        public short getThree() {
            return 123;
        }

        // Conversion of int into integer/UPnP "int" datatype
        @UpnpAction(out = @UpnpOutputArgument(name = "four"))
        public Integer getFour() {
            return 456;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "five", stateVariable = "four"))
        public int getFive() {
            return 456;
        }
    }

    public static class StringsHolder {
        String one = "foo";
        String two = "bar";

        public String getOne() {
            return one;
        }

        public String getTwo() {
            return two;
        }
    }
}
