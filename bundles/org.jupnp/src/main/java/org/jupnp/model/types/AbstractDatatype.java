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

import java.lang.reflect.ParameterizedType;

/**
 * @author Christian Bauer
 */
public abstract class AbstractDatatype<V> implements Datatype<V> {

    private Builtin builtin;

    protected Class<V> getValueType() {
        return (Class<V>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    public boolean isHandlingJavaType(Class type) {
        return getValueType().isAssignableFrom(type);
    }

    @Override
    public V valueOf(String s) throws InvalidValueException {
        return null;
    }

    @Override
    public Builtin getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Builtin builtin) {
        this.builtin = builtin;
    }

    @Override
    public String getString(V value) throws InvalidValueException {
        if (value == null) {
            return "";
        }
        if (!isValid(value)) {
            throw new InvalidValueException("Value is not valid: " + value);
        }
        return value.toString();
    }

    @Override
    public boolean isValid(V value) {
        return value == null || getValueType().isAssignableFrom(value.getClass());
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }

    @Override
    public String getDisplayString() {
        if (this instanceof CustomDatatype) {
            return ((CustomDatatype) this).getName();
        } else if (getBuiltin() != null) {
            return getBuiltin().getDescriptorName();
        } else {
            return getValueType().getSimpleName();
        }
    }
}
