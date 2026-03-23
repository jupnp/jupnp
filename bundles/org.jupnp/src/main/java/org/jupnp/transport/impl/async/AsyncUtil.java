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
package org.jupnp.transport.impl.async;

import jakarta.servlet.ServletRequest;

/**
 * Utility class containing helper methods for the support of the Servlet 2.4
 * non-async and Servlet 3.0 async implementations.
 * 
 * @author Ivan Iliev
 * 
 */
public class AsyncUtil {

    static {
        boolean servlet3 = false;
        try {
            servlet3 = ServletRequest.class.getMethod("startAsync") != null;
        } catch (Exception e) {
        } finally {
            SERVLET3_SUPPORT = servlet3;
        }
    }

    /**
     * True if the {@link ServletRequest} class has a "startAsync" method,
     * otherwise false.
     */
    public static final boolean SERVLET3_SUPPORT;
}
