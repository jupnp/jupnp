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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.osgi.impl.UPnPDeviceImpl;
import org.jupnp.osgi.impl.UPnPServiceImpl;
import org.jupnp.osgi.util.OSGiDataConverter;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPEventListener;

/**
 * Adapter between jUPnP GENA subscription and OSGi UPnPEventListener.
 *
 * @author Bruce Green
 */
public class UPnPEventListenerSubscriptionCallback extends SubscriptionCallback {

    private UPnPDeviceImpl device;
    private UPnPServiceImpl service;
    private UPnPEventListener listener;

    protected UPnPEventListenerSubscriptionCallback(UPnPDeviceImpl device, UPnPServiceImpl service,
            UPnPEventListener listener) {
        super(service.getService());

        this.device = device;
        this.service = service;
        this.listener = listener;
    }

    private String getDeviceId() {
        return (String) device.getDescriptions(null).get(UPnPDevice.UDN);
    }

    private String getServiceId() {
        return service.getId();
    }

    @Override
    protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
            String defaultMsg) {
        logger.error("Failed to establish subscription for device {} service {}.", getDeviceId(), getServiceId());

        if (responseStatus != null) {
            logger.error("Response status code: {}", responseStatus.getStatusCode());
            logger.error("Response status message: {}", responseStatus.getStatusMessage());
            logger.error("Response details: {}", responseStatus.getResponseDetails());
        }
        if (exception != null) {
            logger.error("Exception: {}", exception.getMessage());
        }
        logger.error("Default message: {}", defaultMsg);
    }

    @Override
    protected void established(GENASubscription subscription) {
        logger.trace("Established subscription {} for device {} service {}.", subscription.getSubscriptionId(),
                getDeviceId(), getServiceId());
    }

    @Override
    protected void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus) {
        if (reason == null) {
            logger.trace("Subscription {} for device {} service {} ended.", subscription.getSubscriptionId(),
                    getDeviceId(), getServiceId());
        } else {
            logger.error("Subscription {} for device {} service {} ended with reason {}.",
                    subscription.getSubscriptionId(), getDeviceId(), getServiceId(), reason);
        }
    }

    @Override
    protected void eventReceived(GENASubscription subscription) {
        logger.trace("Subscription {} for device {} service {} received event.", subscription.getSubscriptionId(),
                getDeviceId(), getServiceId());
        Map<String, StateVariableValue> values = subscription.getCurrentValues();
        Dictionary dictionary = new Hashtable();

        for (String key : values.keySet()) {
            StateVariableValue variable = values.get(key);

            Object value = OSGiDataConverter.toOSGiValue(variable.getDatatype(), variable.getValue());

            if (value == null) {
                logger.error("Cannot convert variable {} to OSGi type {}.", variable.getStateVariable().getName(),
                        variable.getDatatype().getDisplayString());
                // TODO: throw an exception
            }

            dictionary.put(variable.getStateVariable().getName(), value);
        }

        listener.notifyUPnPEvent(getDeviceId(), getServiceId(), dictionary);
    }

    @Override
    protected void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
        logger.warn("Subscription {} for device {} service {} missed {} events.", subscription.getSubscriptionId(),
                getDeviceId(), getServiceId(), numberOfMissedEvents);
    }

    @Override
    protected void invalidMessage(RemoteGENASubscription subscription, UnsupportedDataException e) {
        logger.trace("Subscription {} for device {} service {} received invalid XML message causing exception {}.",
                subscription.getSubscriptionId(), getDeviceId(), getServiceId(), e.toString());
    }
}
