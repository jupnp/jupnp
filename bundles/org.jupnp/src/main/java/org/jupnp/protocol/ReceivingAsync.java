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
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.RouterException;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supertype for all asynchronously executing protocols, handling reception of UPnP messages.
 * <p>
 * After instantiation by the {@link ProtocolFactory}, this protocol <code>run()</code>s and
 * calls its own {@link #waitBeforeExecution()} method. By default, the protocol does not wait
 * before then proceeding with {@link #execute()}.
 * </p>
 *
 * @param <M> The type of UPnP message handled by this protocol.
 *
 * @author Christian Bauer
 */
public abstract class ReceivingAsync<M extends UpnpMessage> implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(UpnpService.class);

    private final UpnpService upnpService;

    private M inputMessage;

    protected ReceivingAsync(UpnpService upnpService, M inputMessage) {
        this.upnpService = upnpService;
        this.inputMessage = inputMessage;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public M getInputMessage() {
        return inputMessage;
    }

    @Override
    public void run() {
        boolean proceed;
        try {
            proceed = waitBeforeExecution();
        } catch (InterruptedException e) {
            logger.info("Protocol wait before execution interrupted (on shutdown?): {}", getClass().getSimpleName());
            proceed = false;
        }

        if (proceed) {
            try {
                execute();
            } catch (Exception e) {
                Throwable cause = Exceptions.unwrap(e);
                if (cause instanceof InterruptedException) {
                    logger.info("Interrupted protocol '{}'", getClass().getSimpleName(), e);
                } else {
                    throw new RuntimeException(
                            "Fatal error while executing protocol '" + getClass().getSimpleName() + "'", e);
                }
            }
        }
    }

    /**
     * Provides an opportunity to pause before executing the protocol.
     *
     * @return <code>true</code> (default) if execution should continue after waiting.
     *
     * @throws InterruptedException If waiting has been interrupted, which also stops execution.
     */
    protected boolean waitBeforeExecution() throws InterruptedException {
        // Don't wait by default
        return true;
    }

    protected abstract void execute() throws RouterException;

    protected <H extends UpnpHeader> H getFirstHeader(UpnpHeader.Type headerType, Class<H> subtype) {
        return getInputMessage().getHeaders().getFirstHeader(headerType, subtype);
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}
