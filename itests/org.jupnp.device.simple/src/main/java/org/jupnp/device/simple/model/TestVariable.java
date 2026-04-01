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
package org.jupnp.device.simple.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestVariable {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestVariable.class);
    private final List<ValueChangeListener<TestVariable, Object>> listeners = new CopyOnWriteArrayList<>();
    private Object value;

    public TestVariable(Object value) {
        setValue(value);
    }

    public void setValue(Object value) {
        LOGGER.trace("ENTRY {}.{}: {}", getClass().getName(), "setValue", value);
        Object oldValue = this.value;
        if (oldValue == null || !oldValue.equals(value)) {
            LOGGER.trace("old: {} new: {}", oldValue, value);
            this.value = value;
            listeners.forEach(listener -> listener.valueChanged(this, oldValue, value));
        }
    }

    public Object getValue() {
        return value;
    }

    public void addListener(ValueChangeListener<TestVariable, Object> listener) {
        listeners.add(listener);
    }

    public void removeListener(ValueChangeListener<TestVariable, Object> listener) {
        listeners.remove(listener);
    }
}
