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
package org.jupnp.common.util;

public class DataUtil {
    public static boolean compareBytes(byte[] arg1, byte[] arg2) {
        boolean result = true;

        if (arg1.length != arg2.length) {
            result = false;
        } else {
            for (int i = 0; i < arg1.length; i++) {
                if (arg1[i] != arg2[i]) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    public static boolean compareBytes(Byte[] arg1, byte[] arg2) {
        byte[] bytes = new byte[arg1.length];
        for (int i = 0; i < arg1.length; i++) {
            bytes[i] = arg1[i].byteValue();
        }

        return compareBytes(bytes, arg2);
    }
}
