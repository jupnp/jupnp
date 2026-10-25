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
package org.jupnp.util;

import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class reports violations again UPnP specification. It allows to
 * enable/disable these reports. E.g. for embedded devices it makes sense to
 * disable these checks for performance improvement and to avoid flooding of
 * logs if you have UPnP devices in your network which do not comply to UPnP
 * specifications.
 *
 * @author Jochen Hiller
 * @author Victor Toni - made logger non-static
 */
public class SpecificationViolationReporter {

    private static final SpecificationViolationReporter INSTANCE = new SpecificationViolationReporter();

    /**
     * Defaults to enabled. Is volatile to reflect changes in arbitrary threads immediately.
     */
    private volatile boolean enabled = true;

    private final Logger logger = LoggerFactory.getLogger(SpecificationViolationReporter.class);

    private void _disableReporting() {
        enabled = false;
    }

    private void _enableReporting() {
        enabled = true;
    }

    // The arguments are passed to SLF4J as one flat array; passing the array next to other arguments would log it as a
    // single "[a, b]" argument and leave the remaining placeholders unreplaced.
    private void _report(String format, Object... arguments) {
        if (enabled) {
            logger.warn("UPnP specification violation: " + format, arguments);
        }
    }

    private void _report(Device<DeviceIdentity, Device, Service> device, String format, Object... arguments) {
        if (enabled) {
            if (device == null) {
                _report(format, arguments);
            } else if (logger.isWarnEnabled()) {
                Object[] deviceAndArguments = new Object[arguments.length + 1];
                deviceAndArguments[0] = device;
                System.arraycopy(arguments, 0, deviceAndArguments, 1, arguments.length);
                logger.warn("UPnP specification violation of device '{}': " + format, deviceAndArguments);
            }
        }
    }

    public static void disableReporting() {
        INSTANCE._disableReporting();
    }

    public static void enableReporting() {
        INSTANCE._enableReporting();
    }

    /**
     * Logs a specification violation as a warning, using SLF4J's {@code {}} placeholders in {@code format}. As with
     * SLF4J, a {@link Throwable} as the last argument is logged as the exception with its stack trace and does not
     * fill a placeholder, so pass e.g. its message instead to keep the report on a single line.
     */
    public static void report(String format, Object... arguments) {
        INSTANCE._report(format, arguments);
    }

    /**
     * Same as {@link #report(String, Object...)}, but names the device the violation was found in.
     */
    public static void report(Device<DeviceIdentity, Device, Service> device, String format, Object... arguments) {
        INSTANCE._report(device, format, arguments);
    }
}
