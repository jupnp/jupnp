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
package org.jupnp.model.message;

import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.util.MimeType;

/**
 * A TCP (HTTP) stream response message.
 *
 * @author Christian Bauer
 */
public class StreamResponseMessage extends UpnpMessage<UpnpResponse> {

    public StreamResponseMessage(StreamResponseMessage source) {
        super(source);
    }

    public StreamResponseMessage(UpnpResponse.Status status) {
        super(new UpnpResponse(status));
    }

    public StreamResponseMessage(UpnpResponse operation) {
        super(operation);
    }

    public StreamResponseMessage(UpnpResponse operation, String body) {
        super(operation, BodyType.STRING, body);
    }

    public StreamResponseMessage(String body) {
        super(new UpnpResponse(UpnpResponse.Status.OK), BodyType.STRING, body);
    }

    public StreamResponseMessage(UpnpResponse operation, byte[] body) {
        super(operation, BodyType.BYTES, body);
    }

    public StreamResponseMessage(byte[] body) {
        super(new UpnpResponse(UpnpResponse.Status.OK), BodyType.BYTES, body);
    }

    public StreamResponseMessage(String body, ContentTypeHeader contentType) {
        this(body);
        getHeaders().add(UpnpHeader.Type.CONTENT_TYPE, contentType);
    }

    public StreamResponseMessage(String body, MimeType mimeType) {
        this(body, new ContentTypeHeader(mimeType));
    }

    public StreamResponseMessage(byte[] body, ContentTypeHeader contentType) {
        this(body);
        getHeaders().add(UpnpHeader.Type.CONTENT_TYPE, contentType);
    }

    public StreamResponseMessage(byte[] body, MimeType mimeType) {
        this(body, new ContentTypeHeader(mimeType));
    }
}
