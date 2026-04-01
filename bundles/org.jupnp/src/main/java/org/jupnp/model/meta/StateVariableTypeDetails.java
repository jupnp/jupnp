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
import java.util.Arrays;
import java.util.List;

import org.jupnp.model.Validatable;
import org.jupnp.model.ValidationError;
import org.jupnp.model.types.Datatype;
import org.jupnp.util.SpecificationViolationReporter;

/**
 * Type of a state variable, its default value, and integrity rules for allowed values and ranges.
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public class StateVariableTypeDetails implements Validatable {

    private final Datatype datatype;
    private final String defaultValue;
    private final String[] allowedValues;
    private final StateVariableAllowedValueRange allowedValueRange;

    public StateVariableTypeDetails(Datatype datatype) {
        this(datatype, null, null, null);
    }

    public StateVariableTypeDetails(Datatype datatype, String defaultValue) {
        this(datatype, defaultValue, null, null);
    }

    public StateVariableTypeDetails(Datatype datatype, String defaultValue, String[] allowedValues,
            StateVariableAllowedValueRange allowedValueRange) {
        this.datatype = datatype;
        this.defaultValue = defaultValue;
        this.allowedValues = allowedValues;
        this.allowedValueRange = allowedValueRange;
    }

    public Datatype getDatatype() {
        return datatype;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public String[] getAllowedValues() {
        // TODO: UPNP VIOLATION: DirecTV HR23/700 High Definition DVR Receiver has invalid default value
        if (!foundDefaultInAllowedValues(defaultValue, allowedValues)) {
            List<String> list = new ArrayList<>(Arrays.asList(allowedValues));
            list.add(getDefaultValue());
            return list.toArray(new String[list.size()]);
        }
        return allowedValues;
    }

    public StateVariableAllowedValueRange getAllowedValueRange() {
        return allowedValueRange;
    }

    protected boolean foundDefaultInAllowedValues(String defaultValue, String[] allowedValues) {
        if (defaultValue == null || allowedValues == null) {
            return true;
        }
        for (String s : allowedValues) {
            if (s.equals(defaultValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (getDatatype() == null) {
            errors.add(new ValidationError(getClass(), "datatype", "Service state variable has no datatype"));
        }

        if (getAllowedValues() != null) {

            if (getAllowedValueRange() != null) {
                errors.add(new ValidationError(getClass(), "allowedValues",
                        "Allowed value list of state variable can not also be restricted with allowed value range"));
            }

            if (!Datatype.Builtin.STRING.equals(getDatatype().getBuiltin())) {
                errors.add(new ValidationError(getClass(), "allowedValues",
                        "Allowed value list of state variable only available for string datatype, not: "
                                + getDatatype()));
            }

            for (String s : getAllowedValues()) {
                if (s.length() > 31) {
                    SpecificationViolationReporter.report("Allowed value string must be less than 32 chars: {}", s);
                }
            }

            if (!foundDefaultInAllowedValues(defaultValue, allowedValues)) {
                SpecificationViolationReporter.report("Allowed string values don't contain default value: {}",
                        defaultValue);
            }
        }

        if (getAllowedValueRange() != null) {
            errors.addAll(getAllowedValueRange().validate());
        }

        return errors;
    }
}
