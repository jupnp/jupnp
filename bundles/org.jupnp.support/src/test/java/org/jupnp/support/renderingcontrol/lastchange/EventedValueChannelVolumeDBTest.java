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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
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
        assertEquals(Short.MIN_VALUE, writeAndRead(Short.MIN_VALUE).getVolumeDB());
        assertEquals(0, writeAndRead(0).getVolumeDB());
        assertEquals(Short.MAX_VALUE, writeAndRead(Short.MAX_VALUE).getVolumeDB());
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
