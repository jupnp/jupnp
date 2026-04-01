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

/*
 * UPnPEventHandler captures OSGi UPnP events. When handling a
 * event it compares all the registered UPnPEvent listeners
 * against the source of the event. If a listener matches the
 * source it will notify that listener.
 */

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UPnPEventHandler implements EventHandler {
    private final Logger logger = LoggerFactory.getLogger(UPnPEventHandler.class);
    private ServiceTracker tracker;

    public UPnPEventHandler(BundleContext context) {
        String string = String.format("(%s=%s)", Constants.OBJECTCLASS, UPnPEventListener.class.getName());
        try {
            Filter filter = context.createFilter(string);

            tracker = new ServiceTracker(context, filter, null);
            tracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Cannot create UPnPEventListener tracker.");
            logger.error(e.getMessage());
        }
    }

    @Override
    public void handleEvent(Event event) {
        logger.trace("ENTRY {}.{}: {}", this.getClass().getName(), "handleEvent", event);

        ServiceReference[] references = tracker.getServiceReferences();
        if (references != null) {
            for (ServiceReference reference : references) {
                boolean matches = true;
                Filter filter = (Filter) reference.getProperty(UPnPEventListener.UPNP_FILTER);
                if (filter != null) {
                    matches = event.matches(filter);
                }

                if (matches) {
                    UPnPEventListener listener = (UPnPEventListener) tracker.getService(reference);
                    listener.notifyUPnPEvent((String) event.getProperty(UPnPDevice.UDN),
                            (String) event.getProperty(UPnPService.ID), (Dictionary) event.getProperty("upnp.events"));
                }
            }
        }
    }
}
