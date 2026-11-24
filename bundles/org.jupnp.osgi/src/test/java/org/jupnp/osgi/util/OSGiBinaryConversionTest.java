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
package org.jupnp.osgi.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.jupnp.model.VariableValue;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.InvalidValueException;

/**
 * Tests for {@link OSGiDataConverter}.
 */
class OSGiBinaryConversionTest {

    private static final byte[] HELLO = "hello".getBytes(StandardCharsets.US_ASCII);

    @Test
    void binaryValueFromTheNetworkIsPassedToOSGiDecoded() {
        Datatype<?> base64 = Datatype.Builtin.BIN_BASE64.getDatatype();
        Datatype<?> hex = Datatype.Builtin.BIN_HEX.getDatatype();

        assertArrayEquals(HELLO, (byte[]) OSGiDataConverter.toOSGiValue(base64, base64.valueOf("aGVsbG8=")));
        assertArrayEquals(HELLO, (byte[]) OSGiDataConverter.toOSGiValue(hex, hex.valueOf("68656C6C6F")));
        assertArrayEquals(new byte[0], (byte[]) OSGiDataConverter.toOSGiValue(base64, base64.valueOf("")));
        assertArrayEquals(new byte[0], (byte[]) OSGiDataConverter.toOSGiValue(hex, hex.valueOf("")));
    }

    @Test
    void changingBinaryEventValueDoesNotChangeTheCachedValue() {
        Datatype<?> hex = Datatype.Builtin.BIN_HEX.getDatatype();
        byte[] decoded = (byte[]) hex.valueOf("0123");
        byte[] listenerValue = (byte[]) OSGiDataConverter.toOSGiValue(hex, decoded);
        listenerValue[0] = 0x02;

        assertArrayEquals(new byte[] { 0x01, 0x23 }, (byte[]) OSGiDataConverter.toOSGiValue(hex, decoded));
    }

    @Test
    void malformedHexIsRejectedInsteadOfBecomingAnEmptyValue() {
        Datatype<?> hex = Datatype.Builtin.BIN_HEX.getDatatype();

        assertThrows(InvalidValueException.class, () -> hex.valueOf("GG"));
        assertThrows(InvalidValueException.class, () -> hex.valueOf("1"));
    }

    @Test
    void binaryValueFromOSGiIsAcceptedByTheDatatype() {
        Datatype<?> base64 = Datatype.Builtin.BIN_BASE64.getDatatype();
        Datatype<?> hex = Datatype.Builtin.BIN_HEX.getDatatype();

        assertEquals("aGVsbG8=",
                new VariableValue(base64, OSGiDataConverter.tojUPnPValue("bin.base64", HELLO)).toString());
        assertEquals("68656C6C6F", new VariableValue(hex, OSGiDataConverter.tojUPnPValue("bin.hex", HELLO)).toString());
        assertEquals("",
                new VariableValue(base64, OSGiDataConverter.tojUPnPValue("bin.base64", new byte[0])).toString());
        assertEquals("", new VariableValue(hex, OSGiDataConverter.tojUPnPValue("bin.hex", new byte[0])).toString());
    }
}
