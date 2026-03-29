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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A request message, with a method (GET, POST, NOTIFY, etc).
 *
 * @author Christian Bauer
 */
public class UpnpRequest extends UpnpOperation {

    public enum Method {

        GET("GET"),
        POST("POST"),
        NOTIFY("NOTIFY"),
        MSEARCH("M-SEARCH"),
        SUBSCRIBE("SUBSCRIBE"),
        UNSUBSCRIBE("UNSUBSCRIBE"),
        UNKNOWN("UNKNOWN");

        private static final Map<String, Method> byName = new HashMap<>() {
            {
                for (Method m : Method.values()) {
                    put(m.getHttpName(), m);
                }
            }
        };

        private final String httpName;

        Method(String httpName) {
            this.httpName = httpName;
        }

        public String getHttpName() {
            return httpName;
        }

        public static Method getByHttpName(String httpName) {
            if (httpName == null) {
                return UNKNOWN;
            }
            Method m = byName.get(httpName.toUpperCase(Locale.ENGLISH));
            return m != null ? m : UNKNOWN;
        }
    }

    private Method method;
    private URI uri;

    public UpnpRequest(Method method) {
        this.method = method;
    }

    public UpnpRequest(Method method, URI uri) {
        this.method = method;
        this.uri = uri;
    }

    public UpnpRequest(Method method, URL url) {
        this.method = method;
        try {
            if (url != null) {
                this.uri = url.toURI();
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getHttpMethodName() {
        return method.getHttpName();
    }

    public URI getURI() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return getHttpMethodName() + (getURI() != null ? " " + getURI() : "");
    }
}
