/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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

import java.io.ByteArrayInputStream;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jupnp.http.Headers;
import org.jupnp.model.message.header.UpnpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides UPnP header API in addition to plain multi-map HTTP header access.
 *
 * @author Christian Bauer
 * @author Jochen Hiller - Made logger non-static
 */
public class UpnpHeaders extends Headers {

    private final Logger logger = LoggerFactory.getLogger(UpnpHeaders.class);

    private static final UpnpHeader[] EMPTY_HEADERS = new UpnpHeader[0];

    protected Map<UpnpHeader.Type, List<UpnpHeader>> parsedHeaders;

    public UpnpHeaders() {
    }

    public UpnpHeaders(Map<String, List<String>> headers) {
        super(headers);
    }

    public UpnpHeaders(ByteArrayInputStream inputStream) {
        super(inputStream);
    }

    public UpnpHeaders(boolean normalizeHeaders) {
        super(normalizeHeaders);
    }

    protected void parseHeaders() {
        // This runs as late as possible and only when necessary (getter called and map is dirty)
        parsedHeaders = new EnumMap<>(UpnpHeader.Type.class);
        logger.trace("Parsing all HTTP headers for known UPnP headers: {}", size());
        for (Entry<String, List<String>> entry : entrySet()) {

            if (entry.getKey() == null) {
                continue; // Oh yes, the JDK has 'null' HTTP headers
            }

            UpnpHeader.Type type = UpnpHeader.Type.getByHttpName(entry.getKey());
            if (type == null) {
                logger.trace("Ignoring non-UPNP HTTP header: {}", entry.getKey());
                continue;
            }

            for (String value : entry.getValue()) {
                UpnpHeader upnpHeader = UpnpHeader.newInstance(type, value);
                if (upnpHeader == null || upnpHeader.getValue() == null) {
                    logger.trace(
                            "Ignoring known but irrelevant header (value violates the UDA specification?) '{}': {}",
                            type.getHttpName(), value);
                } else {
                    addParsedValue(type, upnpHeader);
                }
            }
        }
    }

    protected void addParsedValue(UpnpHeader.Type type, UpnpHeader value) {
        logger.trace("Adding parsed header: {}", value);
        List<UpnpHeader> list = parsedHeaders.computeIfAbsent(type, k -> new LinkedList<>());
        list.add(value);
    }

    @Override
    public List<String> put(String key, List<String> values) {
        parsedHeaders = null;
        return super.put(key, values);
    }

    @Override
    public void add(String key, String value) {
        parsedHeaders = null;
        super.add(key, value);
    }

    @Override
    public List<String> remove(Object key) {
        parsedHeaders = null;
        return super.remove(key);
    }

    @Override
    public void clear() {
        parsedHeaders = null;
        super.clear();
    }

    public boolean containsKey(UpnpHeader.Type type) {
        if (parsedHeaders == null) {
            parseHeaders();
        }
        return parsedHeaders.containsKey(type);
    }

    public List<UpnpHeader> get(UpnpHeader.Type type) {
        if (parsedHeaders == null) {
            parseHeaders();
        }
        return parsedHeaders.get(type);
    }

    public void add(UpnpHeader.Type type, UpnpHeader value) {
        super.add(type.getHttpName(), value.getString());
        if (parsedHeaders != null) {
            addParsedValue(type, value);
        }
    }

    public void remove(UpnpHeader.Type type) {
        super.remove(type.getHttpName());
        if (parsedHeaders != null) {
            parsedHeaders.remove(type);
        }
    }

    public UpnpHeader[] getAsArray(UpnpHeader.Type type) {
        if (parsedHeaders == null) {
            parseHeaders();
        }
        List<UpnpHeader> headers = parsedHeaders.get(type);
        if (headers == null || headers.isEmpty())
            return EMPTY_HEADERS;

        // use EMPTY_HEADERS to trigger a new array is created
        return headers.toArray(EMPTY_HEADERS);
    }

    public UpnpHeader getFirstHeader(UpnpHeader.Type type) {
        List<UpnpHeader> headers = get(type);
        if (headers == null || headers.isEmpty())
            return null;

        return headers.get(0);
    }

    public <H extends UpnpHeader> H getFirstHeader(UpnpHeader.Type type, Class<H> subtype) {
        List<UpnpHeader> headers = get(type);
        if (headers == null || headers.isEmpty())
            return null;

        for (UpnpHeader header : headers) {
            if (subtype.isAssignableFrom(header.getClass())) {
                return (H) header;
            }
        }
        return null;
    }

    public String getFirstHeaderString(UpnpHeader.Type type) {
        UpnpHeader header = getFirstHeader(type);
        return header != null ? header.getString() : null;
    }

    public void log() {
        if (logger.isTraceEnabled()) {
            logger.trace("############################ RAW HEADERS ###########################");
            for (Entry<String, List<String>> entry : entrySet()) {
                logger.trace("=== NAME : {}", entry.getKey());
                for (String v : entry.getValue()) {
                    logger.trace("VALUE: {}", v);
                }
            }
            if (parsedHeaders != null && !parsedHeaders.isEmpty()) {
                logger.trace("########################## PARSED HEADERS ##########################");
                for (Map.Entry<UpnpHeader.Type, List<UpnpHeader>> entry : parsedHeaders.entrySet()) {
                    logger.trace("=== TYPE: {}", entry.getKey());
                    for (UpnpHeader upnpHeader : entry.getValue()) {
                        logger.trace("HEADER: {}", upnpHeader);
                    }
                }
            }
            logger.trace("####################################################################");
        }
    }
}
