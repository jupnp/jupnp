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
package org.jupnp.model.message.control;

import org.jupnp.model.Constants;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.EXTHeader;
import org.jupnp.model.message.header.ServerHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.QueryStateVariableAction;

/**
 * @author Christian Bauer
 */
public class OutgoingActionResponseMessage extends StreamResponseMessage implements ActionResponseMessage {

    private String actionNamespace;

    public OutgoingActionResponseMessage(Action action) {
        this(UpnpResponse.Status.OK, action);
    }

    public OutgoingActionResponseMessage(UpnpResponse.Status status) {
        this(status, null);
    }

    public OutgoingActionResponseMessage(UpnpResponse.Status status, Action action) {
        super(new UpnpResponse(status));

        if (action != null) {
            if (action instanceof QueryStateVariableAction) {
                this.actionNamespace = Constants.NS_UPNP_CONTROL_10;
            } else {
                this.actionNamespace = action.getService().getServiceType().toString();
            }
        }

        addHeaders();
    }

    protected void addHeaders() {
        getHeaders().add(UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8));
        getHeaders().add(UpnpHeader.Type.SERVER, new ServerHeader());
        getHeaders().add(UpnpHeader.Type.EXT, new EXTHeader());
    }

    @Override
    public String getActionNamespace() {
        return actionNamespace;
    }
}
