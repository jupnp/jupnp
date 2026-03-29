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

import java.util.Map;
import java.util.Set;

import org.jupnp.model.ValidationException;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;

/**
 * @author Bruce Green
 */
public class UPnPLocalServiceImpl<T> extends LocalService<T> {

    public UPnPLocalServiceImpl(ServiceType serviceType, ServiceId serviceId, Action[] actions,
            StateVariable[] stateVariables) throws ValidationException {
        super(serviceType, serviceId, actions, stateVariables);
    }

    public UPnPLocalServiceImpl(ServiceType serviceType, ServiceId serviceId,
            Map<Action, ActionExecutor> actionExecutors,
            Map<StateVariable, StateVariableAccessor> stateVariableAccessors, Set<Class> stringConvertibleTypes,
            boolean supportsQueryStateVariables) throws ValidationException {
        super(serviceType, serviceId, actionExecutors, stateVariableAccessors, stringConvertibleTypes,
                supportsQueryStateVariables);
    }
}
