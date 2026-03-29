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
package org.jupnp.common.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestData {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestData.class);
    private String file;
    private Properties properties;

    public TestData(String file) {
        this.file = file;
    }

    private Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            InputStream in = this.getClass().getResourceAsStream(file);
            if (in == null) {
                LOGGER.error("No test data file {}.", file);
            } else {
                try {
                    properties.load(in);
                    in.close();
                } catch (IOException e) {
                    LOGGER.error("Cannot read test data file {}.", file);
                }
            }
        }

        return properties;
    }

    public String getStringValue(String name) {
        String value;

        value = getProperties().getProperty(name);
        if (value == null) {
            LOGGER.error("No test data for type {}.", name);
        }

        return value;
    }

    public Object getOSGiUPnPValue(String name, String type, Object value) {
        return OSGiUPnPStringConverter.toOSGiUPnPValue(type, getStringValue(name), value);
    }

    public Object getOSGiUPnPValue(String name, String type) {
        return getOSGiUPnPValue(name, type, null);
    }

    public Object getjUPnPUPnPValue(String type, Object value) {
        return OSGiUPnPStringConverter.tojUPnPUPnPValue(type, value);
    }
}
