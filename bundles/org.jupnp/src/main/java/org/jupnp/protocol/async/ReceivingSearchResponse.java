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

import java.util.concurrent.Executor;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.ValidationError;
import org.jupnp.model.ValidationException;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.discovery.IncomingSearchResponse;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.types.UDN;
import org.jupnp.protocol.ReceivingAsync;
import org.jupnp.protocol.RetrieveRemoteDescriptors;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles reception of search response messages.
 * <p>
 * This protocol implementation is basically the same as
 * the {@link org.jupnp.protocol.async.ReceivingNotification} protocol for
 * an <em>ALIVE</em> message.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingSearchResponse extends ReceivingAsync<IncomingSearchResponse> {

    private final Logger logger = LoggerFactory.getLogger(ReceivingSearchResponse.class);

    public ReceivingSearchResponse(UpnpService upnpService, IncomingDatagramMessage<UpnpResponse> inputMessage) {
        super(upnpService, new IncomingSearchResponse(inputMessage));
    }

    @Override
    protected void execute() throws RouterException {

        if (!getInputMessage().isSearchResponseMessage()) {
            logger.trace("Ignoring invalid search response message: {}", getInputMessage());
            return;
        }

        UDN udn = getInputMessage().getRootDeviceUDN();
        if (udn == null) {
            logger.trace("Ignoring search response message without UDN: {}", getInputMessage());
            return;
        }

        RemoteDeviceIdentity rdIdentity = new RemoteDeviceIdentity(getInputMessage());
        logger.trace("Received device search response: {}", rdIdentity);

        if (getUpnpService().getRegistry().update(rdIdentity)) {
            logger.trace("Remote device was already known: {}", udn);
            return;
        }

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

        if (rdIdentity.getDescriptorURL() == null) {
            logger.trace("Ignoring message without location URL header: {}", getInputMessage());
            return;
        }

        if (rdIdentity.getMaxAgeSeconds() == null) {
            logger.trace("Ignoring message without max-age header: {}", getInputMessage());
            return;
        }

        // Unfortunately, we always have to retrieve the descriptor because at this point we
        // have no idea if it's a root or embedded device

        if (RetrieveRemoteDescriptors.isRetrievalInProgress(rd)) {
            logger.trace("Skip submitting task, active retrieval for URL already in progress: {}",
                    rd.getIdentity().getDescriptorURL());
            return;
        }

        UpnpServiceConfiguration conf = getUpnpService().getConfiguration();
        if (conf != null) {
            Executor executor = conf.getAsyncProtocolExecutor();
            if (executor != null) {
                executor.execute(new RetrieveRemoteDescriptors(getUpnpService(), rd));
            }
        }
    }
}
