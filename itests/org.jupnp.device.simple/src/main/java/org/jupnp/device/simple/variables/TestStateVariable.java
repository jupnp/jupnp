/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
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
 */

package org.jupnp.device.simple.variables;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPService;
import org.jupnp.device.simple.Activator;
import org.jupnp.device.simple.model.TestVariable;

public class TestStateVariable implements Observer, UPnPLocalStateVariable {
	final static String UPNP_EVENT_TOPIC = "org/osgi/service/upnp/UPnPEvent";
	private UPnPDevice device;
	private UPnPService service;
	private String name;
	private Class<?> javaDataType;
	private String dataType;
	private Object defaultValue;
	private boolean sendsEvents;
	private TestVariable variable;

	public TestStateVariable(UPnPDevice device, UPnPService service, Class<?> javaDataType, String dataType, Object defaultValue, boolean sendsEvents, TestVariable variable) {
		this.device = device;
		this.service = service;
		this.name = dataType; //String.format("UPnP%s", dataType);
		this.javaDataType = javaDataType;
		this.dataType = dataType;
		this.defaultValue = defaultValue;
		this.sendsEvents = sendsEvents;
		this.variable = variable;
		
		this.variable.addObserver(this);
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
	public void update(Observable o, Object arg) {
		EventAdmin eventAdmin = Activator.getPlugin().getEventAdmin();
		if (eventAdmin != null) {
			Dictionary<String, Object> values = new Hashtable<String, Object>();
			values.put(getName(), getCurrentValue());
			
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put(UPnPDevice.UDN, device.getDescriptions(null).get(UPnPDevice.UDN));
			properties.put(UPnPService.ID, service.getId());
			properties.put("upnp.events", values);
			
			eventAdmin.sendEvent(new Event(UPNP_EVENT_TOPIC, properties));
		}
	}

}
