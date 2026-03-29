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

import java.util.Dictionary;
import java.util.Hashtable;

import org.jupnp.UpnpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.upnp.UPnPDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 111.2.1 UPnP Base Driver
 * <p>
 * The functionality of the UPnP service is implemented in a UPnP base driver.
 * This is a bundle that implements the UPnP protocols and handles the interaction
 * with bundles that use the UPnP devices. A UPnP base driver bundle
 * must provide the following functions:
 * </p>
 * <ul>
 * <li>Discover UPnP devices on the network and map each discovered device
 * into an OSGi registered UPnP Device service.</li>
 * <li>Present UPnP marked services that are registered with the OSGi
 * Framework on one or more networks to be used by other computers.
 * </li>
 * </ul>
 * <p>
 * UPnPPresent tracks UPnPDevice services registered for export. When a service
 * is registered/unregistered UPnPPresent will add/remove it with jUPnP.
 * </p>
 * <p>
 * When a service changes a state variable that sends events UPnPPresent will
 * send that change to external listeners.
 * </p>
 *
 * @author Bruce Green
 */
@Component
public class UPnPPresent {

    private final Logger logger = LoggerFactory.getLogger(UPnPPresent.class);

    private static final String UPNP_EVENT_TOPIC = "org/osgi/service/upnp/UPnPEvent";
    private UPnPDeviceTracker deviceTracker;

    @Activate
    public UPnPPresent(BundleContext context, @Reference UpnpService upnpService) {
        /*
         * Track all UPnPDevices registered for export.
         */
        String string = String.format("(&(%s=%s)(%s=%s))", Constants.OBJECTCLASS, UPnPDevice.class.getName(),
                UPnPDevice.UPNP_EXPORT, "*");
        try {
            Filter filter = context.createFilter(string);

            deviceTracker = new UPnPDeviceTracker(context, upnpService, filter);
            deviceTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Cannot create UPnPDevice tracker.");
            logger.error("Cannot export UPnPDevices.");
            logger.error(e.getMessage());
        }

        /*
         * Track OSGi UPnP events. Local devices fire a UPnP event when
         * a state variable that sends an event when changed.
         */
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put(EventConstants.EVENT_TOPIC, UPNP_EVENT_TOPIC);
        context.registerService(EventHandler.class.getName(), new UPnPEventHandler(context), properties);
    }

    @Deactivate
    public void deactivate() {
        if (deviceTracker != null) {
            deviceTracker.close();
        }
    }
}
