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
package org.jupnp.model.meta;

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.ModelUtil;
import org.jupnp.model.Validatable;
import org.jupnp.model.ValidationError;
import org.jupnp.model.types.Datatype;
import org.jupnp.util.SpecificationViolationReporter;

/**
 * Describes a single action argument, either input or output.
 * <p>
 * No, I haven't figured out so far what the "return value" thingy is good for.
 * </p>
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public class ActionArgument<S extends Service> implements Validatable {

    public enum Direction {
        IN,
        OUT
    }

    private final String name;
    private final String[] aliases;
    private final String relatedStateVariableName;
    private final Direction direction;
    private final boolean returnValue; // TODO: What is this stuff good for anyway?

    // Package mutable state
    private Action<S> action;

    public ActionArgument(String name, String relatedStateVariableName, Direction direction) {
        this(name, new String[0], relatedStateVariableName, direction, false);
    }

    public ActionArgument(String name, String[] aliases, String relatedStateVariableName, Direction direction) {
        this(name, aliases, relatedStateVariableName, direction, false);
    }

    public ActionArgument(String name, String relatedStateVariableName, Direction direction, boolean returnValue) {
        this(name, new String[0], relatedStateVariableName, direction, returnValue);
    }

    public ActionArgument(String name, String[] aliases, String relatedStateVariableName, Direction direction,
            boolean returnValue) {
        this.name = name;
        this.aliases = aliases;
        this.relatedStateVariableName = relatedStateVariableName;
        this.direction = direction;
        this.returnValue = returnValue;
    }

    public String getName() {
        return name;
    }

    public String[] getAliases() {
        return aliases;
    }

    public boolean isNameOrAlias(String name) {
        if (getName().equalsIgnoreCase(name)) {
            return true;
        }
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public String getRelatedStateVariableName() {
        return relatedStateVariableName;
    }

    public Direction getDirection() {
        return direction;
    }

    public boolean isReturnValue() {
        return returnValue;
    }

    public Action<S> getAction() {
        return action;
    }

    void setAction(Action<S> action) {
        if (this.action != null) {
            throw new IllegalStateException("Final value has been set already, model is immutable");
        }
        this.action = action;
    }

    public Datatype getDatatype() {
        return getAction().getService().getDatatype(this);
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (getName() == null || getName().isEmpty()) {
            errors.add(new ValidationError(getClass(), "name", "Argument without name of: " + getAction()));
        } else if (!ModelUtil.isValidUDAName(getName())) {
            SpecificationViolationReporter.report(getAction().getService().getDevice(), "Invalid argument name: {}",
                    this);
        } else if (getName().length() > 32) {
            SpecificationViolationReporter.report(getAction().getService().getDevice(),
                    "Argument name should be less than 32 characters: {}", this);
        }

        if (getDirection() == null) {
            errors.add(new ValidationError(getClass(), "direction",
                    "Argument '" + getName() + "' requires a direction, either IN or OUT"));
        }

        if (isReturnValue() && getDirection() != ActionArgument.Direction.OUT) {
            errors.add(new ValidationError(getClass(), "direction",
                    "Return value argument '" + getName() + "' must be direction OUT"));
        }

        return errors;
    }

    public ActionArgument<S> deepCopy() {
        return new ActionArgument<>(getName(), getAliases(), getRelatedStateVariableName(), getDirection(),
                isReturnValue());
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ", " + getDirection() + ") " + getName();
    }
}
