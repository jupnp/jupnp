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
package org.jupnp.device.simple.devices;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.osgi.service.upnp.UPnPDevice;

public abstract class BaseUPnPDevice implements UPnPDevice {
    private UPnPDevice parent;
    private List<String> children = new ArrayList<>();

    private Dictionary<String, Object> descriptions = new Hashtable<>();

    protected void setParent(BaseUPnPDevice parent) {
        this.parent = parent;
        if (parent == null) {
            getDescriptions(null).remove(UPnPDevice.PARENT_UDN);
        } else {
            getDescriptions(null).put(UPnPDevice.PARENT_UDN, parent.getDescriptions(null).get(UPnPDevice.UDN));
        }
    }

    public void addChild(BaseUPnPDevice device) {
        device.setParent(this);
        children.add((String) device.getDescriptions(null).get(UPnPDevice.UDN));
        getDescriptions(null).put(UPnPDevice.CHILDREN_UDN, children.toArray(new String[children.size()]));
    }

    public void removeChild(BaseUPnPDevice device) {
        device.setParent(null);
        children.remove((String) device.getDescriptions(null).get(UPnPDevice.UDN));
        getDescriptions(null).put(UPnPDevice.CHILDREN_UDN, children.toArray(new String[children.size()]));
    }

    public void setDescriptions(Dictionary<String, Object> descriptions) {
        this.descriptions = descriptions;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Dictionary getDescriptions(String locale) {
        return descriptions;
    }
}
