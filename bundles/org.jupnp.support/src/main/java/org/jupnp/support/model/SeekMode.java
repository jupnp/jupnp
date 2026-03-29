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
 * ui4 (ABS_COUNT, REL_COUNT, TRACK_NR, TAPE-INDEX, FRAME)
 * time (ABS_TIME, REL_TIME)
 * float (CHANNEL_FREQ, in Hz)
 * 
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public enum SeekMode {

    TRACK_NR("TRACK_NR"),
    ABS_TIME("ABS_TIME"),
    REL_TIME("REL_TIME"),
    ABS_COUNT("ABS_COUNT"),
    REL_COUNT("REL_COUNT"),
    CHANNEL_FREQ("CHANNEL_FREQ"),
    TAPE_INDEX("TAPE-INDEX"),
    FRAME("FRAME");

    private final String protocolString;

    SeekMode(String protocolString) {
        this.protocolString = protocolString;
    }

    @Override
    public String toString() {
        return protocolString;
    }

    public static SeekMode valueOrExceptionOf(String s) {
        for (SeekMode seekMode : values()) {
            if (seekMode.protocolString.equals(s)) {
                return seekMode;
            }
        }
        throw new IllegalArgumentException("Invalid seek mode string: " + s);
    }
}
