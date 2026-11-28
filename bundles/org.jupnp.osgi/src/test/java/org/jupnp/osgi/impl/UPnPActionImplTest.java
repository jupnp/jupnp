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
package org.jupnp.osgi.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Proxy;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpService;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.Datatype.Builtin;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.osgi.util.OSGiContext;

class UPnPActionImplTest {

    @Test
    void keepsEmptyBinaryOutputs() throws Exception {
        assertArrayEquals(new byte[0], (byte[]) invokeWithEmptyOutput(Builtin.BIN_BASE64).get("Payload"));
        assertArrayEquals(new byte[0], (byte[]) invokeWithEmptyOutput(Builtin.BIN_HEX).get("Payload"));
    }

    @Test
    void keepsEmptyStringOutput() throws Exception {
        Dictionary output = invokeWithEmptyOutput(Builtin.STRING);
        assertEquals(1, output.size());
        assertEquals("", output.get("Payload"));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Dictionary invokeWithEmptyOutput(Builtin type) throws Exception {
        Action action = new Action("GetPayload",
                new ActionArgument[] { new ActionArgument("Payload", "Data", ActionArgument.Direction.OUT) });
        StateVariable variable = new StateVariable("Data", new StateVariableTypeDetails(type.getDatatype()));
        ActionExecutor executor = invocation -> invocation.setOutput("Payload", "");
        Map<StateVariable, StateVariableAccessor> variables = new HashMap<>();
        variables.put(variable, null);
        new LocalService(new UDAServiceType("Payload", 1), new UDAServiceId("Payload"), Map.of(action, executor),
                variables, Set.of(), false);

        UpnpService service = (UpnpService) Proxy.newProxyInstance(UpnpService.class.getClassLoader(),
                new Class<?>[] { UpnpService.class }, (proxy, method, args) -> null);
        OSGiContext context = new OSGiContext(null, service);
        try {
            return new UPnPActionImpl(action).invoke(null);
        } finally {
            context.deactivate();
        }
    }
}
