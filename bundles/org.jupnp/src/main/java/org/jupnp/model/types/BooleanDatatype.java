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

import java.util.Locale;

/**
 * @author Christian Bauer
 */
public class BooleanDatatype extends AbstractDatatype<Boolean> {

    public BooleanDatatype() {
    }

    @Override
    public boolean isHandlingJavaType(Class type) {
        return type == Boolean.TYPE || Boolean.class.isAssignableFrom(type);
    }

    @Override
    public Boolean valueOf(String s) throws InvalidValueException {
        if (s.isEmpty()) {
            return null;
        }
        if (s.equals("1") || s.toUpperCase(Locale.ENGLISH).equals("YES")
                || s.toUpperCase(Locale.ENGLISH).equals("TRUE")) {
            return true;
        } else if (s.equals("0") || s.toUpperCase(Locale.ENGLISH).equals("NO")
                || s.toUpperCase(Locale.ENGLISH).equals("FALSE")) {
            return false;
        } else {
            throw new InvalidValueException("Invalid boolean value string: " + s);
        }
    }

    @Override
    public String getString(Boolean value) throws InvalidValueException {
        if (value == null) {
            return "";
        }
        return value ? "1" : "0";
    }
}
