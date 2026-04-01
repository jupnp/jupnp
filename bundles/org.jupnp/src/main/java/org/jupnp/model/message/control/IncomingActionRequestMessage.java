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

import org.jupnp.model.action.ActionException;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.header.SoapActionHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.QueryStateVariableAction;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.SoapActionType;

/**
 * @author Christian Bauer
 */
public class IncomingActionRequestMessage extends StreamRequestMessage implements ActionRequestMessage {

    private final Action action;
    private final String actionNamespace;

    public IncomingActionRequestMessage(StreamRequestMessage source, LocalService service) throws ActionException {
        super(source);

        SoapActionHeader soapActionHeader = getHeaders().getFirstHeader(UpnpHeader.Type.SOAPACTION,
                SoapActionHeader.class);
        if (soapActionHeader == null) {
            throw new ActionException(ErrorCode.INVALID_ACTION, "Missing SOAP action header");
        }

        SoapActionType actionType = soapActionHeader.getValue();

        this.action = service.getAction(actionType.getActionName());
        if (this.action == null) {
            throw new ActionException(ErrorCode.INVALID_ACTION,
                    "Service doesn't implement action: " + actionType.getActionName());
        }

        if (!QueryStateVariableAction.ACTION_NAME.equals(actionType.getActionName())) {
            if (!service.getServiceType().implementsVersion(actionType.getServiceType())) {
                throw new ActionException(ErrorCode.INVALID_ACTION,
                        "Service doesn't support the requested service version");
            }
        }

        this.actionNamespace = actionType.getTypeString();
    }

    public Action getAction() {
        return action;
    }

    @Override
    public String getActionNamespace() {
        return actionNamespace;
    }
}
