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
package org.jupnp.osgi.present;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;

class UPnPActionExecutorTest {

    private Action<LocalService> action;
    private UPnPActionExecutor executor;
    private final List<Dictionary> invocations = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        action = new Action("SetTarget",
                new ActionArgument[] { new ActionArgument("NewTargetValue", "Target", ActionArgument.Direction.IN) });
        new LocalService(new UDAServiceType("SwitchPower"), new UDAServiceId("SwitchPower"), new Action[] { action },
                new StateVariable[] { new StateVariable("Target",
                        new StateVariableTypeDetails(Datatype.Builtin.BOOLEAN.getDatatype())) });

        executor = new UPnPActionExecutor(new UPnPAction() {
            @Override
            public String getName() {
                return action.getName();
            }

            @Override
            public String getReturnArgumentName() {
                return null;
            }

            @Override
            public String[] getInputArgumentNames() {
                return new String[] { "NewTargetValue" };
            }

            @Override
            public String[] getOutputArgumentNames() {
                return null;
            }

            @Override
            public UPnPStateVariable getStateVariable(String argumentName) {
                return null;
            }

            @Override
            public Dictionary invoke(Dictionary args) {
                invocations.add(args);
                return null;
            }
        });
    }

    private ActionInvocation<LocalService> execute(String value) {
        ActionInvocation<LocalService> invocation = new ActionInvocation<>(action, new ActionArgumentValue[] {
                new ActionArgumentValue<>(action.getInputArgument("NewTargetValue"), value) });
        executor.execute(invocation);
        return invocation;
    }

    @Test
    void convertedInputIsPassedToAction() {
        ActionInvocation<LocalService> invocation = execute("1");

        assertNull(invocation.getFailure());
        assertEquals(1, invocations.size());
        assertEquals(Boolean.TRUE, invocations.get(0).get("NewTargetValue"));
    }

    @Test
    void emptyInputFailsInvocation() {
        // An empty boolean has no OSGi value
        ActionInvocation<LocalService> invocation = execute("");

        assertNotNull(invocation.getFailure());
        assertEquals(ErrorCode.ARGUMENT_VALUE_INVALID.getCode(), invocation.getFailure().getErrorCode());
        assertTrue(invocations.isEmpty());
    }
}
