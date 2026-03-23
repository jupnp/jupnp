/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.transport.impl.blocking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Faux implementation mimicking Servlet 3.0's AsyncContext. Used to provide blocking execution
 * for Async Servlet 3.0 operations when ran on Servlet 2.4.
 * 
 * @author Ivan Iliev - Initial contribution and API
 * 
 */
public class FauxAsyncContext {

    private static final long TIME_TO_WAIT = 15000;

    private Long timeout;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private volatile boolean isRunning;

    private boolean isCompleted;

    public FauxAsyncContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
        this.timeout = TIME_TO_WAIT;
    }

    /**
     * Sets the current timeout value in miliseconds - only positive numbers are
     * accepted
     * 
     * @param timeout
     */
    public void setTimeout(long timeout) {
        if (timeout > 0) {
            this.timeout = timeout;
        }
    }

    /**
     * Timeout value in miliseconds. If unset the timeout for blocking
     * operations is Long.MAX_VALUE
     * 
     * @return
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Returns current HttpServletResponse
     * 
     * @return
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * Returns current HttpServletRequest
     * 
     * @return
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * Mark this as completed and stop blocking.
     */
    public void complete() {
        synchronized (this) {
            isRunning = false;
            this.notifyAll();
            isCompleted = true;
        }
    }

    /**
     * Returns true if the complete() method was called, false if we have
     * reached a timeout.
     * 
     * @return
     */
    public boolean isCompleted() {
        return isCompleted;
    }

    /**
     * Blocks the current thread for the set timeout, can be stopped by calling
     * the complete() method.
     */
    public void waitForTimeoutOrCompletion() {
        synchronized (this) {
            if (isCompleted) {
                return;
            }

            isRunning = true;
            // if timeout is not set wait forever for completion
            long totalTimeout = timeout != null ? timeout : Long.MAX_VALUE;
            while (isRunning) {

                long timeToWait = Math.min(totalTimeout, TIME_TO_WAIT);
                try {
                    this.wait(timeToWait);
                } catch (InterruptedException e) {
                    // if our wait is interrupted stop waiting.

                    isRunning = false;
                    return;
                }

                totalTimeout -= timeToWait;

                // timeout - stop waiting
                if (totalTimeout <= 0) {

                    isRunning = false;
                    return;
                }
            }
        }
    }
}
