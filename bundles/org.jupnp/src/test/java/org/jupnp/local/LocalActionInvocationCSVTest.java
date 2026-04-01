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

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
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
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.csv.CSV;
import org.jupnp.model.types.csv.CSVBoolean;
import org.jupnp.model.types.csv.CSVInteger;
import org.jupnp.model.types.csv.CSVString;
import org.jupnp.model.types.csv.CSVUnsignedIntegerFourBytes;

class LocalActionInvocationCSVTest {

    static LocalDevice createTestDevice(LocalService service) throws Exception {
        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("TestDevice", 1),
                new DeviceDetails("Test Device"), service);
    }

    static Object[][] getDevices() throws Exception {
        return new LocalDevice[][] { {
                createTestDevice(SampleData.readService(new AnnotationLocalServiceBinder(), TestServiceOne.class)) }, };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) throws Exception {
        LocalService svc = SampleData.getFirstService(device);

        List<String> testStrings = new CSVString();
        testStrings.add("f\\oo");
        testStrings.add("bar");
        testStrings.add("b,az");
        String result = executeActions(svc, "SetStringVar", "GetStringVar", testStrings);
        List<String> csvString = new CSVString(result);
        assertEquals(3, csvString.size());
        assertEquals("f\\oo", csvString.get(0));
        assertEquals("bar", csvString.get(1));
        assertEquals("b,az", csvString.get(2));

        List<Integer> testIntegers = new CSVInteger();
        testIntegers.add(123);
        testIntegers.add(-456);
        testIntegers.add(789);
        result = executeActions(svc, "SetIntVar", "GetIntVar", testIntegers);
        List<Integer> csvInteger = new CSVInteger(result);
        assertEquals(3, csvInteger.size());
        assertEquals(123, csvInteger.get(0));
        assertEquals(-456, csvInteger.get(1));
        assertEquals(789, csvInteger.get(2));

        List<Boolean> testBooleans = new CSVBoolean();
        testBooleans.add(true);
        testBooleans.add(true);
        testBooleans.add(false);
        result = executeActions(svc, "SetBooleanVar", "GetBooleanVar", testBooleans);
        List<Boolean> csvBoolean = new CSVBoolean(result);
        assertEquals(3, csvBoolean.size());
        assertTrue(csvBoolean.get(0));
        assertTrue(csvBoolean.get(1));
        assertFalse(csvBoolean.get(2));

        List<UnsignedIntegerFourBytes> testUifour = new CSVUnsignedIntegerFourBytes();
        testUifour.add(new UnsignedIntegerFourBytes(123));
        testUifour.add(new UnsignedIntegerFourBytes(456));
        testUifour.add(new UnsignedIntegerFourBytes(789));
        result = executeActions(svc, "SetUifourVar", "GetUifourVar", testUifour);
        List<UnsignedIntegerFourBytes> csvUifour = new CSVUnsignedIntegerFourBytes(result);
        assertEquals(3, csvUifour.size());
        assertEquals(new UnsignedIntegerFourBytes(123), csvUifour.get(0));
        assertEquals(new UnsignedIntegerFourBytes(456), csvUifour.get(1));
        assertEquals(new UnsignedIntegerFourBytes(789), csvUifour.get(2));
    }

    protected String executeActions(LocalService svc, String setAction, String getAction, List input) {
        ActionInvocation setActionInvocation = new ActionInvocation(svc.getAction(setAction));
        setActionInvocation.setInput(svc.getAction(setAction).getFirstInputArgument().getName(), input.toString());
        svc.getExecutor(setActionInvocation.getAction()).execute(setActionInvocation);
        assertNull(setActionInvocation.getFailure());
        assertEquals(0, setActionInvocation.getOutput().length);

        ActionInvocation getActionInvocation = new ActionInvocation(svc.getAction(getAction));
        svc.getExecutor(getActionInvocation.getAction()).execute(getActionInvocation);
        assertNull(getActionInvocation.getFailure());
        assertEquals(1, getActionInvocation.getOutput().length);
        return getActionInvocation.getOutput(svc.getAction(getAction).getFirstOutputArgument()).toString();
    }

    /* ####################################################################################################### */

    @UpnpService(serviceId = @UpnpServiceId("TestService"), serviceType = @UpnpServiceType(value = "TestService", version = 1))
    public static class TestServiceOne {

        @UpnpStateVariable(sendEvents = false)
        private CSV<String> stringVar;

        @UpnpStateVariable(sendEvents = false)
        private CSV<Integer> intVar;

        @UpnpStateVariable(sendEvents = false)
        private CSV<Boolean> booleanVar;

        @UpnpStateVariable(sendEvents = false)
        private CSV<UnsignedIntegerFourBytes> uifourVar;

        @UpnpAction
        public void setStringVar(@UpnpInputArgument(name = "StringVar") CSVString stringVar) {
            this.stringVar = stringVar;
            assertEquals(3, stringVar.size());
            assertEquals("f\\oo", stringVar.get(0));
            assertEquals("bar", stringVar.get(1));
            assertEquals("b,az", stringVar.get(2));
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "StringVar"))
        public CSV<String> getStringVar() {
            return stringVar;
        }

        @UpnpAction
        public void setIntVar(@UpnpInputArgument(name = "IntVar") CSVInteger intVar) {
            this.intVar = intVar;
            assertEquals(3, intVar.size());
            assertEquals(123, intVar.get(0));
            assertEquals(-456, intVar.get(1));
            assertEquals(789, intVar.get(2));
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "IntVar"))
        public CSV<Integer> getIntVar() {
            return intVar;
        }

        @UpnpAction
        public void setBooleanVar(@UpnpInputArgument(name = "BooleanVar") CSVBoolean booleanVar) {
            this.booleanVar = booleanVar;
            assertEquals(3, booleanVar.size());
            assertTrue(booleanVar.get(0));
            assertTrue(booleanVar.get(1));
            assertFalse(booleanVar.get(2));
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "BooleanVar"))
        public CSV<Boolean> getBooleanVar() {
            return booleanVar;
        }

        @UpnpAction
        public void setUifourVar(@UpnpInputArgument(name = "UifourVar") CSVUnsignedIntegerFourBytes uifourVar) {
            this.uifourVar = uifourVar;
            assertEquals(3, uifourVar.size());
            assertEquals(new UnsignedIntegerFourBytes(123), uifourVar.get(0));
            assertEquals(new UnsignedIntegerFourBytes(456), uifourVar.get(1));
            assertEquals(new UnsignedIntegerFourBytes(789), uifourVar.get(2));
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "UifourVar"))
        public CSV<UnsignedIntegerFourBytes> getUifourVar() {
            return uifourVar;
        }
    }
}
