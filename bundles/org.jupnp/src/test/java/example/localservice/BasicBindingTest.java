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
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;

class BasicBindingTest {

    static LocalDevice createTestDevice(Class serviceClass) throws Exception {

        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(serviceClass);
        svc.setManager(new DefaultServiceManager(svc, serviceClass));

        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("BinaryLight", 1),
                new DeviceDetails("Example Binary Light"), svc);
    }

    static Object[][] getDevices() {
        try {
            return new LocalDevice[][] { { createTestDevice(SwitchPowerNamedStateVariable.class) },
                    { createTestDevice(SwitchPowerAnnotatedClass.class) },
                    { createTestDevice(SwitchPowerExtraGetter.class) },
                    { createTestDevice(SwitchPowerBeanReturn.class) }, };
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

        assertEquals("urn:" + UDAServiceId.DEFAULT_NAMESPACE + ":serviceId:SwitchPower", svc.getServiceId().toString());
        assertEquals("urn:" + UDAServiceType.DEFAULT_NAMESPACE + ":service:SwitchPower:1",
                svc.getServiceType().toString());

        assertEquals(2, svc.getStateVariables().length);
        assertEquals(Datatype.Builtin.BOOLEAN,
                svc.getStateVariable("Target").getTypeDetails().getDatatype().getBuiltin());
        assertEquals("0", svc.getStateVariable("Target").getTypeDetails().getDefaultValue());
        assertFalse(svc.getStateVariable("Target").getEventDetails().isSendEvents());

        assertEquals(Datatype.Builtin.BOOLEAN,
                svc.getStateVariable("Status").getTypeDetails().getDatatype().getBuiltin());
        assertEquals("0", svc.getStateVariable("Status").getTypeDetails().getDefaultValue());
        assertTrue(svc.getStateVariable("Status").getEventDetails().isSendEvents());

        assertEquals(4, svc.getActions().length); // Has 3 actions plus QueryStateVariableAction!

        assertEquals("SetTarget", svc.getAction("SetTarget").getName());
        assertEquals(1, svc.getAction("SetTarget").getArguments().length);
        assertEquals("NewTargetValue", svc.getAction("SetTarget").getArguments()[0].getName());
        assertEquals(ActionArgument.Direction.IN, svc.getAction("SetTarget").getArguments()[0].getDirection());
        assertEquals("Target", svc.getAction("SetTarget").getArguments()[0].getRelatedStateVariableName());

        assertEquals("GetTarget", svc.getAction("GetTarget").getName());
        assertEquals(1, svc.getAction("GetTarget").getArguments().length);
        assertEquals("RetTargetValue", svc.getAction("GetTarget").getArguments()[0].getName());
        assertEquals(ActionArgument.Direction.OUT, svc.getAction("GetTarget").getArguments()[0].getDirection());
        assertEquals("Target", svc.getAction("GetTarget").getArguments()[0].getRelatedStateVariableName());
        assertTrue(svc.getAction("GetTarget").getArguments()[0].isReturnValue());

        assertEquals("GetStatus", svc.getAction("GetStatus").getName());
        assertEquals(1, svc.getAction("GetStatus").getArguments().length);
        assertEquals("ResultStatus", svc.getAction("GetStatus").getArguments()[0].getName());
        assertEquals(ActionArgument.Direction.OUT, svc.getAction("GetStatus").getArguments()[0].getDirection());
        assertEquals("Status", svc.getAction("GetStatus").getArguments()[0].getRelatedStateVariableName());
        assertTrue(svc.getAction("GetStatus").getArguments()[0].isReturnValue());
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) {
        // We mostly care about the binding without exceptions, but let's also test invocation
        LocalService svc = device.getServices()[0];

        ActionInvocation setTargetInvocation = new ActionInvocation(svc.getAction("SetTarget"));
        setTargetInvocation.setInput("NewTargetValue", true);
        svc.getExecutor(setTargetInvocation.getAction()).execute(setTargetInvocation);
        assertNull(setTargetInvocation.getFailure());
        assertEquals(0, setTargetInvocation.getOutput().length);

        ActionInvocation getStatusInvocation = new ActionInvocation(svc.getAction("GetStatus"));
        svc.getExecutor(getStatusInvocation.getAction()).execute(getStatusInvocation);
        assertNull(getStatusInvocation.getFailure());
        assertEquals(1, getStatusInvocation.getOutput().length);
        assertEquals("1", getStatusInvocation.getOutput()[0].toString());

        setTargetInvocation = new ActionInvocation(svc.getAction("SetTarget"));
        setTargetInvocation.setInput("NewTargetValue", false);
        svc.getExecutor(setTargetInvocation.getAction()).execute(setTargetInvocation);
        assertNull(setTargetInvocation.getFailure());
        assertEquals(0, setTargetInvocation.getOutput().length);

        ActionInvocation queryStateVariableInvocation = new ActionInvocation(svc.getAction("QueryStateVariable"));
        queryStateVariableInvocation.setInput("varName", "Status");
        svc.getExecutor(queryStateVariableInvocation.getAction()).execute(queryStateVariableInvocation);
        assertNull(queryStateVariableInvocation.getFailure());
        assertEquals(1, queryStateVariableInvocation.getOutput().length);
        assertEquals("0", queryStateVariableInvocation.getOutput()[0].toString());
    }
}
