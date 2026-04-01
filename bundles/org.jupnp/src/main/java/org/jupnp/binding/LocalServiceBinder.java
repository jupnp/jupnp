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
package org.jupnp.binding;

import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;

/**
 * Reads {@link org.jupnp.model.meta.LocalService} metadata given a Java class.
 *
 * @author Christian Bauer
 */
public interface LocalServiceBinder {

    /**
     * @param clazz The Java class that is the source of the service metadata.
     * @return The produced metadata.
     * @throws LocalServiceBindingException If binding failed.
     */
    LocalService read(Class<?> clazz) throws LocalServiceBindingException;

    /**
     *
     * @param clazz The Java class that is the source of the service metadata.
     * @param id The pre-defined identifier of the service.
     * @param type The pre-defined type of the service.
     * @param supportsQueryStateVariables <code>true</code> if the service should support the
     *            deprecated "query any state variable value" action.
     * @param stringConvertibleTypes A list of Java classes which map directly to string-typed
     *            UPnP state variables.
     * @return The produced metadata.
     * @throws LocalServiceBindingException If binding failed.
     */
    LocalService read(Class<?> clazz, ServiceId id, ServiceType type, boolean supportsQueryStateVariables,
            Class[] stringConvertibleTypes) throws LocalServiceBindingException;
}
