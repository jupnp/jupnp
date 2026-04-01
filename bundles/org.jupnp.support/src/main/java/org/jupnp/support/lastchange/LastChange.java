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

import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.types.UnsignedIntegerFourBytes;

/**
 * Collects all state changes per logical instance.
 * <p>
 * This class is supposed to be used on a UPnP state variable field,
 * on a RenderingControl or AVTransport service. The service then
 * sets evented values whenever its state changes, and periodically
 * (e.g. in a background loop) fires the "LastChange" XML content
 * through its PropertyChangeSupport. (Where the ServiceManager picks
 * it up and sends it to all subscribers.)
 * </p>
 * <p>
 * The event subscriber can use this class to marshall the "LastChange"
 * content, when the event XML is received.
 * </p>
 * <p>
 * This class is thread-safe.
 * </p>
 *
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class LastChange {

    private final Event event;
    private final LastChangeParser parser;
    private String previousValue;

    public LastChange(String s) {
        throw new UnsupportedOperationException("This constructor is only for service binding detection");
    }

    public LastChange(LastChangeParser parser, Event event) {
        this.parser = parser;
        this.event = event;
    }

    public LastChange(LastChangeParser parser) {
        this(parser, new Event());
    }

    public LastChange(LastChangeParser parser, String xml) throws Exception {
        if (xml != null && !xml.isEmpty()) {
            this.event = parser.parse(xml);
        } else {
            this.event = new Event();
        }
        this.parser = parser;
    }

    public synchronized void reset() {
        previousValue = toString();
        event.clear();
    }

    public synchronized void setEventedValue(int instanceID, EventedValue<?>... ev) {
        setEventedValue(new UnsignedIntegerFourBytes(instanceID), ev);
    }

    public synchronized void setEventedValue(UnsignedIntegerFourBytes instanceID, EventedValue<?>... ev) {
        for (EventedValue<?> eventedValue : ev) {
            if (eventedValue != null) {
                event.setEventedValue(instanceID, eventedValue);
            }

        }
    }

    public synchronized UnsignedIntegerFourBytes[] getInstanceIDs() {
        List<UnsignedIntegerFourBytes> list = new ArrayList<>();
        for (InstanceID instanceID : event.getInstanceIDs()) {
            list.add(instanceID.getId());
        }
        return list.toArray(new UnsignedIntegerFourBytes[list.size()]);
    }

    synchronized EventedValue<?>[] getEventedValues(UnsignedIntegerFourBytes instanceID) {
        InstanceID inst = event.getInstanceID(instanceID);
        return inst != null ? inst.getValues().toArray(new EventedValue[inst.getValues().size()]) : null;
    }

    public synchronized <EV extends EventedValue<?>> EV getEventedValue(int instanceID, Class<EV> type) {
        return getEventedValue(new UnsignedIntegerFourBytes(instanceID), type);
    }

    public synchronized <EV extends EventedValue<?>> EV getEventedValue(UnsignedIntegerFourBytes id, Class<EV> type) {
        return event.getEventedValue(id, type);
    }

    public synchronized void fire(PropertyChangeSupport propertyChangeSupport) {
        String lastChanges = toString();
        if (lastChanges != null && !lastChanges.isEmpty()) {
            propertyChangeSupport.firePropertyChange("LastChange", previousValue, lastChanges);
            reset();
        }
    }

    @Override
    public synchronized String toString() {
        if (!event.hasChanges()) {
            return "";
        }
        try {
            return parser.generate(event);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
