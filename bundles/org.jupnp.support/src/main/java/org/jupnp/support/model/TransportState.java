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

    String value;

    TransportState() {
        this.value = name();
    }

    public String getValue() {
        return value;
    }

    public TransportState setValue(String value) {
        this.value = value;
        return this;
    }

    public static TransportState valueOrCustomOf(String s) {
        try {
            return TransportState.valueOf(s);
        } catch (IllegalArgumentException e) {
            return TransportState.CUSTOM.setValue(s);
        }
    }
}
