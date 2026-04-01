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
package org.jupnp.support.contentdirectory.callback;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.ErrorCode;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class GetSystemUpdateID extends ActionCallback {

    protected GetSystemUpdateID(Service<?, ?> service) {
        super(new ActionInvocation<>(service.getAction("GetSystemUpdateID")));
    }

    @Override
    public void success(ActionInvocation invocation) {
        boolean ok = true;
        long id = 0;
        try {
            id = Long.valueOf(invocation.getOutput("Id").getValue().toString()); // UnsignedIntegerFourBytes...
        } catch (Exception e) {
            invocation.setFailure(
                    new ActionException(ErrorCode.ACTION_FAILED, "Can't parse GetSystemUpdateID response: " + e, e));
            failure(invocation, null);
            ok = false;
        }
        if (ok) {
            received(invocation, id);
        }
    }

    public abstract void received(ActionInvocation<?> invocation, long systemUpdateID);
}
