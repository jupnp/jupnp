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

import java.util.Map;

import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.UnsignedIntegerTwoBytes;
import org.jupnp.model.types.UnsignedIntegerTwoBytesDatatype;
import org.jupnp.support.lastchange.EventedValue;
import org.jupnp.support.model.Channel;
import org.jupnp.support.shared.AbstractMap;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class EventedValueChannelVolumeDB extends EventedValue<ChannelVolumeDB> {

    public EventedValueChannelVolumeDB(ChannelVolumeDB value) {
        super(value);
    }

    public EventedValueChannelVolumeDB(Map.Entry<String, String>[] attributes) {
        super(attributes);
    }

    @Override
    protected ChannelVolumeDB valueOf(Map.Entry<String, String>[] attributes) {
        Channel channel = null;
        Integer volumeDB = null;
        for (Map.Entry<String, String> attribute : attributes) {
            if (attribute.getKey().equals("channel")) {
                channel = Channel.valueOf(attribute.getValue());
            }
            if (attribute.getKey().equals("val")) {
                volumeDB = new UnsignedIntegerTwoBytesDatatype().valueOf(attribute.getValue()).getValue().intValue();
            }
        }
        return channel != null && volumeDB != null ? new ChannelVolumeDB(channel, volumeDB) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map.Entry<String, String>[] getAttributes() {
        return new Map.Entry[] {
                new AbstractMap.SimpleEntry<>("val",
                        new UnsignedIntegerTwoBytesDatatype()
                                .getString(new UnsignedIntegerTwoBytes(getValue().getVolumeDB()))),
                new AbstractMap.SimpleEntry<>("channel", getValue().getChannel().name()) };
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

    @Override
    protected Datatype<?> getDatatype() {
        return null; // Not needed
    }
}
