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
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.DeviceType;

class StringConvertibleTest {

    static LocalDevice createTestDevice(Class serviceClass) throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(serviceClass);
        svc.setManager(new DefaultServiceManager(svc, serviceClass));

        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new DeviceType("mydomain", "CustomDevice", 1),
                new DeviceDetails("A Custom Device"), svc);
    }

    static Object[][] getDevices() throws Exception {
        return new LocalDevice[][] { { createTestDevice(MyServiceWithStringConvertibles.class) }, };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void validateBinding(LocalDevice device) {
        LocalService svc = device.getServices()[0];

        assertEquals(4, svc.getStateVariables().length);
        for (StateVariable stateVariable : svc.getStateVariables()) {
            assertEquals(Datatype.Builtin.STRING, stateVariable.getTypeDetails().getDatatype().getBuiltin());
        }

        assertEquals(9, svc.getActions().length); // Has 8 actions plus QueryStateVariableAction!

        assertEquals(1, svc.getAction("SetMyURL").getArguments().length);
        assertEquals("In", svc.getAction("SetMyURL").getArguments()[0].getName());
        assertEquals(ActionArgument.Direction.IN, svc.getAction("SetMyURL").getArguments()[0].getDirection());
        assertEquals("MyURL", svc.getAction("SetMyURL").getArguments()[0].getRelatedStateVariableName());
        // The others are all the same...
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) {
        LocalService svc = device.getServices()[0];

        ActionInvocation setMyURL = new ActionInvocation(svc.getAction("SetMyURL"));
        setMyURL.setInput("In", "http://foo/bar");
        svc.getExecutor(setMyURL.getAction()).execute(setMyURL);
        assertNull(setMyURL.getFailure());
        assertEquals(0, setMyURL.getOutput().length);

        ActionInvocation getMyURL = new ActionInvocation(svc.getAction("GetMyURL"));
        svc.getExecutor(getMyURL.getAction()).execute(getMyURL);
        assertNull(getMyURL.getFailure());
        assertEquals(1, getMyURL.getOutput().length);
        assertEquals("http://foo/bar", getMyURL.getOutput()[0].toString());

        ActionInvocation setMyURI = new ActionInvocation(svc.getAction("SetMyURI"));
        setMyURI.setInput("In", "http://foo/bar");
        svc.getExecutor(setMyURI.getAction()).execute(setMyURI);
        assertNull(setMyURI.getFailure());
        assertEquals(0, setMyURI.getOutput().length);

        ActionInvocation getMyURI = new ActionInvocation(svc.getAction("GetMyURI"));
        svc.getExecutor(getMyURI.getAction()).execute(getMyURI);
        assertNull(getMyURI.getFailure());
        assertEquals(1, getMyURI.getOutput().length);
        assertEquals("http://foo/bar", getMyURI.getOutput()[0].toString());

        ActionInvocation setMyNumbers = new ActionInvocation(svc.getAction("SetMyNumbers"));
        setMyNumbers.setInput("In", "1,2,3");
        svc.getExecutor(setMyNumbers.getAction()).execute(setMyNumbers);
        assertNull(setMyNumbers.getFailure());
        assertEquals(0, setMyNumbers.getOutput().length);

        ActionInvocation getMyNumbers = new ActionInvocation(svc.getAction("GetMyNumbers"));
        svc.getExecutor(getMyNumbers.getAction()).execute(getMyNumbers);
        assertNull(getMyNumbers.getFailure());
        assertEquals(1, getMyNumbers.getOutput().length);
        assertEquals("1,2,3", getMyNumbers.getOutput()[0].toString());

        ActionInvocation setMyStringConvertible = new ActionInvocation(svc.getAction("SetMyStringConvertible"));
        setMyStringConvertible.setInput("In", "foobar");
        svc.getExecutor(setMyStringConvertible.getAction()).execute(setMyStringConvertible);
        assertNull(setMyStringConvertible.getFailure());
        assertEquals(0, setMyStringConvertible.getOutput().length);

        ActionInvocation getMyStringConvertible = new ActionInvocation(svc.getAction("GetMyStringConvertible"));
        svc.getExecutor(getMyStringConvertible.getAction()).execute(getMyStringConvertible);
        assertNull(getMyStringConvertible.getFailure());
        assertEquals(1, getMyStringConvertible.getOutput().length);
        assertEquals("foobar", getMyStringConvertible.getOutput()[0].toString());
    }
}
