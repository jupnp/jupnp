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
import org.jupnp.transport.RouterException;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Supertype for all synchronously executing protocols, sending UPnP messages.
 * <p>
 * After instantiation by the {@link ProtocolFactory}, this protocol <code>run()</code>s and
 * calls its {@link #execute()} method.
 * </p>
 * <p>
 * A {@link RouterException} during execution will be wrapped in a fatal <code>RuntimeException</code>,
 * unless its cause is an <code>InterruptedException</code>, in which case an INFO message will be logged.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class SendingAsync implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(UpnpService.class);

    private final UpnpService upnpService;

    protected SendingAsync(UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (Exception e) {
            Throwable cause = Exceptions.unwrap(e);
            if (cause instanceof InterruptedException) {
                logger.info("Interrupted protocol", e);
            } else {
                throw new RuntimeException("Fatal error while executing protocol '" + getClass().getSimpleName() + "'",
                        e);
            }
        }
    }

    protected abstract void execute() throws RouterException;

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ")";
    }
}
