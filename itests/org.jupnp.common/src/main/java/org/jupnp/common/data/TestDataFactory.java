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
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestDataFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataFactory.class);
    private static String DEFAULT_FILE = "test-data-factory.properties";
    private static String DEFAULT_KEY = "org.jupnp.osgi.test.data.factory.properties";
    public static TestDataFactory instance;

    private String file = DEFAULT_FILE;
    private String key = DEFAULT_KEY;
    private Properties properties;
    private Map<String, TestData> data;

    public static TestDataFactory getInstance() {
        if (instance == null) {
            instance = new TestDataFactory();
        }

        return instance;
    }

    private Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            InputStream in = this.getClass().getResourceAsStream(DEFAULT_FILE);
            if (in == null) {
                LOGGER.error("No test data factory file {}.", file);
            } else {
                try {
                    properties.load(in);
                    in.close();
                } catch (IOException e) {
                    LOGGER.error("Cannot read test data factory file {}.", file);
                }
            }
        }

        return properties;
    }

    public TestDataFactory() {
        file = System.getProperty(key, file);
    }

    public TestData getTestData(String id) {

        if (data == null) {
            data = new Hashtable<>();
            Properties properties = getProperties();

            for (Object key : properties.keySet()) {
                String value = properties.getProperty((String) key);

                data.put((String) key, new TestData(value));
            }
        }

        return data.get(id);
    }

    public static void main(String[] args) {
        TestData data = TestDataFactory.getInstance().getTestData("initial");

        System.out.printf("data: %s\n", data);
        System.out.printf("int: %s\n", data.getOSGiUPnPValue("int", "float"));
    }
}
