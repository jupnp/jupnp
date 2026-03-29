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

import org.jupnp.model.Command;
import org.jupnp.model.ServiceManager;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;

/**
 * Reads the value of a state variable, given an instance that implements the service.
 *
 * TODO: The design of this is not final, not happy with the relationship between ActionExecutor and this.
 *
 * @author Christian Bauer
 */
public abstract class StateVariableAccessor {

    public StateVariableValue read(final StateVariable<LocalService> stateVariable, final Object serviceImpl)
            throws Exception {

        class AccessCommand implements Command {
            Object result;

            @Override
            public void execute(ServiceManager serviceManager) throws Exception {
                result = read(serviceImpl);
                if (stateVariable.getService().isStringConvertibleType(result)) {
                    result = result.toString();
                }
            }
        }

        AccessCommand cmd = new AccessCommand();
        stateVariable.getService().getManager().execute(cmd);
        return new StateVariableValue(stateVariable, cmd.result);
    }

    public abstract Class<?> getReturnType();

    // TODO: Especially this shouldn't be public
    public abstract Object read(Object serviceImpl) throws Exception;

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}
