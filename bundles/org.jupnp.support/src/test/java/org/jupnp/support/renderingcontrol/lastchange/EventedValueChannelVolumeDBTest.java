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
package org.jupnp.support.renderingcontrol.lastchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.support.lastchange.LastChange;
import org.jupnp.support.model.Channel;

class EventedValueChannelVolumeDBTest {

    @Test
    void negativeGainIsWrittenAndRead() throws Exception {
        // -1 dB, VolumeDB is a signed two byte integer in units of 1/256 dB
        final ChannelVolumeDB value = writeAndRead(-256);

        assertEquals(Channel.Master, value.getChannel());
        assertEquals(-256, value.getVolumeDB());
    }

    @Test
    void limitsOfSignedTwoBytesAreWrittenAndRead() throws Exception {
        assertEquals(-Short.MAX_VALUE, writeAndRead(-Short.MAX_VALUE).getVolumeDB());
        assertEquals(0, writeAndRead(0).getVolumeDB());
        assertEquals(Short.MAX_VALUE, writeAndRead(Short.MAX_VALUE).getVolumeDB());
    }

    @Test
    void rejectsReservedVolumeOnReadAndWrite() {
        assertInstanceOf(InvalidValueException.class,
                assertThrows(RuntimeException.class, () -> fromAttribute("-32768")).getCause());
        assertThrows(InvalidValueException.class,
                () -> new EventedValueChannelVolumeDB(new ChannelVolumeDB(Channel.Master, (int) Short.MIN_VALUE))
                        .getAttributes());
    }

    @Test
    void rejectsNonNumericVolumeWithoutSchemaValidation() {
        assertInstanceOf(InvalidValueException.class,
                assertThrows(RuntimeException.class, () -> fromAttribute("NOT_IMPLEMENTED")).getCause());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static EventedValueChannelVolumeDB fromAttribute(String value) {
        return new EventedValueChannelVolumeDB(
                new Map.Entry[] { new AbstractMap.SimpleImmutableEntry<>("channel", "Master"),
                        new AbstractMap.SimpleImmutableEntry<>("val", value) });
    }

    private ChannelVolumeDB writeAndRead(final int volumeDB) throws Exception {
        final LastChange written = new LastChange(new RenderingControlLastChangeParser());
        written.setEventedValue(0,
                new RenderingControlVariable.VolumeDB(new ChannelVolumeDB(Channel.Master, volumeDB)));
        final String xml = written.toString();
        assertTrue(xml.contains("val=\"" + volumeDB + "\""), xml);

        final LastChange read = new LastChange(new RenderingControlLastChangeParser(), xml);
        return read.getEventedValue(0, RenderingControlVariable.VolumeDB.class).getValue();
    }
}
