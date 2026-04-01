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
package org.jupnp.osgi.discover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.jupnp.UpnpService;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.osgi.impl.UPnPDeviceImpl;
import org.jupnp.osgi.impl.UPnPServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPEventListener;
import org.osgi.service.upnp.UPnPService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors and handles addition/removal of UPnPEventListeners for a device.
 * <p>
 * A UPnPEventListener can listen to all devices, some devices,
 * some device and some services, etc.
 * </p>
 * <p>
 * When a UPnPEventListener is registered this tracker will
 * determine if the device it represents applies to this
 * listener. If it does this tracker will establish then
 * necessary subscription with the device and relay its
 * events back to the listener via a callback. A listener
 * may have one or more callbacks associated with it.
 * </p>
 *
 * @author Bruce Green
 */
class UPnPEventListenerTracker extends ServiceTracker {

    private final Logger logger = LoggerFactory.getLogger(UPnPEventListenerTracker.class);

    private UpnpService upnpService;
    private UPnPDeviceImpl device;
    private Map<ServiceReference, List<SubscriptionCallback>> listenerCallbacks = new Hashtable<>();

    public UPnPEventListenerTracker(BundleContext context, Filter filter, UpnpService upnpService,
            UPnPDeviceImpl device) {
        super(context, filter, null);
        logger.trace("ENTRY {}.{}: {} {} {} {}", this.getClass().getName(), "<init>", context, filter, upnpService,
                device);
        this.upnpService = upnpService;
        this.device = device;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        logger.trace("ENTRY {}.{}: {}", this.getClass().getName(), "addingService", reference);
        final UPnPEventListener listener = (UPnPEventListener) super.addingService(reference);

        Filter filter = (Filter) reference.getProperty(UPnPEventListener.UPNP_FILTER);
        if (filter != null) {
            List<SubscriptionCallback> callbacks = new ArrayList<>();
            UPnPServiceImpl[] services = (UPnPServiceImpl[]) device.getServices();
            if (services != null) {
                Dictionary descriptions = device.getDescriptions(null);
                boolean all = filter.match(descriptions);

                if (all) {
                    logger.trace("Matched UPnPEvent listener for device {} service: ALL.",
                            device.getDevice().getIdentity().getUdn().toString());
                }

                for (UPnPServiceImpl service : services) {
                    boolean match = all;

                    if (!match) {
                        Dictionary dictionary = new Hashtable();
                        for (Object key : Collections.list(descriptions.keys())) {
                            dictionary.put(key, descriptions.get(key));
                        }
                        dictionary.put(UPnPService.ID, service.getId());
                        dictionary.put(UPnPService.TYPE, service.getType());
                        match = filter.match(dictionary);
                        if (match) {
                            logger.trace("Matched UPnPEvent listener for device {} service: {}.",
                                    device.getDevice().getIdentity().getUdn().toString(), service.getId());
                        }
                    }

                    if (match) {
                        logger.trace("Creating subscription callback for device {} service: {}.",
                                device.getDevice().getIdentity().getUdn().toString(), service.getId());
                        SubscriptionCallback callback = new UPnPEventListenerSubscriptionCallback(device, service,
                                listener);
                        upnpService.getControlPoint().execute(callback);
                        callbacks.add(callback);
                    }
                }
            }

            listenerCallbacks.put(reference, callbacks);
        }

        return listener;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        logger.trace("ENTRY {}.{}: {} {}", this.getClass().getName(), "removedService", reference, service);

        List<SubscriptionCallback> callbacks = listenerCallbacks.get(reference);
        if (callbacks != null) {
            for (SubscriptionCallback callback : callbacks) {
                // TODO: callbacks are executed ... don't know how to remove them
            }
        }

        listenerCallbacks.remove(reference);
        super.removedService(reference, service);
    }
}
