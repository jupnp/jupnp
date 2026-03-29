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
public class EventedValueChannelVolume extends EventedValue<ChannelVolume> {

    public EventedValueChannelVolume(ChannelVolume value) {
        super(value);
    }

    public EventedValueChannelVolume(Map.Entry<String, String>[] attributes) {
        super(attributes);
    }

    @Override
    protected ChannelVolume valueOf(Map.Entry<String, String>[] attributes) {
        Channel channel = null;
        Integer volume = null;
        for (Map.Entry<String, String> attribute : attributes) {
            if (attribute.getKey().equals("channel")) {
                channel = Channel.valueOf(attribute.getValue());
            }
            if (attribute.getKey().equals("val")) {
                volume = new UnsignedIntegerTwoBytesDatatype().valueOf(attribute.getValue()).getValue().intValue();
            }
        }
        return channel != null && volume != null ? new ChannelVolume(channel, volume) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map.Entry<String, String>[] getAttributes() {
        return new Map.Entry[] {
                new AbstractMap.SimpleEntry<>("val",
                        new UnsignedIntegerTwoBytesDatatype()
                                .getString(new UnsignedIntegerTwoBytes(getValue().getVolume()))),
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
