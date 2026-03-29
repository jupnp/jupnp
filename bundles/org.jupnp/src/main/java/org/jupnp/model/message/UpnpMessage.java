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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A non-streaming message, the interface between the transport layer and the protocols.
 * <p>
 * Defaults to UDA version 1.0 and a string body type. Message content is not streamed,
 * it is always read into memory and transported as a string or bytes message body.
 * </p>
 * <p>
 * Subtypes of this class typically implement the integrity rules for individual UPnP
 * messages, for example, what headers a particular message requires.
 * </p>
 * <p>
 * Messages are not thread-safe.
 * </p>
 * 
 * @author Christian Bauer
 */
public abstract class UpnpMessage<O extends UpnpOperation> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpnpMessage.class);

    public enum BodyType {
        STRING,
        BYTES
    }

    private int udaMajorVersion = 1;
    private int udaMinorVersion = 0;

    private O operation;
    private UpnpHeaders headers = new UpnpHeaders();
    private Object body;
    private BodyType bodyType = BodyType.STRING;

    protected UpnpMessage(UpnpMessage<O> source) {
        this.operation = source.getOperation();
        this.headers = source.getHeaders();
        this.body = source.getBody();
        this.bodyType = source.getBodyType();
        this.udaMajorVersion = source.getUdaMajorVersion();
        this.udaMinorVersion = source.getUdaMinorVersion();
    }

    protected UpnpMessage(O operation) {
        this.operation = operation;
    }

    protected UpnpMessage(O operation, BodyType bodyType, Object body) {
        this.operation = operation;
        this.bodyType = bodyType;
        this.body = body;
    }

    public int getUdaMajorVersion() {
        return udaMajorVersion;
    }

    public void setUdaMajorVersion(int udaMajorVersion) {
        this.udaMajorVersion = udaMajorVersion;
    }

    public int getUdaMinorVersion() {
        return udaMinorVersion;
    }

    public void setUdaMinorVersion(int udaMinorVersion) {
        this.udaMinorVersion = udaMinorVersion;
    }

    public UpnpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(UpnpHeaders headers) {
        this.headers = headers;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(String string) {
        this.bodyType = BodyType.STRING;
        this.body = string;
    }

    public void setBody(BodyType bodyType, Object body) {
        this.bodyType = bodyType;
        this.body = body;
    }

    public void setBodyCharacters(byte[] characterData) {
        setBody(UpnpMessage.BodyType.STRING, new String(characterData, getContentTypeCharset()));
    }

    public boolean hasBody() {
        return getBody() != null;
    }

    public BodyType getBodyType() {
        return bodyType;
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }

    public String getBodyString() {
        try {
            if (!hasBody()) {
                return null;
            }
            if (getBodyType().equals(BodyType.STRING)) {
                String body = (String) getBody();
                if (body.charAt(0) == '\ufeff') { /* utf8 BOM */
                    body = body.substring(1);
                }
                return body;
            } else {
                return new String((byte[]) getBody(), getContentTypeCharset());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] getBodyBytes() {
        try {
            if (!hasBody()) {
                return null;
            }
            if (getBodyType().equals(BodyType.STRING)) {
                return getBodyString().getBytes(getContentTypeCharset());
            } else {
                return (byte[]) getBody();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public O getOperation() {
        return operation;
    }

    public boolean isContentTypeMissingOrText() {
        ContentTypeHeader contentTypeHeader = getContentTypeHeader();
        // This is against the HTTP specification: If there is no content type we MAY assume that
        // the entity body is bytes. However, to support broken UPnP devices which also violate the
        // UPnP spec and do not send any content type at all, we need to assume no content type
        // means a textual entity body is available.
        if (contentTypeHeader == null) {
            return true;
        }
        if (contentTypeHeader.isText()) {
            return true;
        }
        // Only if there was any content-type header and none was text
        return false;
    }

    public ContentTypeHeader getContentTypeHeader() {
        return getHeaders().getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class);
    }

    public boolean isContentTypeText() {
        ContentTypeHeader ct = getContentTypeHeader();
        return ct != null && ct.isText();
    }

    public boolean isContentTypeTextUDA() {
        ContentTypeHeader ct = getContentTypeHeader();
        return ct != null && ct.isUDACompliantXML();
    }

    public Charset getContentTypeCharset() {
        ContentTypeHeader ct = getContentTypeHeader();
        if (ct == null) {
            return StandardCharsets.UTF_8;
        }

        String charsetValue = ct.getValue().getParameters().get("charset");
        if (charsetValue == null) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(charsetValue);
        } catch (UnsupportedCharsetException e) {
            LOGGER.warn("UpnpMessage has unsupported charset '{}' using UTF-8 instead", charsetValue, e);
            return StandardCharsets.UTF_8;
        }
    }

    public boolean hasHostHeader() {
        return getHeaders().getFirstHeader(UpnpHeader.Type.HOST) != null;
    }

    public boolean isBodyNonEmptyString() {
        return hasBody() && getBodyType().equals(UpnpMessage.BodyType.STRING) && !getBodyString().isEmpty();
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") " + getOperation().toString();
    }
}
