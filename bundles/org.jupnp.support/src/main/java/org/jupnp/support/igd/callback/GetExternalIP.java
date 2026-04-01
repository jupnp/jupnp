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
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class GetExternalIP extends ActionCallback {

    protected GetExternalIP(Service<?, ?> service) {
        super(new ActionInvocation<>(service.getAction("GetExternalIPAddress")));
    }

    @Override
    public void success(ActionInvocation invocation) {
        success((String) invocation.getOutput("NewExternalIPAddress").getValue());
    }

    protected abstract void success(String externalIPAddress);
}
