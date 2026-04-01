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
package org.jupnp.model.action;

import org.jupnp.model.meta.Action;
import org.jupnp.model.profile.RemoteClientInfo;

/**
 * An action invocation by a remote control point.
 *
 * @author Christian Bauer
 */
public class RemoteActionInvocation extends ActionInvocation {

    protected final RemoteClientInfo remoteClientInfo;

    public RemoteActionInvocation(Action action, ActionArgumentValue[] input, ActionArgumentValue[] output,
            RemoteClientInfo remoteClientInfo) {
        super(action, input, output, null);
        this.remoteClientInfo = remoteClientInfo;
    }

    public RemoteActionInvocation(Action action, RemoteClientInfo remoteClientInfo) {
        super(action);
        this.remoteClientInfo = remoteClientInfo;
    }

    public RemoteActionInvocation(ActionException failure, RemoteClientInfo remoteClientInfo) {
        super(failure);
        this.remoteClientInfo = remoteClientInfo;
    }

    public RemoteClientInfo getRemoteClientInfo() {
        return remoteClientInfo;
    }
}
