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
package org.jupnp.support.model;

/**
 * @author Christian Bauer
 */
public enum TransportState {

    STOPPED,
    PLAYING,
    TRANSITIONING,
    PAUSED_PLAYBACK,
    PAUSED_RECORDING,
    RECORDING,
    NO_MEDIA_PRESENT,
    CUSTOM;

    /**
     * @return The name of this constant. A constant is shared by all devices and doesn't hold the vendor-defined value
     *         a device reported for {@link #CUSTOM}, see {@link TransportInfo#getCurrentTransportStateValue()}.
     */
    public String getValue() {
        return name();
    }

    /**
     * @deprecated Has no effect, a constant is shared by all devices and must not hold the value one of them reported.
     */
    @Deprecated
    public TransportState setValue(String value) {
        return this;
    }

    public static TransportState valueOrCustomOf(String s) {
        try {
            return TransportState.valueOf(s);
        } catch (IllegalArgumentException e) {
            return TransportState.CUSTOM;
        }
    }
}
