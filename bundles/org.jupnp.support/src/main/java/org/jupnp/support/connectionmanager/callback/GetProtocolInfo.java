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
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.support.model.ProtocolInfos;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class GetProtocolInfo extends ActionCallback {

    protected GetProtocolInfo(Service<?, ?> service) {
        this(service, null);
    }

    protected GetProtocolInfo(Service<?, ?> service, ControlPoint controlPoint) {
        super(new ActionInvocation(service.getAction("GetProtocolInfo")), controlPoint);
    }

    @Override
    public void success(ActionInvocation invocation) {
        try {
            ActionArgumentValue<?> sink = invocation.getOutput("Sink");
            ActionArgumentValue<?> source = invocation.getOutput("Source");

            received(invocation, sink != null ? new ProtocolInfos(sink.toString()) : null,
                    source != null ? new ProtocolInfos(source.toString()) : null);

        } catch (Exception e) {
            invocation.setFailure(
                    new ActionException(ErrorCode.ACTION_FAILED, "Can't parse ProtocolInfo response: " + e, e));
            failure(invocation, null);
        }
    }

    public abstract void received(ActionInvocation<?> actionInvocation, ProtocolInfos sinkProtocolInfos,
            ProtocolInfos sourceProtocolInfos);
}
