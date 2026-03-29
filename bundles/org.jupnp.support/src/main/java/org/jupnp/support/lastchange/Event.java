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
public class Event {

    protected List<InstanceID> instanceIDs = new ArrayList<>();

    public Event() {
    }

    public Event(List<InstanceID> instanceIDs) {
        this.instanceIDs = instanceIDs;
    }

    public List<InstanceID> getInstanceIDs() {
        return instanceIDs;
    }

    public InstanceID getInstanceID(UnsignedIntegerFourBytes id) {
        for (InstanceID instanceID : instanceIDs) {
            if (instanceID.getId().equals(id)) {
                return instanceID;
            }
        }
        return null;
    }

    public void clear() {
        instanceIDs = new ArrayList<>();
    }

    public void setEventedValue(UnsignedIntegerFourBytes id, EventedValue<?> ev) {
        InstanceID instanceID = null;
        for (InstanceID i : getInstanceIDs()) {
            if (i.getId().equals(id)) {
                instanceID = i;
            }
        }
        if (instanceID == null) {
            instanceID = new InstanceID(id);
            getInstanceIDs().add(instanceID);
        }

        instanceID.getValues().removeIf(existingEv -> existingEv.getClass().equals(ev.getClass()));
        instanceID.getValues().add(ev);
    }

    public <EV extends EventedValue<?>> EV getEventedValue(UnsignedIntegerFourBytes id, Class<EV> type) {
        for (InstanceID instanceID : getInstanceIDs()) {
            if (instanceID.getId().equals(id)) {
                for (EventedValue<?> eventedValue : instanceID.getValues()) {
                    if (eventedValue.getClass().equals(type)) {
                        return (EV) eventedValue;
                    }
                }
            }
        }
        return null;
    }

    public boolean hasChanges() {
        for (InstanceID instanceID : instanceIDs) {
            if (!instanceID.getValues().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
