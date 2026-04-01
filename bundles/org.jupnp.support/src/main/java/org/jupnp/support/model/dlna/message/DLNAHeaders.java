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
package org.jupnp.support.model.dlna.message;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.support.model.dlna.message.header.DLNAHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides UPnP header API in addition to plain multi-map HTTP header access.
 *
 * @author Mario Franco
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class DLNAHeaders extends UpnpHeaders {

    private final Logger logger = LoggerFactory.getLogger(DLNAHeaders.class.getName());

    protected Map<DLNAHeader.Type, List<UpnpHeader<?>>> parsedDLNAHeaders;

    public DLNAHeaders() {
    }

    public DLNAHeaders(Map<String, List<String>> headers) {
        super(headers);
    }

    public DLNAHeaders(ByteArrayInputStream inputStream) {
        super(inputStream);
    }

    @Override
    protected void parseHeaders() {
        if (parsedHeaders == null) {
            super.parseHeaders();
        }

        // This runs as late as possible and only when necessary (getter called and map is dirty)
        parsedDLNAHeaders = new LinkedHashMap<>();
        logger.debug("Parsing all HTTP headers for known UPnP headers: {}", size());
        for (Entry<String, List<String>> entry : entrySet()) {
            if (entry.getKey() == null) {
                continue; // Oh yes, the JDK has 'null' HTTP headers
            }

            DLNAHeader.Type type = DLNAHeader.Type.getByHttpName(entry.getKey());
            if (type == null) {
                logger.debug("Ignoring non-UPNP HTTP header: {}", entry.getKey());
                continue;
            }

            for (String value : entry.getValue()) {
                UpnpHeader<?> upnpHeader = DLNAHeader.newInstance(type, value);
                if (upnpHeader == null || upnpHeader.getValue() == null) {
                    logger.debug(
                            "Ignoring known but non-parsable header (value violates the UDA specification?) '{}': {}",
                            type.getHttpName(), value);
                } else {
                    addParsedValue(type, upnpHeader);
                }
            }
        }
    }

    protected void addParsedValue(DLNAHeader.Type type, UpnpHeader<?> value) {
        logger.debug("Adding parsed header: {}", value);
        List<UpnpHeader<?>> list = parsedDLNAHeaders.computeIfAbsent(type, k -> new LinkedList<>());
        list.add(value);
    }

    @Override
    public List<String> put(String key, List<String> values) {
        parsedDLNAHeaders = null;
        return super.put(key, values);
    }

    @Override
    public void add(String key, String value) {
        parsedDLNAHeaders = null;
        super.add(key, value);
    }

    @Override
    public List<String> remove(Object key) {
        parsedDLNAHeaders = null;
        return super.remove(key);
    }

    @Override
    public void clear() {
        parsedDLNAHeaders = null;
        super.clear();
    }

    public boolean containsKey(DLNAHeader.Type type) {
        if (parsedDLNAHeaders == null)
            parseHeaders();
        return parsedDLNAHeaders.containsKey(type);
    }

    public List<UpnpHeader<?>> get(DLNAHeader.Type type) {
        if (parsedDLNAHeaders == null)
            parseHeaders();
        return parsedDLNAHeaders.get(type);
    }

    public void add(DLNAHeader.Type type, UpnpHeader<?> value) {
        super.add(type.getHttpName(), value.getString());
        if (parsedDLNAHeaders != null)
            addParsedValue(type, value);
    }

    public void remove(DLNAHeader.Type type) {
        super.remove(type.getHttpName());
        if (parsedDLNAHeaders != null) {
            parsedDLNAHeaders.remove(type);
        }
    }

    public UpnpHeader<?>[] getAsArray(DLNAHeader.Type type) {
        if (parsedDLNAHeaders == null) {
            parseHeaders();
        }
        return parsedDLNAHeaders.get(type) != null
                ? parsedDLNAHeaders.get(type).toArray(new UpnpHeader[parsedDLNAHeaders.get(type).size()])
                : new UpnpHeader[0];
    }

    public UpnpHeader<?> getFirstHeader(DLNAHeader.Type type) {
        return getAsArray(type).length > 0 ? getAsArray(type)[0] : null;
    }

    @SuppressWarnings("unchecked")
    public <H extends UpnpHeader<?>> H getFirstHeader(DLNAHeader.Type type, Class<H> subtype) {
        UpnpHeader<?>[] headers = getAsArray(type);

        for (UpnpHeader<?> header : headers) {
            if (subtype.isAssignableFrom(header.getClass())) {
                return (H) header;
            }
        }
        return null;
    }

    @Override
    public void log() {
        if (logger.isTraceEnabled()) {
            super.log();
            if (parsedDLNAHeaders != null && !parsedDLNAHeaders.isEmpty()) {
                logger.trace("########################## PARSED DLNA HEADERS ##########################");
                for (Map.Entry<DLNAHeader.Type, List<UpnpHeader<?>>> entry : parsedDLNAHeaders.entrySet()) {
                    logger.trace("=== TYPE: {}", entry.getKey());
                    for (UpnpHeader<?> upnpHeader : entry.getValue()) {
                        logger.trace("HEADER: {}", upnpHeader);
                    }
                }
            }
            logger.trace("####################################################################");
        }
    }
}
