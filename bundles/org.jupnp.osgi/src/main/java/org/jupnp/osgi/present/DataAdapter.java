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
package org.jupnp.osgi.present;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.osgi.util.OSGiContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Green
 * @author Jochen Hiller - Changed to use Compact2 compliant Java Beans
 */
public class DataAdapter implements UPnPEventListener {

    private final Logger logger = LoggerFactory.getLogger(DataAdapter.class);

    private PropertyChangeSupport propertyChangeSupport;

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    public DataAdapter(LocalService<LocalDevice> service) {
        propertyChangeSupport = new PropertyChangeSupport(this);
        LocalDevice device = service.getDevice();
        String string = String.format("(&(%s=%s)(%s=%s))", UPnPDevice.UDN,
                device.getIdentity().getUdn().getIdentifierString(), UPnPService.ID, service.getServiceId());
        logger.trace("filter: {}", string);

        try {
            BundleContext context = OSGiContext.getBundleContext();
            Filter filter = context.createFilter(string);

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put(UPnPEventListener.UPNP_FILTER, filter);
            context.registerService(UPnPEventListener.class.getName(), this, properties);
        } catch (InvalidSyntaxException e) {
            logger.error("Cannot create DataAdapter ({}).", service.getServiceId());
            logger.error(e.getMessage());
        }
    }

    @Override
    public void notifyUPnPEvent(String deviceId, String serviceId, Dictionary events) {
        logger.trace("ENTRY {}.{}: {} {} {}", this.getClass().getName(), "notifyUPnPEvent", deviceId, serviceId,
                events);

        for (String key : (List<String>) Collections.list(events.keys())) {
            Object value = events.get(key);
            propertyChangeSupport.firePropertyChange(key, null, value);
        }
    }
}
