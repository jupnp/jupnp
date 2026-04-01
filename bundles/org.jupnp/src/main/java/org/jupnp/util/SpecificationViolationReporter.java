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

    private void _report(String format, Object... arguments) {
        if (enabled) {
            String logFormat = "{}: " + format;
            logger.warn(logFormat, "UPnP specification violation", arguments);
        }
    }

    private void _report(Device<DeviceIdentity, Device, Service> device, String format, Object... arguments) {
        if (enabled) {
            if (device == null) {
                String logFormat = "{}: " + format;
                logger.warn(logFormat, "UPnP specification violation", arguments);
            } else {
                String logFormat = "{} of device '{}': " + format;
                logger.warn(logFormat, "UPnP specification violation", device, arguments);
            }
        }
    }

    public static void disableReporting() {
        INSTANCE._disableReporting();
    }

    public static void enableReporting() {
        INSTANCE._enableReporting();
    }

    public static void report(String format, Object... arguments) {
        INSTANCE._report(format, arguments);
    }

    public static void report(Device<DeviceIdentity, Device, Service> device, String format, Object... arguments) {
        INSTANCE._report(device, format, arguments);
    }
}
