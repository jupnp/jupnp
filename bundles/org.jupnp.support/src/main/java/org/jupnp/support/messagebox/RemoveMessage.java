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
package org.jupnp.support.messagebox;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.support.messagebox.model.Message;

/**
 * ATTENTION: My Samsung TV does not implement this!
 *
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class RemoveMessage extends ActionCallback {

    protected RemoveMessage(Service<?, ?> service, Message message) {
        this(service, message.getId());
    }

    protected RemoveMessage(Service<?, ?> service, int id) {
        super(new ActionInvocation<>(service.getAction("RemoveMessage")));
        getActionInvocation().setInput("MessageID", id);
    }
}
