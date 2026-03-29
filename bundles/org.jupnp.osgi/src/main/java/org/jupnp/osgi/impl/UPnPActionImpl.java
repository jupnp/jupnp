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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.osgi.util.OSGiContext;
import org.jupnp.osgi.util.OSGiDataConverter;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Green
 */
public class UPnPActionImpl implements UPnPAction {

    private final Logger logger = LoggerFactory.getLogger(UPnPActionImpl.class);

    private Action<?> action;

    public UPnPActionImpl(Action<?> action) {
        this.action = action;
    }

    @Override
    public String getName() {
        return action.getName();
    }

    @Override
    public String getReturnArgumentName() {
        String name = null;

        for (ActionArgument<?> argument : action.getArguments()) {
            if (argument.isReturnValue()) {
                name = argument.getName();
                break;
            }
        }

        return name;
    }

    @Override
    public String[] getInputArgumentNames() {
        List<String> list = new ArrayList<>();
        for (ActionArgument<?> argument : action.getInputArguments()) {
            list.add(argument.getName());
        }

        return !list.isEmpty() ? list.toArray(new String[list.size()]) : null;
    }

    @Override
    public String[] getOutputArgumentNames() {
        List<String> list = new ArrayList<>();
        for (ActionArgument<?> argument : action.getOutputArguments()) {
            list.add(argument.getName());
        }

        return !list.isEmpty() ? list.toArray(new String[list.size()]) : null;
    }

    @Override
    public UPnPStateVariable getStateVariable(String argumentName) {
        StateVariable variable = null;

        ActionArgument<?> argument = action.getInputArgument(argumentName);
        if (argument == null) {
            argument = action.getOutputArgument(argumentName);
        }
        if (argument != null) {
            String name = argument.getRelatedStateVariableName();
            variable = action.getService().getStateVariable(name);
        }

        return argument != null ? new UPnPStateVariableImpl(variable) : null;
    }

    @Override
    public Dictionary invoke(Dictionary args) throws Exception {
        Dictionary<Object, Object> output = null;

        List<ActionArgumentValue<?>> input = new ArrayList<>();

        if (args != null) {
            for (String key : (ArrayList<String>) Collections.list(args.keys())) {
                ActionArgument<?> argument = action.getInputArgument(key);

                Object value = args.get(key);
                // System.out.printf("key: %s value: %s\n", key, value);

                if (!value.getClass().equals(argument.getDatatype().getBuiltin().getDeclaringClass())) {
                    value = OSGiDataConverter.tojUPnPValue(argument.getDatatype().getBuiltin().getDescriptorName(),
                            value);
                    // System.out.printf("key: %s value: %s\n", key, value);
                }

                input.add(new ActionArgumentValue<>(argument, value));
            }
        }

        ControlPoint controlPoint = OSGiContext.getUpnpService().getControlPoint();
        ActionInvocation actionInvocation = new ActionInvocation(action,
                input.toArray(new ActionArgumentValue[input.size()]));

        new ActionCallback.Default(actionInvocation, controlPoint).run();

        if (actionInvocation.getFailure() == null) {
            ActionArgumentValue<?>[] arguments = actionInvocation.getOutput();
            if (arguments != null && arguments.length != 0) {
                output = new Hashtable<>();
                for (ActionArgumentValue<?> argument : arguments) {
                    String name = argument.getArgument().getName();
                    Object value = argument.getValue();

                    if (value == null) {
                        logger.error("Received null value for variable {} to OSGi type {}.", name,
                                argument.getDatatype().getDisplayString());
                        // throw an exception
                    } else {
                        // System.out.printf("name: %s value: %s (%s)\n", name, value, value.getClass().getName());

                        value = OSGiDataConverter.toOSGiValue(argument.getDatatype(), value);

                        if (value == null) {
                            logger.error("Cannot convert variable {} to OSGi type {}.", name,
                                    argument.getDatatype().getDisplayString());
                            // throw an exception
                        }
                        output.put(name, value);
                    }
                }
            }
        }

        return output;
    }
}
