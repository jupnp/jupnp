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
package org.jupnp.support.igd.callback;

import java.util.Map;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UnsignedIntegerTwoBytes;
import org.jupnp.support.model.PortMapping;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class PortMappingEntryGet extends ActionCallback {

    protected PortMappingEntryGet(Service<?, ?> service, long index) {
        this(service, null, index);
    }

    protected PortMappingEntryGet(Service<?, ?> service, ControlPoint controlPoint, long index) {
        super(new ActionInvocation<>(service.getAction("GetGenericPortMappingEntry")), controlPoint);

        getActionInvocation().setInput("NewPortMappingIndex", new UnsignedIntegerTwoBytes(index));
    }

    @Override
    public void success(ActionInvocation invocation) {

        Map<String, ActionArgumentValue<Service<?, ?>>> outputMap = invocation.getOutputMap();
        success(new PortMapping(outputMap));
    }

    protected abstract void success(PortMapping portMapping);
}
