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

import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;

/**
 * @author Christian Bauer
 */
public class IncomingActionResponseMessage extends StreamResponseMessage implements ActionResponseMessage {

    public IncomingActionResponseMessage(StreamResponseMessage source) {
        super(source);
    }

    public IncomingActionResponseMessage(UpnpResponse operation) {
        super(operation);
    }

    @Override
    public String getActionNamespace() {
        return null; // TODO: We _could_ read this in SOAPActionProcessor and set it when we receive a response but why?
    }

    public boolean isFailedNonRecoverable() {
        int statusCode = getOperation().getStatusCode();
        return getOperation().isFailed() && !(statusCode == UpnpResponse.Status.METHOD_NOT_SUPPORTED.getStatusCode()
                || statusCode == UpnpResponse.Status.INTERNAL_SERVER_ERROR.getStatusCode() && hasBody());
    }

    public boolean isFailedRecoverable() {
        return hasBody() && getOperation().getStatusCode() == UpnpResponse.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
}
