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
package org.jupnp.protocol;

import org.jupnp.UpnpService;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supertype for all synchronously executing protocols, handling reception of UPnP messages and return a response.
 * <p>
 * After instantiation by the {@link ProtocolFactory}, this protocol <code>run()</code>s and
 * calls its own {@link #waitBeforeExecution()} method. By default, the protocol does not wait
 * before then proceeding with {@link #executeSync()}.
 * </p>
 * <p>
 * The returned response will be available to the client of this protocol. The
 * client will then call either {@link #responseSent(org.jupnp.model.message.StreamResponseMessage)}
 * or {@link #responseException(Throwable)}, depending on whether the response was successfully
 * delivered. The protocol can override these methods to decide if the whole procedure it is
 * implementing was successful or not, including not only creation but also delivery of the response.
 * </p>
 *
 * @param <IN> The type of incoming UPnP message handled by this protocol.
 * @param <OUT> The type of response UPnP message created by this protocol.
 *
 * @author Christian Bauer
 */
public abstract class ReceivingSync<IN extends StreamRequestMessage, OUT extends StreamResponseMessage>
        extends ReceivingAsync<IN> {

    private final Logger logger = LoggerFactory.getLogger(UpnpService.class);

    protected final RemoteClientInfo remoteClientInfo;
    protected OUT outputMessage;

    protected ReceivingSync(UpnpService upnpService, IN inputMessage) {
        super(upnpService, inputMessage);
        this.remoteClientInfo = new RemoteClientInfo(inputMessage);
    }

    public OUT getOutputMessage() {
        return outputMessage;
    }

    @Override
    protected final void execute() throws RouterException {
        outputMessage = executeSync();

        if (outputMessage != null && !getRemoteClientInfo().getExtraResponseHeaders().isEmpty()) {
            logger.trace("Setting extra headers on response message: {}",
                    getRemoteClientInfo().getExtraResponseHeaders().size());
            outputMessage.getHeaders().putAll(getRemoteClientInfo().getExtraResponseHeaders());
        }
    }

    protected abstract OUT executeSync() throws RouterException;

    /**
     * Called by the client of this protocol after the returned response has been successfully delivered.
     * <p>
     * NOOP by default.
     * </p>
     */
    public void responseSent(StreamResponseMessage responseMessage) {
    }

    /**
     * Called by the client of this protocol if the returned response was not delivered.
     * <p>
     * NOOP by default.
     * </p>
     *
     * @param t The reason why the response wasn't delivered.
     */
    public void responseException(Throwable t) {
    }

    public RemoteClientInfo getRemoteClientInfo() {
        return remoteClientInfo;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}
