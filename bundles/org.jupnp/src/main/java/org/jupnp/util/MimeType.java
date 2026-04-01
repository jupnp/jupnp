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
package org.jupnp.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Christian Bauer
 */
public class MimeType {

    public static final String WILDCARD = "*";

    private String type;
    private String subtype;
    private Map<String, String> parameters;

    public MimeType() {
        this(WILDCARD, WILDCARD);
    }

    public MimeType(String type, String subtype, Map<String, String> parameters) {
        this.type = type == null ? WILDCARD : type;
        this.subtype = subtype == null ? WILDCARD : subtype;
        if (parameters == null) {
            this.parameters = Map.of();
        } else {
            Map<String, String> map = new TreeMap<>(String::compareToIgnoreCase);
            map.putAll(parameters);
            this.parameters = Collections.unmodifiableMap(map);
        }
    }

    public MimeType(String type, String subtype) {
        this(type, subtype, Map.of());
    }

    public String getType() {
        return this.type;
    }

    public boolean isWildcardType() {
        return this.getType().equals(WILDCARD);
    }

    public String getSubtype() {
        return this.subtype;
    }

    public boolean isWildcardSubtype() {
        return this.getSubtype().equals(WILDCARD);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public boolean isCompatible(MimeType other) {
        if (other == null) {
            return false;
        }
        if (type.equals(WILDCARD) || other.type.equals(WILDCARD)) {
            return true;
        } else if (type.equalsIgnoreCase(other.type) && (subtype.equals(WILDCARD) || other.subtype.equals(WILDCARD))) {
            return true;
        } else {
            return this.type.equalsIgnoreCase(other.type) && this.subtype.equalsIgnoreCase(other.subtype);
        }
    }

    public static MimeType valueOf(String stringValue) throws IllegalArgumentException {
        if (stringValue == null) {
            throw new IllegalArgumentException("String value is null");
        }

        String params = null;
        int semicolonIndex = stringValue.indexOf(";");
        if (semicolonIndex > -1) {
            params = stringValue.substring(semicolonIndex + 1).trim();
            stringValue = stringValue.substring(0, semicolonIndex);
        }
        String major = null;
        String subtype = null;
        String[] paths = stringValue.split("/");

        if (paths.length < 2 && stringValue.equals(WILDCARD)) {

            major = WILDCARD;
            subtype = WILDCARD;

        } else if (paths.length == 2) {

            major = paths[0].trim();
            subtype = paths[1].trim();

        } else if (paths.length != 2) {

            throw new IllegalArgumentException("Error parsing string: " + stringValue);
        }

        if (params != null && !params.isEmpty()) {
            HashMap<String, String> map = new HashMap<>();

            int start = 0;

            while (start < params.length()) {
                start = readParamsIntoMap(map, params, start);
            }
            return new MimeType(major, subtype, map);
        } else {
            return new MimeType(major, subtype);
        }
    }

    public static int readParamsIntoMap(Map<String, String> map, String params, int start) {
        boolean quote = false;
        boolean backslash = false;

        int end = getEnd(params, start);
        String name = params.substring(start, end).trim();
        if (end < params.length() && params.charAt(end) == '=') {
            end++;
        }

        StringBuilder buffer = new StringBuilder(params.length() - end);
        int i = end;
        for (; i < params.length(); i++) {
            char c = params.charAt(i);

            switch (c) {
                case '"': {
                    if (backslash) {
                        backslash = false;
                        buffer.append(c);
                    } else {
                        quote = !quote;
                    }
                    break;
                }
                case '\\': {
                    if (backslash) {
                        backslash = false;
                        buffer.append(c);
                    } else {
                        backslash = true;
                    }
                    break;
                }
                case ';': {
                    if (!quote) {
                        String value = buffer.toString().trim();
                        map.put(name, value);
                        return i + 1;
                    } else {
                        buffer.append(c);
                    }
                    break;
                }
                default: {
                    buffer.append(c);
                    break;
                }
            }
        }
        String value = buffer.toString().trim();
        map.put(name, value);
        return i;
    }

    protected static int getEnd(String params, int start) {
        int equals = params.indexOf('=', start);
        int semicolon = params.indexOf(';', start);
        if (equals == -1 && semicolon == -1) {
            return params.length();
        }
        if (equals == -1) {
            return semicolon;
        }
        if (semicolon == -1) {
            return equals;
        }
        return Math.min(equals, semicolon);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MimeType mimeType = (MimeType) o;

        if (!Objects.equals(parameters, mimeType.parameters)) {
            return false;
        }
        if (!subtype.equalsIgnoreCase(mimeType.subtype)) {
            return false;
        }
        if (!type.equalsIgnoreCase(mimeType.type)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.toLowerCase().hashCode();
        result = 31 * result + subtype.toLowerCase().hashCode();
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(toStringNoParameters());
        if (getParameters() != null || !getParameters().isEmpty()) {
            for (String name : getParameters().keySet()) {
                sb.append(";").append(name).append("=\"").append(getParameters().get(name)).append("\"");
            }
        }
        return sb.toString();
    }

    public String toStringNoParameters() {
        return getType() + "/" + getSubtype();
    }
}
