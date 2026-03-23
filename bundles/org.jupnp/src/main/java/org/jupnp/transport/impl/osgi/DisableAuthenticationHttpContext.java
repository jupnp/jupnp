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
package org.jupnp.transport.impl.osgi;

import java.lang.reflect.Proxy;

import org.osgi.service.http.HttpContext;

/**
 * An {@link HttpContext} implementation that disables any security handling for any HTTP servlets registered with it.
 * 
 * @author Ivan Iliev
 * 
 */
public final class DisableAuthenticationHttpContext {

    private DisableAuthenticationHttpContext() {
    }

    public static HttpContext create() {
        return (HttpContext) Proxy.newProxyInstance(HttpContext.class.getClassLoader(),
                new Class<?>[] { HttpContext.class }, (proxy, method, args) -> {
                    if ("handleSecurity".equals(method.getName())) {
                        return true;
                    }
                    return null;
                });
    }
}
