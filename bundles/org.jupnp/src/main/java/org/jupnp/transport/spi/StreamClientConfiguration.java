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

import java.util.concurrent.ExecutorService;

/**
 * Collection of typically needed configuration settings.
 *
 * @author Christian Bauer
 */
public interface StreamClientConfiguration {

    /**
     * Used to execute the actual HTTP request, the StreamClient waits on the "current" thread for
     * completion or timeout. You probably want to use the same executor service for both, so usually
     * this is {@link org.jupnp.UpnpServiceConfiguration#getSyncProtocolExecutorService()}.
     *
     * @return The <code>ExecutorService</code> to use for actual sending of HTTP requests.
     */
    ExecutorService getRequestExecutorService();

    /**
     * @return The number of seconds to wait for a request to expire, spanning connect and data-reads.
     */
    int getTimeoutSeconds();

    /**
     * @return Configured value or default of 5 retries.
     */
    int getRetryIterations();

    /**
     * @return If the request completion takes longer than this, a warning will be logged (<code>0</code> to disable)
     */
    int getLogWarningSeconds();

    /**
     * @return A request will not be executed again if it has failed in the last X seconds ({@code 0} to disable)
     */
    int getRetryAfterSeconds();

    /**
     * Used for outgoing HTTP requests if no other value was already set on messages.
     *
     * @param majorVersion The UPnP UDA major version.
     * @param minorVersion The UPnP UDA minor version.
     * @return The HTTP user agent value.
     */
    String getUserAgentValue(int majorVersion, int minorVersion);
}
