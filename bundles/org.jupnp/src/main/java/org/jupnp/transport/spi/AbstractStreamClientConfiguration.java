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
import java.util.concurrent.TimeUnit;

import org.jupnp.model.ServerClientTokens;

/**
 * @author Christian Bauer
 */
public abstract class AbstractStreamClientConfiguration implements StreamClientConfiguration {

    protected ExecutorService requestExecutorService;
    protected int timeoutSeconds = 10;
    protected int logWarningSeconds = 5;
    protected int retryAfterSeconds = (int) TimeUnit.MINUTES.toSeconds(10);
    protected int retryIterations = 5;

    protected AbstractStreamClientConfiguration(ExecutorService requestExecutorService) {
        this.requestExecutorService = requestExecutorService;
    }

    protected AbstractStreamClientConfiguration(ExecutorService requestExecutorService, int timeoutSeconds) {
        this.requestExecutorService = requestExecutorService;
        this.timeoutSeconds = timeoutSeconds;
    }

    protected AbstractStreamClientConfiguration(ExecutorService requestExecutorService, int timeoutSeconds,
            int logWarningSeconds) {
        this.requestExecutorService = requestExecutorService;
        this.timeoutSeconds = timeoutSeconds;
        this.logWarningSeconds = logWarningSeconds;
    }

    protected AbstractStreamClientConfiguration(ExecutorService requestExecutorService, int timeoutSeconds,
            int logWarningSeconds, int retryAfterSeconds, int retryIterations) {
        this.requestExecutorService = requestExecutorService;
        this.timeoutSeconds = timeoutSeconds;
        this.logWarningSeconds = logWarningSeconds;
        this.retryAfterSeconds = retryAfterSeconds;
        this.retryIterations = retryIterations;
    }

    @Override
    public ExecutorService getRequestExecutorService() {
        return requestExecutorService;
    }

    /**
     * @return Configured value or default of 60 seconds.
     */
    @Override
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * @return Configured value or default of 5 retries.
     */
    @Override
    public int getRetryIterations() {
        return retryIterations;
    }

    /**
     * @return Configured value or default of 5 seconds.
     */
    @Override
    public int getLogWarningSeconds() {
        return logWarningSeconds;
    }

    @Override
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * @return Defaults to string value of {@link org.jupnp.model.ServerClientTokens}.
     */
    @Override
    public String getUserAgentValue(int majorVersion, int minorVersion) {
        return new ServerClientTokens(majorVersion, minorVersion).toString();
    }
}
