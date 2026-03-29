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

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.support.model.PortMapping;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class PortMappingDelete extends ActionCallback {

    protected final PortMapping portMapping;

    protected PortMappingDelete(Service<?, ?> service, PortMapping portMapping) {
        this(service, null, portMapping);
    }

    protected PortMappingDelete(Service<?, ?> service, ControlPoint controlPoint, PortMapping portMapping) {
        super(new ActionInvocation<>(service.getAction("DeletePortMapping")), controlPoint);

        this.portMapping = portMapping;

        getActionInvocation().setInput("NewExternalPort", portMapping.getExternalPort());
        getActionInvocation().setInput("NewProtocol", portMapping.getProtocol());
        if (portMapping.hasRemoteHost()) {
            getActionInvocation().setInput("NewRemoteHost", portMapping.getRemoteHost());
        }
    }
}
