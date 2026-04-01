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
package example.controlpoint;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.BooleanDatatype;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;

import example.binarylight.BinaryLightSampleData;

class ActionInvocationTest {

    static LocalService bindService(Class<?> clazz) throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        // Let's also test the overloaded reader
        LocalService svc = binder.read(clazz, new UDAServiceId("SwitchPower"), new UDAServiceType("SwitchPower", 1),
                true, new Class[] { MyString.class });
        svc.setManager(new DefaultServiceManager(svc, clazz));
        return svc;
    }

    static Object[][] getDevices() throws Exception {
        return new LocalDevice[][] { { BinaryLightSampleData.createDevice(bindService(TestServiceOne.class)) },
                { BinaryLightSampleData.createDevice(bindService(TestServiceTwo.class)) },
                { BinaryLightSampleData.createDevice(bindService(TestServiceThree.class)) }, };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        Service service = device.findService(new UDAServiceId("SwitchPower"));
        Action getStatusAction = service.getAction("GetStatus");

        final boolean[] tests = new boolean[3];

        ActionInvocation getStatusInvocation = new ActionInvocation(getStatusAction);

        ActionCallback getStatusCallback = new ActionCallback(getStatusInvocation) {

            @Override
            public void success(ActionInvocation invocation) {
                ActionArgumentValue status = invocation.getOutput("ResultStatus");

                assertNotNull(status);

                assertEquals("ResultStatus", status.getArgument().getName());

                assertEquals(BooleanDatatype.class, status.getDatatype().getClass());
                assertEquals(Datatype.Builtin.BOOLEAN, status.getDatatype().getBuiltin());

                assertEquals(Boolean.FALSE, status.getValue());
                assertEquals("0", status.toString()); // '0' is 'false' in UPnP
                tests[0] = true;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                System.err.println(defaultMsg);
            }
        };

        upnpService.getControlPoint().execute(getStatusCallback);

        Action action = service.getAction("SetTarget");

        ActionInvocation setTargetInvocation = new ActionInvocation(action);

        setTargetInvocation.setInput("NewTargetValue", true); // Can throw InvalidValueException

        ActionCallback setTargetCallback = new ActionCallback(setTargetInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                ActionArgumentValue[] output = invocation.getOutput();
                assertEquals(0, output.length);
                tests[1] = true;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                System.err.println(defaultMsg);
            }
        };

        upnpService.getControlPoint().execute(setTargetCallback);

        getStatusInvocation = new ActionInvocation(getStatusAction);
        new ActionCallback.Default(getStatusInvocation, upnpService.getControlPoint()).run();
        ActionArgumentValue status = getStatusInvocation.getOutput("ResultStatus");
        if (Boolean.valueOf(true).equals(status.getValue())) {
            tests[2] = true;
        }

        for (boolean test : tests) {
            assertTrue(test);
        }

        LocalService svc = (LocalService) service;

        ActionInvocation getTargetInvocation = new ActionInvocation(svc.getAction("GetTarget"));
        svc.getExecutor(getTargetInvocation.getAction()).execute(getTargetInvocation);
        assertNull(getTargetInvocation.getFailure());
        assertEquals(1, getTargetInvocation.getOutput().length);
        assertEquals("1", getTargetInvocation.getOutput()[0].toString());

        ActionInvocation setMyStringInvocation = new ActionInvocation(svc.getAction("SetMyString"));
        setMyStringInvocation.setInput("MyString", "foo");
        svc.getExecutor(setMyStringInvocation.getAction()).execute(setMyStringInvocation);
        assertNull(setMyStringInvocation.getFailure());
        assertEquals(0, setMyStringInvocation.getOutput().length);

        ActionInvocation getMyStringInvocation = new ActionInvocation(svc.getAction("GetMyString"));
        svc.getExecutor(getMyStringInvocation.getAction()).execute(getMyStringInvocation);
        assertNull(getTargetInvocation.getFailure());
        assertEquals(1, getMyStringInvocation.getOutput().length);
        assertEquals("foo", getMyStringInvocation.getOutput()[0].toString());
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActionsWithAlias(LocalDevice device) throws Exception {

        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        Service service = device.findService(new UDAServiceId("SwitchPower"));
        Action getStatusAction = service.getAction("GetStatus");

        final boolean[] tests = new boolean[1];

        Action action = service.getAction("SetTarget");
        ActionInvocation setTargetInvocation = new ActionInvocation(action);
        setTargetInvocation.setInput("NewTargetValue1", true);
        ActionCallback setTargetCallback = new ActionCallback(setTargetInvocation) {

            @Override
            public void success(ActionInvocation invocation) {
                ActionArgumentValue[] output = invocation.getOutput();
                assertEquals(output.length, 0);
                tests[0] = true;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                System.err.println(defaultMsg);
            }
        };
        upnpService.getControlPoint().execute(setTargetCallback);

        for (boolean test : tests) {
            assertTrue(test);
        }

        LocalService svc = (LocalService) service;

        ActionInvocation getTargetInvocation = new ActionInvocation(svc.getAction("GetTarget"));
        svc.getExecutor(getTargetInvocation.getAction()).execute(getTargetInvocation);
        assertNull(getTargetInvocation.getFailure());
        assertEquals(1, getTargetInvocation.getOutput().length);
        assertEquals("1", getTargetInvocation.getOutput()[0].toString());

        ActionInvocation setMyStringInvocation = new ActionInvocation(svc.getAction("SetMyString"));
        setMyStringInvocation.setInput("MyString1", "foo");
        svc.getExecutor(setMyStringInvocation.getAction()).execute(setMyStringInvocation);
        assertNull(setMyStringInvocation.getFailure());
        assertEquals(0, setMyStringInvocation.getOutput().length);

        ActionInvocation getMyStringInvocation = new ActionInvocation(svc.getAction("GetMyString"));
        svc.getExecutor(getMyStringInvocation.getAction()).execute(getMyStringInvocation);
        assertNull(getTargetInvocation.getFailure());
        assertEquals(1, getMyStringInvocation.getOutput().length);
        assertEquals("foo", getMyStringInvocation.getOutput()[0].toString());
    }

    /* ####################################################################################################### */

    public static class TestServiceOne {

        @UpnpStateVariable(sendEvents = false)
        private boolean target = false;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpStateVariable(sendEvents = false)
        private MyString myString;

        @UpnpAction
        public void setTarget(
                @UpnpInputArgument(name = "NewTargetValue", aliases = { "NewTargetValue1" }) boolean newTargetValue) {
            target = newTargetValue;
            status = newTargetValue;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
        public boolean getTarget() {
            return target;
        }

        @UpnpAction(name = "GetStatus", out = @UpnpOutputArgument(name = "ResultStatus", getterName = "getStatus"))
        public void dummyStatus() {
            // NOOP
        }

        public boolean getStatus() {
            return status;
        }

        @UpnpAction
        public void setMyString(@UpnpInputArgument(name = "MyString", aliases = { "MyString1" }) MyString myString) {
            this.myString = myString;
        }

        @UpnpAction(name = "GetMyString", out = @UpnpOutputArgument(name = "MyString", getterName = "getMyString"))
        public void getMyStringDummy() {
        }

        public MyString getMyString() {
            return myString;
        }
    }

    public static class TestServiceTwo {

        @UpnpStateVariable(sendEvents = false)
        private boolean target = false;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpStateVariable(sendEvents = false)
        private MyString myString;

        @UpnpAction
        public void setTarget(
                @UpnpInputArgument(name = "NewTargetValue", aliases = { "NewTargetValue1" }) boolean newTargetValue) {
            target = newTargetValue;
            status = newTargetValue;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
        public boolean getTarget() {
            return target;
        }

        @UpnpAction(name = "GetStatus", out = @UpnpOutputArgument(name = "ResultStatus", getterName = "getStatus"))
        public StatusHolder dummyStatus() {
            return new StatusHolder(status);
        }

        @UpnpAction
        public void setMyString(@UpnpInputArgument(name = "MyString", aliases = { "MyString1" }) MyString myString) {
            this.myString = myString;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "MyString", getterName = "getMyString"))
        public MyStringHolder getMyString() {
            return new MyStringHolder(myString);
        }

        public static class StatusHolder {
            boolean st;

            public StatusHolder(boolean st) {
                this.st = st;
            }

            public boolean getStatus() {
                return st;
            }
        }

        public static class MyStringHolder {
            MyString myString;

            public MyStringHolder(MyString myString) {
                this.myString = myString;
            }

            public MyString getMyString() {
                return myString;
            }
        }
    }

    public static class TestServiceThree {

        @UpnpStateVariable(sendEvents = false)
        private boolean target = false;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpStateVariable(sendEvents = false)
        private MyString myString;

        @UpnpAction
        public void setTarget(
                @UpnpInputArgument(name = "NewTargetValue", aliases = { "NewTargetValue1" }) boolean newTargetValue) {
            target = newTargetValue;
            status = newTargetValue;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
        public boolean getTarget() {
            return target;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }

        @UpnpAction
        public void setMyString(@UpnpInputArgument(name = "MyString", aliases = { "MyString1" }) MyString myString) {
            this.myString = myString;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "MyString"))
        public MyString getMyString() {
            return myString;
        }
    }

    public static class MyString {
        private String s;

        public MyString(String s) {
            this.s = s;
        }

        public String getS() {
            return s;
        }

        @Override
        public String toString() {
            return s;
        }
    }
}
