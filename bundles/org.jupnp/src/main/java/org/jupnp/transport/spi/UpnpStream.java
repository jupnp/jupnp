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
package org.jupnp.transport.spi;

import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.protocol.ProtocolCreationException;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.protocol.ReceivingSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A runnable representation of a single HTTP request/response procedure.
 * <p>
 * Instantiated by the {@link StreamServer}, executed by the
 * {@link org.jupnp.transport.Router}. See the pseudo-code example
 * in the documentation of {@link StreamServer}. An implementation's
 * <code>run()</code> method has to call the {@link #process(org.jupnp.model.message.StreamRequestMessage)},
 * {@link #responseSent(org.jupnp.model.message.StreamResponseMessage)} and
 * {@link #responseException(Throwable)} methods.
 * </p>
 * <p>
 * An implementation does not have to be thread-safe.
 * </p>
 * 
 * @author Christian Bauer
 */
public abstract class UpnpStream implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(UpnpStream.class);

    protected final ProtocolFactory protocolFactory;
    protected ReceivingSync syncProtocol;

    protected UpnpStream(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }

    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    /**
     * Selects a UPnP protocol, runs it within the calling thread, returns the response.
     * <p>
     * This method will return <code>null</code> if the UPnP protocol returned <code>null</code>.
     * The HTTP response in this case is always <em>404 NOT FOUND</em>. Any other (HTTP) error
     * condition will be encapsulated in the returned response message and has to be
     * passed to the HTTP client as it is.
     * </p>
     * 
     * @param requestMsg The TCP (HTTP) stream request message.
     * @return The TCP (HTTP) stream response message, or <code>null</code> if a 404 should be send to the client.
     */
    public StreamResponseMessage process(StreamRequestMessage requestMsg) {
        logger.trace("Processing stream request message: {}", requestMsg);

        try {
            // Try to get a protocol implementation that matches the request message
            syncProtocol = getProtocolFactory().createReceivingSync(requestMsg);
        } catch (ProtocolCreationException e) {
            logger.warn("Processing stream request failed", e);
            return new StreamResponseMessage(UpnpResponse.Status.NOT_IMPLEMENTED);
        }

        // Run it
        logger.trace("Running protocol for synchronous message processing: {}", syncProtocol);
        syncProtocol.run();

        // ... then grab the response
        StreamResponseMessage responseMsg = syncProtocol.getOutputMessage();

        if (responseMsg == null) {
            // That's ok, the caller is supposed to handle this properly (e.g. convert it to HTTP 404)
            logger.trace("Protocol did not return any response message");
            return null;
        }
        logger.trace("Protocol returned response: {}", responseMsg);
        return responseMsg;
    }

    /**
     * Must be called by a subclass after the response has been successfully sent to the client.
     *
     * @param responseMessage The response message successfully sent to the client.
     */
    protected void responseSent(StreamResponseMessage responseMessage) {
        if (syncProtocol != null) {
            syncProtocol.responseSent(responseMessage);
        }
    }

    /**
     * Must be called by a subclass if the response was not delivered to the client.
     *
     * @param t The reason why the response wasn't delivered.
     */
    protected void responseException(Throwable t) {
        if (syncProtocol != null) {
            syncProtocol.responseException(t);
        }
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}
