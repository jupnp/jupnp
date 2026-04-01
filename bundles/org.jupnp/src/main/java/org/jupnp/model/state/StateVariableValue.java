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
package org.jupnp.model.state;

import org.jupnp.model.VariableValue;
import org.jupnp.model.meta.Service;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.types.InvalidValueException;

/**
 * Represents the value of a state variable.
 *
 * @author Christian Bauer
 */
public class StateVariableValue<S extends Service> extends VariableValue {

    private StateVariable<S> stateVariable;

    public StateVariableValue(StateVariable<S> stateVariable, Object value) throws InvalidValueException {
        super(stateVariable.getTypeDetails().getDatatype(), value);
        this.stateVariable = stateVariable;
    }

    public StateVariable<S> getStateVariable() {
        return stateVariable;
    }
}
