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
package org.jupnp.internal.compat.java.beans;

/**
 * This class is a simple implementation of java.beans.PropertyChangeEvent to be
 * able to run jUPnP on JavaSE Embedded 8 compact 2 profile.
 * 
 * It is functional compatible to java.beans for the needed functionalities of
 * jUPnP, but does NOT inherit from java.util.EventObject and is NOT
 * Serializable.
 * 
 * This class is immutable.
 * 
 * @see java.beans.PropertyChangeEvent
 * 
 * @author Jochen Hiller - Initial contribution
 */
public class PropertyChangeEvent {

    // all event properties we need
    private final Object source;
    private final String propertyName;
    private final Object oldValue;
    private final Object newValue;

    public PropertyChangeEvent(Object source, String propName, Object oldValue, Object newValue) {
        this.source = source;
        this.propertyName = propName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Object getSource() {
        return this.source;
    }

    public String getPropertyName() {
        return this.propertyName;
    }

    public Object getOldValue() {
        return this.oldValue;
    }

    public Object getNewValue() {
        return this.newValue;
    }
}
