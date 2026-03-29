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
import java.util.Hashtable;
import java.util.List;

import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.Service;
import org.jupnp.model.meta.StateVariable;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

/**
 * @author Bruce Green
 */
public class UPnPServiceImpl implements UPnPService {

    private Service service;
    private UPnPAction[] actions;
    private Hashtable<String, UPnPAction> actionsIndex;
    private UPnPStateVariable[] variables;
    private Hashtable<String, UPnPStateVariable> variablesIndex;

    public UPnPServiceImpl(Service service) {
        this.service = service;

        if (service.getActions() != null) {
            List<UPnPAction> list = new ArrayList<>();
            actionsIndex = new Hashtable<>();

            for (Action<?> action : service.getActions()) {
                UPnPAction item = new UPnPActionImpl(action);
                list.add(item);
                actionsIndex.put(item.getName(), item);
            }

            actions = list.toArray(new UPnPAction[list.size()]);
        }

        if (service.getStateVariables() != null) {
            List<UPnPStateVariable> list = new ArrayList<>();
            variablesIndex = new Hashtable<>();

            for (StateVariable<?> variable : service.getStateVariables()) {
                UPnPStateVariable item = new UPnPStateVariableImpl(variable);
                list.add(item);
                variablesIndex.put(item.getName(), item);
            }

            variables = list.toArray(new UPnPStateVariable[list.size()]);
        }
    }

    @Override
    public String getId() {
        return service.getServiceId().toString();
    }

    @Override
    public String getType() {
        return service.getServiceType().toString();
    }

    @Override
    public String getVersion() {
        return String.valueOf(service.getServiceType().getVersion());
    }

    @Override
    public UPnPAction getAction(String name) {
        return actionsIndex != null ? actionsIndex.get(name) : null;
    }

    @Override
    public UPnPAction[] getActions() {
        return actions;
    }

    @Override
    public UPnPStateVariable[] getStateVariables() {
        return variables;
    }

    @Override
    public UPnPStateVariable getStateVariable(String name) {
        return variablesIndex != null ? variablesIndex.get(name) : null;
    }

    public Service<?, ?> getService() {
        return service;
    }
}
