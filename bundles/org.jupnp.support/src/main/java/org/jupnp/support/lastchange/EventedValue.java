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
package org.jupnp.support.lastchange;

import java.util.Map;

import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.support.shared.AbstractMap;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class EventedValue<V> {

    protected final V value;

    protected EventedValue(V value) {
        this.value = value;
    }

    protected EventedValue(Map.Entry<String, String>[] attributes) {
        try {
            this.value = valueOf(attributes);
        } catch (InvalidValueException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public V getValue() {
        return value;
    }

    public Map.Entry<String, String>[] getAttributes() {
        return new Map.Entry[] { new AbstractMap.SimpleEntry<>("val", toString()) };
    }

    protected V valueOf(Map.Entry<String, String>[] attributes) {
        V v = null;
        for (Map.Entry<String, String> attribute : attributes) {
            if (attribute.getKey().equals("val")) {
                v = valueOf(attribute.getValue());
            }
        }
        return v;
    }

    protected V valueOf(String s) {
        return (V) getDatatype().valueOf(s);
    }

    @Override
    public String toString() {
        return getDatatype().getString(getValue());
    }

    protected abstract Datatype getDatatype();
}
