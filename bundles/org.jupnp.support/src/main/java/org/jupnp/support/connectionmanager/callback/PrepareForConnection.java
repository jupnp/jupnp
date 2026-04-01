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
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.support.model.ConnectionInfo;
import org.jupnp.support.model.ProtocolInfo;

/**
 * @author Alessio Gaeta
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class PrepareForConnection extends ActionCallback {

    protected PrepareForConnection(Service<?, ?> service, ProtocolInfo remoteProtocolInfo,
            ServiceReference peerConnectionManager, int peerConnectionID, ConnectionInfo.Direction direction) {
        this(service, null, remoteProtocolInfo, peerConnectionManager, peerConnectionID, direction);
    }

    protected PrepareForConnection(Service<?, ?> service, ControlPoint controlPoint, ProtocolInfo remoteProtocolInfo,
            ServiceReference peerConnectionManager, int peerConnectionID, ConnectionInfo.Direction direction) {
        super(new ActionInvocation<>(service.getAction("PrepareForConnection")), controlPoint);

        getActionInvocation().setInput("RemoteProtocolInfo", remoteProtocolInfo.toString());
        getActionInvocation().setInput("PeerConnectionManager", peerConnectionManager.toString());
        getActionInvocation().setInput("PeerConnectionID", peerConnectionID);
        getActionInvocation().setInput("Direction", direction.toString());
    }

    @Override
    public void success(ActionInvocation invocation) {
        received(invocation, (Integer) invocation.getOutput("ConnectionID").getValue(),
                (Integer) invocation.getOutput("RcsID").getValue(),
                (Integer) invocation.getOutput("AVTransportID").getValue());
    }

    public abstract void received(ActionInvocation<?> invocation, int connectionID, int rcsID, int avTransportID);
}
