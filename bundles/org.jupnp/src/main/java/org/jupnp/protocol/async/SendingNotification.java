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
package org.jupnp.protocol.async;

import java.util.ArrayList;
import java.util.List;

import org.jupnp.UpnpService;
import org.jupnp.model.Location;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.message.discovery.OutgoingNotificationRequest;
import org.jupnp.model.message.discovery.OutgoingNotificationRequestDeviceType;
import org.jupnp.model.message.discovery.OutgoingNotificationRequestRootDevice;
import org.jupnp.model.message.discovery.OutgoingNotificationRequestServiceType;
import org.jupnp.model.message.discovery.OutgoingNotificationRequestUDN;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.model.types.ServiceType;
import org.jupnp.protocol.SendingAsync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sending notification messages for a registered local device.
 * <p>
 * Sends all required (dozens) of messages three times, waits between 0 and 150
 * milliseconds between each bulk sending procedure.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class SendingNotification extends SendingAsync {

    private final Logger logger = LoggerFactory.getLogger(SendingNotification.class);

    private LocalDevice device;

    protected SendingNotification(UpnpService upnpService, LocalDevice device) {
        super(upnpService);
        this.device = device;
    }

    public LocalDevice getDevice() {
        return device;
    }

    @Override
    protected void execute() throws RouterException {

        List<NetworkAddress> activeStreamServers = getUpnpService().getRouter().getActiveStreamServers(null);
        if (activeStreamServers.isEmpty()) {
            logger.trace("Aborting notifications, no active stream servers found (network disabled?)");
            return;
        }

        // Prepare it once, it's the same for each repetition
        List<Location> descriptorLocations = new ArrayList<>();
        for (NetworkAddress activeStreamServer : activeStreamServers) {
            descriptorLocations.add(new Location(activeStreamServer,
                    getUpnpService().getConfiguration().getNamespace().getDescriptorPathString(getDevice())));
        }

        for (int i = 0; i < getBulkRepeat(); i++) {
            try {

                for (Location descriptorLocation : descriptorLocations) {
                    sendMessages(descriptorLocation);
                }

                // UDA 1.0 is silent about this but UDA 1.1 recomments "a few hundred milliseconds"
                logger.trace("Sleeping {} milliseconds", getBulkIntervalMilliseconds());
                Thread.sleep(getBulkIntervalMilliseconds());

            } catch (InterruptedException e) {
                logger.warn("Advertisement thread was interrupted", e);
            }
        }
    }

    protected int getBulkRepeat() {
        return 3; // UDA 1.0 says maximum 3 times for alive messages, let's just do it for all
    }

    protected int getBulkIntervalMilliseconds() {
        return 150;
    }

    public void sendMessages(Location descriptorLocation) throws RouterException {
        logger.trace("Sending root device messages: {}", getDevice());
        List<OutgoingNotificationRequest> rootDeviceMsgs = createDeviceMessages(getDevice(), descriptorLocation);
        for (OutgoingNotificationRequest upnpMessage : rootDeviceMsgs) {
            getUpnpService().getRouter().send(upnpMessage);
        }

        if (getDevice().hasEmbeddedDevices()) {
            for (LocalDevice embeddedDevice : getDevice().findEmbeddedDevices()) {
                logger.trace("Sending embedded device messages: {}", embeddedDevice);
                List<OutgoingNotificationRequest> embeddedDeviceMsgs = createDeviceMessages(embeddedDevice,
                        descriptorLocation);
                for (OutgoingNotificationRequest upnpMessage : embeddedDeviceMsgs) {
                    getUpnpService().getRouter().send(upnpMessage);
                }
            }
        }

        List<OutgoingNotificationRequest> serviceTypeMsgs = createServiceTypeMessages(getDevice(), descriptorLocation);
        if (!serviceTypeMsgs.isEmpty()) {
            logger.trace("Sending service type messages");
            for (OutgoingNotificationRequest upnpMessage : serviceTypeMsgs) {
                getUpnpService().getRouter().send(upnpMessage);
            }
        }
    }

    protected List<OutgoingNotificationRequest> createDeviceMessages(LocalDevice device, Location descriptorLocation) {
        List<OutgoingNotificationRequest> msgs = new ArrayList<>();

        // See the tables in UDA 1.0 section 1.1.2

        if (device.isRoot()) {
            msgs.add(new OutgoingNotificationRequestRootDevice(descriptorLocation, device, getNotificationSubtype()));
        }

        msgs.add(new OutgoingNotificationRequestUDN(descriptorLocation, device, getNotificationSubtype()));
        msgs.add(new OutgoingNotificationRequestDeviceType(descriptorLocation, device, getNotificationSubtype()));

        return msgs;
    }

    protected List<OutgoingNotificationRequest> createServiceTypeMessages(LocalDevice device,
            Location descriptorLocation) {
        List<OutgoingNotificationRequest> msgs = new ArrayList<>();

        for (ServiceType serviceType : device.findServiceTypes()) {
            msgs.add(new OutgoingNotificationRequestServiceType(descriptorLocation, device, getNotificationSubtype(),
                    serviceType));
        }

        return msgs;
    }

    protected abstract NotificationSubtype getNotificationSubtype();
}
