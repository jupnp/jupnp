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
package org.jupnp.binding.staging;

import java.util.List;

import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableAllowedValueRange;
import org.jupnp.model.meta.StateVariableEventDetails;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.types.Datatype;

/**
 * @author Christian Bauer
 */
public class MutableStateVariable {

    public String name;
    public Datatype dataType;
    public String defaultValue;
    public List<String> allowedValues;
    public MutableAllowedValueRange allowedValueRange;
    public StateVariableEventDetails eventDetails;

    public StateVariable build() {
        return new StateVariable(name,
                new StateVariableTypeDetails(dataType, defaultValue,
                        allowedValues == null || allowedValues.isEmpty() ? null
                                : allowedValues.toArray(new String[allowedValues.size()]),
                        allowedValueRange == null ? null
                                : new StateVariableAllowedValueRange(allowedValueRange.minimum,
                                        allowedValueRange.maximum, allowedValueRange.step)),
                eventDetails);
    }
}
