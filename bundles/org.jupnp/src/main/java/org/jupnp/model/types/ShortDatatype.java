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
package org.jupnp.model.types;

/**
 *
 * @author Christian Bauer
 */
public class ShortDatatype extends AbstractDatatype<Short> {

    @Override
    public boolean isHandlingJavaType(Class type) {
        return type == Short.TYPE || Short.class.isAssignableFrom(type);
    }

    @Override
    public Short valueOf(String s) throws InvalidValueException {
        if (s.isEmpty()) {
            return null;
        }
        try {
            Short value = Short.parseShort(s.trim());
            if (!isValid(value)) {
                throw new InvalidValueException("Not a valid short: " + s);
            }
            return value;
        } catch (NumberFormatException e) {
            throw new InvalidValueException("Can't convert string to number: " + s, e);
        }
    }
}
