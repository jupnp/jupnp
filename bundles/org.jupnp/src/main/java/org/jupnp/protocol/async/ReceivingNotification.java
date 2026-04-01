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

import org.jupnp.UpnpService;
import org.jupnp.model.ValidationError;
import org.jupnp.model.ValidationException;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.discovery.IncomingNotificationRequest;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.types.UDN;
import org.jupnp.protocol.ReceivingAsync;
import org.jupnp.protocol.RetrieveRemoteDescriptors;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles reception of notification messages.
 * <p>
 * First, the UDN is created from the received message.
 * </p>
 * <p>
 * If an <em>ALIVE</em> message has been received, a new background process will be started
 * running {@link org.jupnp.protocol.RetrieveRemoteDescriptors}.
 * </p>
 * <p>
 * If a <em>BYEBYE</em> message has been received, the device will be removed from the registry
 * directly.
 * </p>
 * <p>
 * The following was added to the UDA 1.1 spec (in 1.3), clarifying the handling of messages:
 * </p>
 * <p>
 * "If a control point has received at least one 'byebye' message of a root device, embedded device, or
 * service, then the control point can assume that all are no longer available."
 * </p>
 * <p>
 * Of course, they contradict this a little later:
 * </p>
 * <p>
 * "Only when all original advertisements of a root device, embedded device, and services have
 * expired can a control point assume that they are no longer available."
 * </p>
 * <p>
 * This could mean that even if we get 'byeby'e for the root device, we still have to assume that its services
 * are available. That clearly makes no sense at all and I think it's just badly worded and relates to the
 * previous sentence wich says "if you don't get byebye's, rely on the expiration timeout". It does not
 * imply that a service or embedded device lives beyond its root device. It actually reinforces that we are
 * free to ignore anything that happens as long as the root device is not gone with 'byebye' or has expired.
 * In other words: There is no reason at all why SSDP sends dozens of messages for all embedded devices and
 * services. The composite is the root device and the composite defines the lifecycle of all.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingNotification extends ReceivingAsync<IncomingNotificationRequest> {

    private final Logger logger = LoggerFactory.getLogger(ReceivingNotification.class);

    public ReceivingNotification(UpnpService upnpService, IncomingDatagramMessage<UpnpRequest> inputMessage) {
        super(upnpService, new IncomingNotificationRequest(inputMessage));
    }

    @Override
    protected void execute() throws RouterException {

        UDN udn = getInputMessage().getUDN();
        if (udn == null) {
            logger.trace("Ignoring notification message without UDN: {}", getInputMessage());
            return;
        }

        RemoteDeviceIdentity rdIdentity = new RemoteDeviceIdentity(getInputMessage());
        logger.trace("Received device notification: {}", rdIdentity);

        RemoteDevice rd;
        try {
            rd = new RemoteDevice(rdIdentity);
        } catch (ValidationException e) {
            logger.warn("Validation errors of device during discovery: {}", rdIdentity);
            for (ValidationError validationError : e.getErrors()) {
                logger.warn(validationError.toString());
            }
            return;
        }

        if (getInputMessage().isAliveMessage()) {

            logger.trace("Received device ALIVE advertisement, descriptor location is: {}",
                    rdIdentity.getDescriptorURL());

            if (rdIdentity.getDescriptorURL() == null) {
                logger.trace("Ignoring message without location URL header: {}", getInputMessage());
                return;
            }

            if (rdIdentity.getMaxAgeSeconds() == null) {
                logger.trace("Ignoring message without max-age header: {}", getInputMessage());
                return;
            }

            if (getUpnpService().getRegistry().update(rdIdentity)) {
                logger.trace("Remote device was already known: {}", udn);
                return;
            }

            // Unfortunately, we always have to retrieve the descriptor because at this point we
            // have no idea if it's a root or embedded device
            getUpnpService().getConfiguration().getAsyncProtocolExecutor()
                    .execute(new RetrieveRemoteDescriptors(getUpnpService(), rd));

        } else if (getInputMessage().isByeByeMessage()) {

            logger.trace("Received device BYEBYE advertisement");
            boolean removed = getUpnpService().getRegistry().removeDevice(rd);
            if (removed) {
                logger.trace("Removed remote device from registry: {}", rd);
            }

        } else {
            logger.trace("Ignoring unknown notification message: {}", getInputMessage());
        }
    }
}
