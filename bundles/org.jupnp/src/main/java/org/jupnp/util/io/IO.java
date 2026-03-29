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
package org.jupnp.util.io;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @author Christian Bauer
 */
public class IO {

    public static String readLines(InputStream is) throws IOException {
        if (is == null) {
            throw new IllegalArgumentException("InputStream is null");
        }

        BufferedReader inputReader = new BufferedReader(new InputStreamReader(is));

        StringBuilder input = new StringBuilder();
        String inputLine;
        while ((inputLine = inputReader.readLine()) != null) {
            input.append(inputLine).append(System.lineSeparator());
        }

        return input.length() > 0 ? input.toString() : "";
    }

    /**
     * Read the given InputStream into a byte array. This method should be replaced by
     * java.io.InputStream#readAllBytes when Android 13 (API Level 33) is more widely used.
     *
     * @param inputStream the InputStream to be read
     * @return a byte array containing the data from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];

        // Read data from InputStream in chunks of 1024 bytes
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            // Write the read data into ByteArrayOutputStream
            buffer.write(data, 0, nRead);
        }

        // Return the complete byte array
        return buffer.toByteArray();
    }
}
