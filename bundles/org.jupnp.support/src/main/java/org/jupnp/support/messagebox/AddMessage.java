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
import org.jupnp.util.MimeType;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class AddMessage extends ActionCallback {

    protected final MimeType mimeType = MimeType.valueOf("text/xml;charset=\"utf-8\"");

    protected AddMessage(Service<?, ?> service, Message message) {
        super(new ActionInvocation<>(service.getAction("AddMessage")));

        getActionInvocation().setInput("MessageID", Integer.toString(message.getId()));
        getActionInvocation().setInput("MessageType", mimeType.toString());
        getActionInvocation().setInput("Message", message.toString());
    }
}
