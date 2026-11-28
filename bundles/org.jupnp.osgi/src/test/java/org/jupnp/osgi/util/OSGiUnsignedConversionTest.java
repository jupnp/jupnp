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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.UnsignedIntegerOneByte;
import org.jupnp.model.types.UnsignedIntegerTwoBytes;

class OSGiUnsignedConversionTest {

    @Test
    void unsignedFourByteIntegerKeepsItsValue() {
        Datatype<?> ui4 = Datatype.Builtin.UI4.getDatatype();

        assertEquals(0L, OSGiDataConverter.toOSGiValue(ui4, new UnsignedIntegerFourBytes(0)));
        assertEquals(2147483647L, OSGiDataConverter.toOSGiValue(ui4, new UnsignedIntegerFourBytes(2147483647L)));
        assertEquals(2147483648L, OSGiDataConverter.toOSGiValue(ui4, new UnsignedIntegerFourBytes(2147483648L)));
        assertEquals(4294967295L, OSGiDataConverter.toOSGiValue(ui4, new UnsignedIntegerFourBytes(4294967295L)));
    }

    @Test
    void smallerUnsignedIntegersAreMappedToInteger() {
        assertEquals(255,
                OSGiDataConverter.toOSGiValue(Datatype.Builtin.UI1.getDatatype(), new UnsignedIntegerOneByte(255)));
        assertEquals(65535,
                OSGiDataConverter.toOSGiValue(Datatype.Builtin.UI2.getDatatype(), new UnsignedIntegerTwoBytes(65535)));
    }
}
