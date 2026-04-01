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
package org.jupnp.support.model.dlna.message.header;

import org.jupnp.model.message.header.InvalidHeaderException;
import org.jupnp.model.types.BytesRange;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.support.model.dlna.types.AvailableSeekRangeType;
import org.jupnp.support.model.dlna.types.NormalPlayTimeRange;

/**
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class AvailableSeekRangeHeader extends DLNAHeader<AvailableSeekRangeType> {

    public AvailableSeekRangeHeader() {
    }

    public AvailableSeekRangeHeader(AvailableSeekRangeType timeSeekRange) {
        setValue(timeSeekRange);
    }

    @Override
    public void setString(String s) {
        if (!s.isEmpty()) {
            String[] params = s.split(" ");
            if (params.length > 1) {
                try {
                    AvailableSeekRangeType.Mode mode = null;
                    NormalPlayTimeRange timeRange = null;
                    BytesRange byteRange = null;

                    // Parse Mode
                    try {
                        mode = AvailableSeekRangeType.Mode.valueOf("MODE_" + params[0]);
                    } catch (IllegalArgumentException e) {
                        throw new InvalidValueException("Invalid AvailableSeekRange Mode");
                    }

                    boolean useTime = true;
                    // Parse Second Token
                    try {
                        timeRange = NormalPlayTimeRange.valueOf(params[1], true);
                    } catch (InvalidValueException timeInvalidValueException) {
                        try {
                            byteRange = BytesRange.valueOf(params[1]);
                            useTime = false;
                        } catch (InvalidValueException bytesInvalidValueException) {
                            throw new InvalidValueException("Invalid AvailableSeekRange Range");
                        }
                    }
                    if (useTime) {
                        if (params.length > 2) {
                            // Parse Third Token
                            byteRange = BytesRange.valueOf(params[2]);
                            setValue(new AvailableSeekRangeType(mode, timeRange, byteRange));
                        } else {
                            setValue(new AvailableSeekRangeType(mode, timeRange));
                        }
                    } else {
                        setValue(new AvailableSeekRangeType(mode, byteRange));
                    }
                    return;
                } catch (InvalidValueException e) {
                    throw new InvalidHeaderException(
                            "Invalid AvailableSeekRange header value: " + s + "; " + e.getMessage(), e);
                }
            }
        }
        throw new InvalidHeaderException("Invalid AvailableSeekRange header value: " + s);
    }

    @Override
    public String getString() {
        AvailableSeekRangeType t = getValue();
        String s = Integer.toString(t.getModeFlag().ordinal());
        if (t.getNormalPlayTimeRange() != null) {
            s += " " + t.getNormalPlayTimeRange().getString(false);
        }
        if (t.getBytesRange() != null) {
            s += " " + t.getBytesRange().getString(false);
        }
        return s;
    }
}
