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
package org.jupnp.support.avtransport.callback;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.model.PositionInfo;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class GetPositionInfo extends ActionCallback {

    protected GetPositionInfo(Service<?, ?> service) {
        this(new UnsignedIntegerFourBytes(0), service);
    }

    protected GetPositionInfo(UnsignedIntegerFourBytes instanceId, Service<?, ?> service) {
        super(new ActionInvocation<>(service.getAction("GetPositionInfo")));
        getActionInvocation().setInput("InstanceID", instanceId);
    }

    @Override
    public void success(ActionInvocation invocation) {
        PositionInfo positionInfo = new PositionInfo(invocation.getOutputMap());
        received(invocation, positionInfo);
    }

    public abstract void received(ActionInvocation<?> invocation, PositionInfo positionInfo);
}
