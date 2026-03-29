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
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.model.Connection;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class GetStatusInfo extends ActionCallback {

    protected GetStatusInfo(Service<?, ?> service) {
        super(new ActionInvocation<>(service.getAction("GetStatusInfo")));
    }

    @Override
    public void success(ActionInvocation invocation) {

        try {
            Connection.Status status = Connection.Status
                    .valueOf(invocation.getOutput("NewConnectionStatus").getValue().toString());

            Connection.Error lastError = Connection.Error
                    .valueOf(invocation.getOutput("NewLastConnectionError").getValue().toString());

            success(new Connection.StatusInfo(status,
                    (UnsignedIntegerFourBytes) invocation.getOutput("NewUptime").getValue(), lastError));

        } catch (Exception e) {
            invocation.setFailure(
                    new ActionException(ErrorCode.ARGUMENT_VALUE_INVALID, "Invalid status or last error string", e));
            failure(invocation, null);
        }
    }

    protected abstract void success(Connection.StatusInfo statusInfo);
}
