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

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.types.UnsignedIntegerFourBytes;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class InstanceID {

    protected UnsignedIntegerFourBytes id;
    protected List<EventedValue<?>> values;

    public InstanceID(UnsignedIntegerFourBytes id) {
        this(id, new ArrayList<>());
    }

    public InstanceID(UnsignedIntegerFourBytes id, List<EventedValue<?>> values) {
        this.id = id;
        this.values = values;
    }

    public UnsignedIntegerFourBytes getId() {
        return id;
    }

    public List<EventedValue<?>> getValues() {
        return values;
    }
}
