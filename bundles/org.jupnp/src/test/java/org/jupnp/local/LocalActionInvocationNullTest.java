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

import org.junit.jupiter.api.Test;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.data.SampleData;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UDADeviceType;

/**
 * @author Christian Bauer
 */
class LocalActionInvocationNullTest {

    @Test
    void invokeActions() throws Exception {

        LocalDevice device = new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("SomeDevice", 1),
                new DeviceDetails("Some Device"), SampleData.readService(LocalTestServiceOne.class));
        LocalService<LocalTestServiceOne> svc = SampleData.getFirstService(device);

        ActionInvocation invocation;

        // This succeeds
        invocation = new ActionInvocation(svc.getAction("SetSomeValues"));
        invocation.setInput("One", "foo");
        invocation.setInput("Two", "bar");
        invocation.setInput("Three", "baz");
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertNull(invocation.getFailure());
        assertEquals("foo", svc.getManager().getImplementation().one);
        assertEquals("bar", svc.getManager().getImplementation().two);
        assertEquals("baz", svc.getManager().getImplementation().three.toString());

        // Empty string is fine, will be converted into "null"
        invocation = new ActionInvocation(svc.getAction("SetSomeValues"));
        invocation.setInput("One", "foo");
        invocation.setInput("Two", "");
        invocation.setInput("Three", null);
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertNull(invocation.getFailure());
        assertEquals("foo", svc.getManager().getImplementation().one);
        assertNull(svc.getManager().getImplementation().two);
        assertNull(svc.getManager().getImplementation().three);

        // Null is not fine for primitive input arguments
        invocation = new ActionInvocation(svc.getAction("SetPrimitive"));
        invocation.setInput("Primitive", "");
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertEquals(ErrorCode.ARGUMENT_VALUE_INVALID.getCode(), invocation.getFailure().getErrorCode());
        assertEquals(
                "The argument value is invalid. Primitive action method argument 'Primitive' requires input value, can't be null or empty string.",
                invocation.getFailure().getMessage());

        // We forgot to set one and it's a local invocation (no string conversion)
        invocation = new ActionInvocation(svc.getAction("SetSomeValues"));
        invocation.setInput("One", null);
        // OOPS! invocation.setInput("Two", null);
        invocation.setInput("Three", null);
        svc.getExecutor(invocation.getAction()).execute(invocation);
        assertNull(invocation.getFailure());
        assertNull(svc.getManager().getImplementation().one);
        assertNull(svc.getManager().getImplementation().two);
        assertNull(svc.getManager().getImplementation().three);
    }

    @UpnpService(serviceId = @UpnpServiceId("SomeService"), serviceType = @UpnpServiceType(value = "SomeService", version = 1), supportsQueryStateVariables = false, stringConvertibleTypes = MyString.class)
    public static class LocalTestServiceOne {

        @UpnpStateVariable(name = "A_ARG_TYPE_One", sendEvents = false)
        private String one;

        @UpnpStateVariable(name = "A_ARG_TYPE_Two", sendEvents = false)
        private String two;

        @UpnpStateVariable(name = "A_ARG_TYPE_Three", sendEvents = false)
        private MyString three;

        @UpnpStateVariable(sendEvents = false)
        private boolean primitive;

        @UpnpAction
        public void setSomeValues(@UpnpInputArgument(name = "One") String one,
                @UpnpInputArgument(name = "Two") String two, @UpnpInputArgument(name = "Three") MyString three) {
            this.one = one;
            this.two = two;
            this.three = three;
        }

        @UpnpAction
        public void setPrimitive(@UpnpInputArgument(name = "Primitive") boolean b) {
            this.primitive = b;
        }
    }

    public static class MyString {
        private String s;

        public MyString(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }
}
