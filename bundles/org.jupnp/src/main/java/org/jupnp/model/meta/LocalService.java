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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jupnp.model.ModelUtil;
import org.jupnp.model.ServiceManager;
import org.jupnp.model.ValidationException;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;

/**
 * The metadata of a service created on this host, by application code.
 * <p>
 * After instantiation {@link #setManager(org.jupnp.model.ServiceManager)} must
 * be called to bind the service metadata to the service implementation.
 * </p>
 *
 * @author Christian Bauer
 */
public class LocalService<T> extends Service<LocalDevice, LocalService> {

    protected final Map<Action, ActionExecutor> actionExecutors;
    protected final Map<StateVariable, StateVariableAccessor> stateVariableAccessors;
    protected final Set<Class> stringConvertibleTypes;
    protected final boolean supportsQueryStateVariables;

    protected ServiceManager manager;

    public LocalService(ServiceType serviceType, ServiceId serviceId, Action[] actions, StateVariable[] stateVariables)
            throws ValidationException {
        super(serviceType, serviceId, actions, stateVariables);
        this.manager = null;
        this.actionExecutors = new HashMap<>();
        this.stateVariableAccessors = new HashMap<>();
        this.stringConvertibleTypes = new HashSet<>();
        this.supportsQueryStateVariables = true;
    }

    public LocalService(ServiceType serviceType, ServiceId serviceId, Map<Action, ActionExecutor> actionExecutors,
            Map<StateVariable, StateVariableAccessor> stateVariableAccessors, Set<Class> stringConvertibleTypes,
            boolean supportsQueryStateVariables) throws ValidationException {

        super(serviceType, serviceId, actionExecutors.keySet().toArray(new Action[actionExecutors.size()]),
                stateVariableAccessors.keySet().toArray(new StateVariable[stateVariableAccessors.size()]));

        this.supportsQueryStateVariables = supportsQueryStateVariables;
        this.stringConvertibleTypes = stringConvertibleTypes;
        this.stateVariableAccessors = stateVariableAccessors;
        this.actionExecutors = actionExecutors;
    }

    public synchronized void setManager(ServiceManager<T> manager) {
        if (this.manager != null) {
            throw new IllegalStateException("Manager is final");
        }
        this.manager = manager;
    }

    public synchronized ServiceManager<T> getManager() {
        if (manager == null) {
            throw new IllegalStateException("Unmanaged service, no implementation instance available");
        }
        return manager;
    }

    public boolean isSupportsQueryStateVariables() {
        return supportsQueryStateVariables;
    }

    public Set<Class> getStringConvertibleTypes() {
        return stringConvertibleTypes;
    }

    public boolean isStringConvertibleType(Object o) {
        return o != null && isStringConvertibleType(o.getClass());
    }

    public boolean isStringConvertibleType(Class clazz) {
        return ModelUtil.isStringConvertibleType(getStringConvertibleTypes(), clazz);
    }

    public StateVariableAccessor getAccessor(String stateVariableName) {
        StateVariable sv;
        return (sv = getStateVariable(stateVariableName)) != null ? getAccessor(sv) : null;
    }

    public StateVariableAccessor getAccessor(StateVariable stateVariable) {
        return stateVariableAccessors.get(stateVariable);
    }

    public ActionExecutor getExecutor(String actionName) {
        Action action;
        return (action = getAction(actionName)) != null ? getExecutor(action) : null;
    }

    public ActionExecutor getExecutor(Action action) {
        return actionExecutors.get(action);
    }

    @Override
    public Action getQueryStateVariableAction() {
        return getAction(QueryStateVariableAction.ACTION_NAME);
    }

    @Override
    public String toString() {
        return super.toString() + ", Manager: " + manager;
    }
}
