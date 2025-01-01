/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.model.meta;

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.ModelUtil;
import org.jupnp.model.Validatable;
import org.jupnp.model.ValidationError;
import org.jupnp.util.SpecificationViolationReporter;

/**
 * Describes an action and its input/output arguments.
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public class Action<S extends Service> implements Validatable {

    private final String name;
    private final ActionArgument[] arguments;
    private final ActionArgument[] inputArguments;
    private final ActionArgument[] outputArguments;

    // Package mutable state
    private S service;

    public Action(String name, ActionArgument[] arguments) {
        this.name = name;
        if (arguments != null) {

            List<ActionArgument> inputList = new ArrayList<>();
            List<ActionArgument> outputList = new ArrayList<>();

            for (ActionArgument argument : arguments) {
                argument.setAction(this);
                if (argument.getDirection().equals(ActionArgument.Direction.IN))
                    inputList.add(argument);
                if (argument.getDirection().equals(ActionArgument.Direction.OUT))
                    outputList.add(argument);
            }

            this.arguments = arguments;
            this.inputArguments = inputList.toArray(new ActionArgument[inputList.size()]);
            this.outputArguments = outputList.toArray(new ActionArgument[outputList.size()]);
        } else {
            this.arguments = new ActionArgument[0];
            this.inputArguments = new ActionArgument[0];
            this.outputArguments = new ActionArgument[0];
        }
    }

    public String getName() {
        return name;
    }

    public boolean hasArguments() {
        return getArguments() != null && getArguments().length > 0;
    }

    public ActionArgument[] getArguments() {
        return arguments;
    }

    public S getService() {
        return service;
    }

    void setService(S service) {
        if (this.service != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.service = service;
    }

    public ActionArgument<S> getFirstInputArgument() {
        if (!hasInputArguments())
            throw new IllegalStateException("No input arguments: " + this);
        return getInputArguments()[0];
    }

    public ActionArgument<S> getFirstOutputArgument() {
        if (!hasOutputArguments())
            throw new IllegalStateException("No output arguments: " + this);
        return getOutputArguments()[0];
    }

    public ActionArgument<S>[] getInputArguments() {
        return inputArguments;
    }

    public ActionArgument<S> getInputArgument(String name) {
        for (ActionArgument<S> arg : getInputArguments()) {
            if (arg.isNameOrAlias(name)) {
                return arg;
            }
        }
        return null;
    }

    public ActionArgument<S>[] getOutputArguments() {
        return outputArguments;
    }

    public ActionArgument<S> getOutputArgument(String name) {
        for (ActionArgument<S> arg : getOutputArguments()) {
            if (arg.getName().equals(name)) {
                return arg;
            }
        }
        return null;
    }

    public boolean hasInputArguments() {
        return getInputArguments() != null && getInputArguments().length > 0;
    }

    public boolean hasOutputArguments() {
        return getOutputArguments() != null && getOutputArguments().length > 0;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ", Arguments: "
                + (getArguments() != null ? getArguments().length : "NO ARGS") + ") " + getName();
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (getName() == null || getName().isEmpty()) {
            errors.add(new ValidationError(getClass(), "name", "Action without name of: " + getService()));
        } else if (!ModelUtil.isValidUDAName(getName())) {
            SpecificationViolationReporter.report(getService().getDevice(), "Invalid action name: {}", this);
        }

        for (ActionArgument actionArgument : getArguments()) {
            // Check argument relatedStateVariable in service state table

            if (getService().getStateVariable(actionArgument.getRelatedStateVariableName()) == null) {
                errors.add(new ValidationError(getClass(), "arguments",
                        "Action argument references an unknown state variable: "
                                + actionArgument.getRelatedStateVariableName()));
            }
        }

        ActionArgument retValueArgument = null;
        int retValueArgumentIndex = 0;
        int i = 0;
        for (ActionArgument actionArgument : getArguments()) {
            // Check retval
            if (actionArgument.isReturnValue()) {
                if (actionArgument.getDirection() == ActionArgument.Direction.IN) {
                    SpecificationViolationReporter.report(getService().getDevice(),
                            "Input argument can not have <retval/>");
                } else {
                    if (retValueArgument != null) {
                        SpecificationViolationReporter.report(getService().getDevice(),
                                "Only one argument of action '{}' can be <retval/>", getName());
                    }
                    retValueArgument = actionArgument;
                    retValueArgumentIndex = i;
                }
            }
            i++;
        }
        if (retValueArgument != null) {
            for (int j = 0; j < retValueArgumentIndex; j++) {
                ActionArgument a = getArguments()[j];
                if (a.getDirection() == ActionArgument.Direction.OUT) {
                    SpecificationViolationReporter.report(getService().getDevice(),
                            "Argument '{}' of action '{}' is <retval/> but not the first OUT argument",
                            retValueArgument.getName(), getName());
                }
            }
        }

        for (ActionArgument argument : arguments) {
            errors.addAll(argument.validate());
        }

        return errors;
    }

    public Action<S> deepCopy() {
        ActionArgument<S>[] actionArgumentsDupe = new ActionArgument[getArguments().length];
        for (int i = 0; i < getArguments().length; i++) {
            ActionArgument arg = getArguments()[i];
            actionArgumentsDupe[i] = arg.deepCopy();
        }

        return new Action<>(getName(), actionArgumentsDupe);
    }
}
