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
import org.jupnp.transport.RouterException;

/**
 * Supertype for all synchronously executing protocols, sending UPnP messages.
 * <p>
 * After instantiation by the {@link ProtocolFactory}, this protocol <code>run()</code>s and
 * calls its {@link #executeSync()} method.
 * </p>
 *
 * @param <IN> The type of request UPnP message send by this protocol.
 * @param <OUT> The type of response UPnP message expected by this protocol.
 *
 * @author Christian Bauer
 */
public abstract class SendingSync<IN extends StreamRequestMessage, OUT extends StreamResponseMessage>
        extends SendingAsync {

    private final IN inputMessage;
    protected OUT outputMessage;

    protected SendingSync(UpnpService upnpService, IN inputMessage) {
        super(upnpService);
        this.inputMessage = inputMessage;
    }

    public IN getInputMessage() {
        return inputMessage;
    }

    public OUT getOutputMessage() {
        return outputMessage;
    }

    @Override
    protected final void execute() throws RouterException {
        outputMessage = executeSync();
    }

    protected abstract OUT executeSync() throws RouterException;

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}
