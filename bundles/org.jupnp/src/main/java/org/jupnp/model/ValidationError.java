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
package org.jupnp.model;

/**
 * Representing an integrity rule validation failure.
 *
 * @author Christian Bauer
 */
public class ValidationError {
    private Class clazz;
    private String propertyName;
    private String message;

    public ValidationError(Class clazz, String message) {
        this.clazz = clazz;
        this.message = message;
    }

    public ValidationError(Class clazz, String propertyName, String message) {
        this.clazz = clazz;
        this.propertyName = propertyName;
        this.message = message;
    }

    public Class getClazz() {
        return clazz;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (Class: " + getClazz().getSimpleName() + ", propertyName: "
                + getPropertyName() + "): " + message;
    }
}
