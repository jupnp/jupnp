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
package org.jupnp.device.simple.actions;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.jupnp.device.simple.variables.TestStateVariable;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;

public class SetAction implements UPnPAction {
    private String name;
    private String[] argumentNames;
    private Map<String, UPnPStateVariable> variables = new HashMap<>();

    public SetAction(String name, TestStateVariable[] variables) {
        this.name = name;

        this.argumentNames = new String[variables.length];
        for (int i = 0; i < variables.length; i++) {
            this.argumentNames[i] = String.format("%s", variables[i].getName());
            this.variables.put(argumentNames[i], variables[i]);
        }
    }

    public SetAction(TestStateVariable variable) {
        this(String.format("Set%s", variable.getName()), new TestStateVariable[] { variable });
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getReturnArgumentName() {
        return null;
    }

    @Override
    public String[] getInputArgumentNames() {
        return argumentNames;
    }

    @Override
    public String[] getOutputArgumentNames() {
        return null;
    }

    @Override
    public UPnPStateVariable getStateVariable(String argumentName) {
        return variables.get(argumentName);
    }

    @Override
    public Dictionary invoke(Dictionary args) throws Exception {
        for (Object key : Collections.list(args.keys())) {
            String name = (String) key;
            Object value = args.get(key);

            TestStateVariable variable = (TestStateVariable) variables.get(name);
            variable.setCurrentValue(value);
        }

        return null;
    }
}
