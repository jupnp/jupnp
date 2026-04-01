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
/**
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*  limitations under the License.
*/
package org.jupnp.util.io;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * format validation
 * <p/>
 * This class encodes/decodes hexadecimal data
 *
 * @author Jeffrey Rodriguez
 * @version $Id: HexBin.java 125124 2005-01-14 00:23:54Z kkrouse $
 */
public final class HexBin {

    private static final int BASELENGTH = 255;
    private static final int LOOKUPLENGTH = 16;
    private static byte[] hexNumberTable = new byte[BASELENGTH];
    private static byte[] lookUpHexAlphabet = new byte[LOOKUPLENGTH];

    static {
        Arrays.fill(hexNumberTable, (byte) -1);
        for (int i = '9'; i >= '0'; i--) {
            hexNumberTable[i] = (byte) (i - '0');
        }
        for (int i = 'F'; i >= 'A'; i--) {
            hexNumberTable[i] = (byte) (i - 'A' + 10);
        }
        for (int i = 'f'; i >= 'a'; i--) {
            hexNumberTable[i] = (byte) (i - 'a' + 10);
        }

        for (int i = 0; i < 10; i++) {
            lookUpHexAlphabet[i] = (byte) ('0' + i);
        }
        for (int i = 10; i <= 15; i++) {
            lookUpHexAlphabet[i] = (byte) ('A' + i - 10);
        }
    }

    /**
     * byte to be tested if it is Base64 alphabet
     *
     * @param octect
     * @return
     */
    static boolean isHex(byte octect) {
        return (hexNumberTable[octect] != -1);
    }

    /**
     * Converts bytes to a hex string
     */
    public static String bytesToString(byte[] binaryData) {
        if (binaryData == null) {
            return null;
        }
        return new String(encode(binaryData));
    }

    /**
     * Converts bytes to a hex string with separator (e.g. colon)
     */
    public static String bytesToString(byte[] binaryData, String separator) {
        if (binaryData == null) {
            return null;
        }
        String s = new String(encode(binaryData));
        StringBuilder sb = new StringBuilder();
        int i = 1;
        char[] chars = s.toCharArray();
        for (char c : chars) {
            sb.append(c);
            if (i == 2) {
                sb.append(separator);
                i = 1;
            } else {
                i++;
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    /**
     * Converts a hex string to a byte array.
     */
    public static byte[] stringToBytes(String hexEncoded) {
        return decode(hexEncoded.getBytes());
    }

    /**
     * Converts a hex string to a byte array.
     */
    public static byte[] stringToBytes(String hexEncoded, String separator) {
        return decode(hexEncoded.replaceAll(separator, "").getBytes());
    }

    /**
     * array of byte to encode
     *
     * @param binaryData
     * @return return encode binary array
     */
    public static byte[] encode(byte[] binaryData) {
        if (binaryData == null) {
            return null;
        }
        int lengthData = binaryData.length;
        int lengthEncode = lengthData * 2;
        byte[] encodedData = new byte[lengthEncode];
        for (int i = 0; i < lengthData; i++) {
            encodedData[i * 2] = lookUpHexAlphabet[(binaryData[i] >> 4) & 0xf];
            encodedData[i * 2 + 1] = lookUpHexAlphabet[binaryData[i] & 0xf];
        }
        return encodedData;
    }

    public static byte[] decode(byte[] binaryData) {
        if (binaryData == null) {
            return null;
        }
        int lengthData = binaryData.length;
        if (lengthData % 2 != 0) {
            return null;
        }

        int lengthDecode = lengthData / 2;
        byte[] decodedData = new byte[lengthDecode];
        for (int i = 0; i < lengthDecode; i++) {
            if (!isHex(binaryData[i * 2]) || !isHex(binaryData[i * 2 + 1])) {
                return null;
            }
            decodedData[i] = (byte) ((hexNumberTable[binaryData[i * 2]] << 4) | hexNumberTable[binaryData[i * 2 + 1]]);
        }
        return decodedData;
    }

    /**
     * Decodes Hex data into octects
     *
     * @param binaryData String containing Hex data
     * @return string containing decoded data.
     */
    public static String decode(String binaryData) {
        if (binaryData == null) {
            return null;
        }

        byte[] decoded = decode(binaryData.getBytes(StandardCharsets.UTF_8));
        return decoded == null ? null : new String(decoded);
    }

    /**
     * Encodes octects (using utf-8) into Hex data
     *
     * @param binaryData String containing Hex data
     * @return string containing decoded data.
     */
    public static String encode(String binaryData) {
        if (binaryData == null) {
            return null;
        }

        byte[] encoded = encode(binaryData.getBytes(StandardCharsets.UTF_8));
        return encoded == null ? null : new String(encoded);
    }
}
