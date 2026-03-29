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
package org.jupnp.model.message.header;

import org.jupnp.model.types.UnsignedIntegerFourBytes;

/**
 * @author Christian Bauer
 */
public class EventSequenceHeader extends UpnpHeader<UnsignedIntegerFourBytes> {

    public EventSequenceHeader() {
    }

    public EventSequenceHeader(long value) {
        setValue(new UnsignedIntegerFourBytes(value));
    }

    @Override
    public void setString(String s) throws InvalidHeaderException {

        // Cut off leading zeros
        if (!"0".equals(s)) {
            while (s.startsWith("0")) {
                s = s.substring(1);
            }
        }

        try {
            setValue(new UnsignedIntegerFourBytes(s));
        } catch (NumberFormatException e) {
            throw new InvalidHeaderException("Invalid event sequence, " + e.getMessage(), e);
        }
    }

    @Override
    public String getString() {
        return getValue().toString();
    }
}
