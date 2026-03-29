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
 * The metadata of a named state variable.
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public class StateVariable<S extends Service> implements Validatable {

    private final String name;
    private final StateVariableTypeDetails type;
    private final StateVariableEventDetails eventDetails;

    // Package mutable state
    private S service;

    public StateVariable(String name, StateVariableTypeDetails type) {
        this(name, type, new StateVariableEventDetails());
    }

    public StateVariable(String name, StateVariableTypeDetails type, StateVariableEventDetails eventDetails) {
        this.name = name;
        this.type = type;
        this.eventDetails = eventDetails;
    }

    public String getName() {
        return name;
    }

    public StateVariableTypeDetails getTypeDetails() {
        return type;
    }

    public StateVariableEventDetails getEventDetails() {
        return eventDetails;
    }

    public S getService() {
        return service;
    }

    void setService(S service) {
        if (this.service != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.service = service;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (getName() == null || getName().isEmpty()) {
            errors.add(new ValidationError(getClass(), "name", "StateVariable without name of: " + getService()));
        } else if (!ModelUtil.isValidUDAName(getName())) {
            SpecificationViolationReporter.report(getService().getDevice(), "Invalid state variable name: {}", this);
        }

        errors.addAll(getTypeDetails().validate());

        return errors;
    }

    public boolean isModeratedNumericType() {
        return Datatype.Builtin.isNumeric(getTypeDetails().getDatatype().getBuiltin())
                && getEventDetails().getEventMinimumDelta() > 0;
    }

    public StateVariable<S> deepCopy() {
        return new StateVariable<>(getName(), getTypeDetails(), getEventDetails());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(getClass().getSimpleName());
        sb.append(", Name: ").append(getName());
        sb.append(", Type: ").append(getTypeDetails().getDatatype().getDisplayString()).append(")");
        if (!getEventDetails().isSendEvents()) {
            sb.append(" (No Events)");
        }
        if (getTypeDetails().getDefaultValue() != null) {
            sb.append(" Default Value: ").append("'").append(getTypeDetails().getDefaultValue()).append("'");
        }
        if (getTypeDetails().getAllowedValues() != null) {
            sb.append(" Allowed Values: ");
            for (String s : getTypeDetails().getAllowedValues()) {
                sb.append(s).append("|");
            }
        }
        return sb.toString();
    }
}
