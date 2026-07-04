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
package org.jupnp.transport.impl.jetty12;

import java.util.List;
import java.util.Map;

import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.Response;
import org.eclipse.jetty.http.HttpField;
import org.jupnp.http.Headers;

/**
 * Converts from/to jUPnP {@link Headers} to/from Jetty 12.x header format.
 *
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - initial contribution
 * @author Holger Friedrich - adapted to Jetty 12
 */
public class HeaderUtil {

    private HeaderUtil() {
        // no instance of this class
    }

    /**
     * Add all jUPnP {@link Headers} header information to a {@link Request}.
     *
     * @param request to enrich with header information
     * @param headers to be added to the {@link Request}
     */
    public static void add(final Request request, final Headers headers) {
        request.headers(fields -> {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                for (final String value : entry.getValue()) {
                    fields.add(entry.getKey(), value);
                }
            }
        });
    }

    /**
     * Get all header information from a {@link Response} as jUPnP {@link Headers}.
     *
     * @param response {@link Response}, must not be null
     * @return {@link Headers}, never {@code null}
     */
    public static Headers get(final Response response) {
        final Headers headers = new Headers();
        for (HttpField httpField : response.getHeaders()) {
            headers.add(httpField.getName(), httpField.getValue());
        }

        return headers;
    }
}
