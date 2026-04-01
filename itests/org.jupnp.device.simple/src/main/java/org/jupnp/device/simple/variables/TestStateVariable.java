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
package org.jupnp.device.simple.variables;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jupnp.device.simple.Activator;
import org.jupnp.device.simple.model.TestVariable;
import org.jupnp.device.simple.model.ValueChangeListener;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPService;

public class TestStateVariable implements UPnPLocalStateVariable, ValueChangeListener<TestVariable, Object> {
    static final String UPNP_EVENT_TOPIC = "org/osgi/service/upnp/UPnPEvent";
    private UPnPDevice device;
    private UPnPService service;
    private String name;
    private Class<?> javaDataType;
    private String dataType;
    private Object defaultValue;
    private boolean sendsEvents;
    private TestVariable variable;

    public TestStateVariable(UPnPDevice device, UPnPService service, Class<?> javaDataType, String dataType,
            Object defaultValue, boolean sendsEvents, TestVariable variable) {
        this.device = device;
        this.service = service;
        this.name = dataType; // String.format("UPnP%s", dataType);
        this.javaDataType = javaDataType;
        this.dataType = dataType;
        this.defaultValue = defaultValue;
        this.sendsEvents = sendsEvents;
        this.variable = variable;

        this.variable.addListener(this);
    }

    @Override
    public String getName() {
        return name;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Class getJavaDataType() {
        return javaDataType;
    }

    @Override
    public String getUPnPDataType() {
        return dataType;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String[] getAllowedValues() {
        return null;
    }

    @Override
    public Number getMinimum() {
        return null;
    }

    @Override
    public Number getMaximum() {
        return null;
    }

    @Override
    public Number getStep() {
        return null;
    }

    @Override
    public boolean sendsEvents() {
        return sendsEvents;
    }

    @Override
    public Object getCurrentValue() {
        return variable.getValue();
    }

    public void setCurrentValue(Object value) {
        this.variable.setValue(value);
    }

    @Override
    public void valueChanged(TestVariable source, Object oldValue, Object newValue) {
        EventAdmin eventAdmin = Activator.getPlugin().getEventAdmin();
        if (eventAdmin != null) {
            Dictionary<String, Object> values = new Hashtable<>();
            values.put(getName(), getCurrentValue());

            Map<String, Object> properties = new HashMap<>();
            properties.put(UPnPDevice.UDN, device.getDescriptions(null).get(UPnPDevice.UDN));
            properties.put(UPnPService.ID, service.getId());
            properties.put("upnp.events", values);

            eventAdmin.sendEvent(new Event(UPNP_EVENT_TOPIC, properties));
        }
    }
}
