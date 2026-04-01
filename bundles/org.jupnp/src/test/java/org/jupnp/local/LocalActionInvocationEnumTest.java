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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
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

class LocalActionInvocationEnumTest {

    static LocalDevice createTestDevice(LocalService service) throws Exception {
        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("BinaryLight", 1),
                new DeviceDetails("Example Binary Light"), service);
    }

    static Object[][] getDevices() throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        return new LocalDevice[][] { { createTestDevice(SampleData.readService(binder, TestServiceOne.class)) },
                { createTestDevice(SampleData.readService(binder, TestServiceTwo.class)) },
                { createTestDevice(SampleData.readService(binder, TestServiceThree.class)) }, };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) throws Exception {

        LocalService svc = SampleData.getFirstService(device);

        ActionInvocation checkTargetInvocation = new ActionInvocation(svc.getAction("GetTarget"));
        svc.getExecutor(checkTargetInvocation.getAction()).execute(checkTargetInvocation);
        assertNull(checkTargetInvocation.getFailure());
        assertEquals(1, checkTargetInvocation.getOutput().length);
        assertEquals("UNKNOWN", checkTargetInvocation.getOutput()[0].toString());

        ActionInvocation setTargetInvocation = new ActionInvocation(svc.getAction("SetTarget"));
        setTargetInvocation.setInput("NewTargetValue", "ON");
        svc.getExecutor(setTargetInvocation.getAction()).execute(setTargetInvocation);
        assertNull(setTargetInvocation.getFailure());
        assertEquals(0, setTargetInvocation.getOutput().length);

        ActionInvocation getTargetInvocation = new ActionInvocation(svc.getAction("GetTarget"));
        svc.getExecutor(getTargetInvocation.getAction()).execute(getTargetInvocation);
        assertNull(getTargetInvocation.getFailure());
        assertEquals(1, getTargetInvocation.getOutput().length);
        assertEquals("ON", getTargetInvocation.getOutput()[0].toString());

        ActionInvocation getStatusInvocation = new ActionInvocation(svc.getAction("GetStatus"));
        svc.getExecutor(getStatusInvocation.getAction()).execute(getStatusInvocation);
        assertNull(getStatusInvocation.getFailure());
        assertEquals(1, getStatusInvocation.getOutput().length);
        assertEquals("1", getStatusInvocation.getOutput()[0].toString());
    }

    /* ####################################################################################################### */

    @UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
    public static class TestServiceOne {

        public enum Target {
            ON,
            OFF,
            UNKNOWN
        }

        @UpnpStateVariable(sendEvents = false)
        private Target target = Target.UNKNOWN;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") String newTargetValue) {
            target = Target.valueOf(newTargetValue);

            status = target == Target.ON;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
        public Target getTarget() {
            return target;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }
    }

    /* ####################################################################################################### */

    @UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
    public static class TestServiceTwo {

        public enum Target {
            ON,
            OFF,
            UNKNOWN
        }

        @UpnpStateVariable(sendEvents = false)
        private Target target = Target.UNKNOWN;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") String newTargetValue) {
            target = Target.valueOf(newTargetValue);

            status = target == Target.ON;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue", stateVariable = "Target", getterName = "getRealTarget"))
        public void getTarget() {
        }

        public Target getRealTarget() {
            return target;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }
    }

    /* ####################################################################################################### */

    @UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
    public static class TestServiceThree {

        public enum Target {
            ON,
            OFF,
            UNKNOWN
        }

        public static class TargetHolder {
            private Target t;

            public TargetHolder(Target t) {
                this.t = t;
            }

            public Target getTarget() {
                return t;
            }
        }

        @UpnpStateVariable(sendEvents = false)
        private Target target = Target.UNKNOWN;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") String newTargetValue) {
            target = Target.valueOf(newTargetValue);

            status = target == Target.ON;
        }

        @UpnpAction(name = "GetTarget", out = @UpnpOutputArgument(name = "RetTargetValue", getterName = "getTarget"))
        public TargetHolder getTargetHolder() {
            return new TargetHolder(target);
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }
    }

    /* ####################################################################################################### */

    @UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
    public static class TestServiceFour {

        public enum Target {
            ON,
            OFF,
            UNKNOWN
        }

        public static class TargetHolder {
            private Target t;

            public TargetHolder(Target t) {
                this.t = t;
            }

            public Target getT() {
                return t;
            }
        }

        @UpnpStateVariable(sendEvents = false)
        private Target target = Target.UNKNOWN;

        @UpnpStateVariable
        private boolean status = false;

        @UpnpAction
        public void setTarget(@UpnpInputArgument(name = "NewTargetValue") String newTargetValue) {
            target = Target.valueOf(newTargetValue);

            status = target == Target.ON;
        }

        @UpnpAction(name = "GetTarget", out = @UpnpOutputArgument(name = "RetTargetValue", stateVariable = "Target", getterName = "getT"))
        public TargetHolder getTargetHolder() {
            return new TargetHolder(target);
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
        public boolean getStatus() {
            return status;
        }
    }
}
