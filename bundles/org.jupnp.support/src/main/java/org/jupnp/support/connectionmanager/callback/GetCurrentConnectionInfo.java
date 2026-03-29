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
package org.jupnp.support.connectionmanager.callback;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.model.ServiceReference;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.support.model.ConnectionInfo;
import org.jupnp.support.model.ProtocolInfo;

/**
 * @author Alessio Gaeta - Initial Contribution
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class GetCurrentConnectionInfo extends ActionCallback {

    protected GetCurrentConnectionInfo(Service<?, ?> service, int connectionID) {
        this(service, null, connectionID);
    }

    protected GetCurrentConnectionInfo(Service<?, ?> service, ControlPoint controlPoint, int connectionID) {
        super(new ActionInvocation<>(service.getAction("GetCurrentConnectionInfo")), controlPoint);
        getActionInvocation().setInput("ConnectionID", connectionID);
    }

    @Override
    public void success(ActionInvocation invocation) {

        try {
            ConnectionInfo info = new ConnectionInfo((Integer) invocation.getInput("ConnectionID").getValue(),
                    (Integer) invocation.getOutput("RcsID").getValue(),
                    (Integer) invocation.getOutput("AVTransportID").getValue(),
                    new ProtocolInfo(invocation.getOutput("ProtocolInfo").toString()),
                    new ServiceReference(invocation.getOutput("PeerConnectionManager").toString()),
                    (Integer) invocation.getOutput("PeerConnectionID").getValue(),
                    ConnectionInfo.Direction.valueOf(invocation.getOutput("Direction").toString()),
                    ConnectionInfo.Status.valueOf(invocation.getOutput("Status").toString()));

            received(invocation, info);

        } catch (Exception e) {
            invocation.setFailure(
                    new ActionException(ErrorCode.ACTION_FAILED, "Can't parse ConnectionInfo response: " + e, e));
            failure(invocation, null);
        }
    }

    public abstract void received(ActionInvocation<?> invocation, ConnectionInfo connectionInfo);
}
