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
package org.jupnp.model.message.gena;

import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.UpnpHeader;

/**
 * @author Christian Bauer
 */
public class OutgoingEventResponseMessage extends StreamResponseMessage {

    public OutgoingEventResponseMessage() {
        super(new UpnpResponse(UpnpResponse.Status.OK));
        getHeaders().add(UpnpHeader.Type.CONTENT_TYPE, new ContentTypeHeader());
    }

    public OutgoingEventResponseMessage(UpnpResponse operation) {
        super(operation);
        getHeaders().add(UpnpHeader.Type.CONTENT_TYPE, new ContentTypeHeader());
    }
}
