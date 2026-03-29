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
package org.jupnp.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs periodically and calls {@link org.jupnp.registry.RegistryImpl#maintain()}.
 *
 * @author Christian Bauer
 */
public class RegistryMaintainer implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(RegistryMaintainer.class);

    private final RegistryImpl registry;
    private final int sleepIntervalMillis;

    private volatile boolean stopped = false;

    public RegistryMaintainer(RegistryImpl registry, int sleepIntervalMillis) {
        this.registry = registry;
        this.sleepIntervalMillis = sleepIntervalMillis;
    }

    public void stop() {
        logger.trace("Setting stopped status on thread");
        stopped = true;
    }

    @Override
    public void run() {
        stopped = false;
        logger.trace("Running registry maintenance loop every milliseconds: {}", sleepIntervalMillis);
        while (!stopped) {

            try {
                registry.maintain();
                Thread.sleep(sleepIntervalMillis);
            } catch (InterruptedException e) {
                stopped = true;
            }

        }
        logger.trace("Stopped status on thread received, ending maintenance loop");
    }
}
