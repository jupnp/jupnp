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
package org.jupnp.tool.cli;

import java.util.Arrays;
import java.util.List;

/**
 * @author Jochen Hiller - Initial contribution
 */
public class PrintUtils {

    private static final char[] BYTEARRAY_OF_SPACES = new char[128];
    private static final String STRING_OF_SPACES;

    static {
        Arrays.fill(BYTEARRAY_OF_SPACES, ' ');
        STRING_OF_SPACES = String.valueOf(BYTEARRAY_OF_SPACES);
    }

    public static String printTable(List<String[]> table, int spaceBetweenColumns) {
        // calculate max column size
        int[] maxColumnSizes = new int[table.get(0).length];
        for (int i = 0; i < maxColumnSizes.length; i++) {
            maxColumnSizes[i] = 0;
        }
        for (String[] line : table) {
            for (int i = 0; i < line.length; i++) {
                if (line[i] != null) {
                    maxColumnSizes[i] = Math.max(maxColumnSizes[i], line[i].length());
                }
            }
        }
        // now print
        StringBuilder sb = new StringBuilder();
        for (String[] row : table) {
            for (int columnIndex = 0; columnIndex < row.length; columnIndex++) {
                sb.append(row[columnIndex]);
                // add required number of spaces
                sb.append(STRING_OF_SPACES, 0,
                        maxColumnSizes[columnIndex] - row[columnIndex].length() + spaceBetweenColumns);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
