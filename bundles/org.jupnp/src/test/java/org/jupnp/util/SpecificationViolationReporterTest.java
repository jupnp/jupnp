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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.types.UDN;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests for {@link SpecificationViolationReporter}.
 *
 * @author Holger Friedrich - Initial contribution
 */
class SpecificationViolationReporterTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(SpecificationViolationReporter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
        SpecificationViolationReporter.enableReporting();
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void replacesAllPlaceholders() {
        SpecificationViolationReporter.report("UPC must be 12 digits all-numeric: '{}' for device '{}'", "F85196ADFC44",
                "TELESTAR DIGIBIT Twin_Rev.3");

        assertEquals("UPnP specification violation: UPC must be 12 digits all-numeric: 'F85196ADFC44' for device "
                + "'TELESTAR DIGIBIT Twin_Rev.3'", appender.list.get(0).getFormattedMessage());
    }

    @Test
    void replacesSinglePlaceholderWithoutBrackets() {
        SpecificationViolationReporter.report("Invalid value '{}'", "x");

        assertEquals("UPnP specification violation: Invalid value 'x'", appender.list.get(0).getFormattedMessage());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void replacesAllPlaceholdersWithDevice() throws Exception {
        RemoteDevice device = new RemoteDevice(new RemoteDeviceIdentity(UDN.uniqueSystemIdentifier("test-device"), 1800,
                URI.create("http://localhost:8080/device.xml").toURL(), null, null));

        SpecificationViolationReporter.report((Device) device, "Invalid value '{}' in '{}'", "x", "y");

        assertEquals("UPnP specification violation of device '" + device + "': Invalid value 'x' in 'y'",
                appender.list.get(0).getFormattedMessage());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void namesDeviceWithoutArguments() throws Exception {
        RemoteDevice device = new RemoteDevice(new RemoteDeviceIdentity(UDN.uniqueSystemIdentifier("test-device"), 1800,
                URI.create("http://localhost:8080/device.xml").toURL(), null, null));

        SpecificationViolationReporter.report((Device) device, "Input argument can not have <retval/>");

        assertEquals("UPnP specification violation of device '" + device + "': Input argument can not have <retval/>",
                appender.list.get(0).getFormattedMessage());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void replacesAllPlaceholdersWithoutDevice() {
        SpecificationViolationReporter.report((Device) null, "Invalid value '{}' in '{}'", "x", "y");

        assertEquals("UPnP specification violation: Invalid value 'x' in 'y'",
                appender.list.get(0).getFormattedMessage());
    }
}
