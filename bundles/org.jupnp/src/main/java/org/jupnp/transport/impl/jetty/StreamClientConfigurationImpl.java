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
package org.jupnp.transport.impl.jetty;

import java.util.concurrent.ExecutorService;

import org.jupnp.transport.spi.AbstractStreamClientConfiguration;

/**
 * Settings for the Jetty 12.x implementation.
 *
 * @author Christian Bauer - initial contribution
 * @author Victor Toni - add option for buffer size
 */
public class StreamClientConfigurationImpl extends AbstractStreamClientConfiguration {

    public StreamClientConfigurationImpl(ExecutorService timeoutExecutorService) {
        super(timeoutExecutorService);
    }

    public StreamClientConfigurationImpl(ExecutorService timeoutExecutorService, int timeoutSeconds) {
        super(timeoutExecutorService, timeoutSeconds);
    }

    public StreamClientConfigurationImpl(ExecutorService timeoutExecutorService, int timeoutSeconds,
            int logWarningSeconds, int retryAfterSeconds, int retryIterations) {
        super(timeoutExecutorService, timeoutSeconds, logWarningSeconds, retryAfterSeconds, retryIterations);
    }

    /**
     * @return By default <code>0</code>.
     */
    public int getRequestRetryCount() {
        return 0;
    }

    /**
     * Note: leaving this to the default value of {@code -1} will let the HTTP client use its defaults.
     *
     * @return By default <code>-1</code>, change to change buffer size)
     */
    public int getSocketBufferSize() {
        return -1;
    }
}
