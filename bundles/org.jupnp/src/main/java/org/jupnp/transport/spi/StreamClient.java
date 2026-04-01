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

/**
 * Service for sending TCP (HTTP) stream request messages.
 * 
 * <p>
 * An implementation has to be thread-safe.
 * Its constructor may throw {@link org.jupnp.transport.spi.InitializationException}.
 * </p>
 *
 * @param <C> The type of the service's configuration.
 *
 * @author Christian Bauer
 */
public interface StreamClient<C extends StreamClientConfiguration> {

    /**
     * Sends the given request via TCP (HTTP) and returns the response.
     *
     * <p>
     * This method must implement expiration of timed out requests using the
     * {@link StreamClientConfiguration} settings. When a request expires, a
     * <code>null</code> response will be returned.
     * </p>
     * <p>
     * This method will always try to complete execution without throwing an exception. It will
     * return <code>null</code> if an error occurs, and optionally log any exception messages.
     * </p>
     * <p>
     * The rules for logging are:
     * </p>
     * <ul>
     * <li>If the caller interrupts the calling thread, log at <code>TRACE</code>.</li>
     * <li>If the request expires because the timeout has been reached, log at <code>INFO</code> level.</li>
     * <li>If another error occurs, log at <code>WARNING</code> level</li>
     * </ul>
     * <p>
     * This method <strong>is required</strong> to add a <code>Host</code> HTTP header to the
     * outgoing HTTP request, even if the given
     * {@link org.jupnp.model.message.StreamRequestMessage} does not contain such a header.
     * </p>
     * <p>
     * This method will add the <code>User-Agent</code> HTTP header to the outgoing HTTP request if
     * the given message did not already contain such a header. You can set this default value in your
     * {@link StreamClientConfiguration}.
     * </p>
     *
     * @param message The message to send.
     * @return The response or <code>null</code> if no response has been received or an error occurred.
     * @throws InterruptedException if you interrupt the calling thread.
     */
    StreamResponseMessage sendRequest(StreamRequestMessage message) throws InterruptedException;

    /**
     * Stops the service, closes any connection pools etc.
     */
    void stop();

    /**
     * @return This service's configuration.
     */
    C getConfiguration();
}
