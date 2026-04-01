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

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This class is a simple implementation of java.beans.PropertyChangeSupport to
 * be able to run jUPnP on JavaSE Embedded 8 compact 2 profile.
 * 
 * It is functional compatible to java.beans for the needed functionalities of
 * jUPnP, with only a limited set of functions. It will only support to fire
 * old/new value, no filtering per property, event will be fired to ALL
 * registered listeners.
 * 
 * @see java.beans.PropertyChangeSupport
 * 
 * @author Jochen Hiller - Initial contribution
 */
public class PropertyChangeSupport {

    /** Registered listeners for synchronized usage. */
    private CopyOnWriteArrayList<PropertyChangeListener> listeners = new CopyOnWriteArrayList<>();

    /** The source object for property changes, */
    private Object source;

    public PropertyChangeSupport(Object source) {
        this.source = source;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.listeners.add(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Fire an event for property changed. It will be fired when
     * <ul>
     * <li>oldValue == null</li>
     * <li>newValue == null</li>
     * <li>!oldValue.equals(newValue)</li>
     * </ul>
     * The property name will be put to event, but NOT used for filtering to
     * listeners. All listeners will be called synchronously. The order is not
     * guaranteed. As the event is immutable, the same event will be fired to
     * all listeners.
     */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            PropertyChangeEvent event = new PropertyChangeEvent(this.source, propertyName, oldValue, newValue);
            for (PropertyChangeListener listener : listeners) {
                listener.propertyChange(event);
            }

        }
    }
}
