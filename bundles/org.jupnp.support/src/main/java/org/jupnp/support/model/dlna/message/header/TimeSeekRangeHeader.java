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
import org.jupnp.support.model.dlna.types.NormalPlayTimeRange;
import org.jupnp.support.model.dlna.types.TimeSeekRangeType;

/**
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class TimeSeekRangeHeader extends DLNAHeader<TimeSeekRangeType> {

    public TimeSeekRangeHeader() {
    }

    public TimeSeekRangeHeader(TimeSeekRangeType timeSeekRange) {
        setValue(timeSeekRange);
    }

    @Override
    public void setString(String s) {
        if (!s.isEmpty()) {
            String[] params = s.split(" ");
            if (params.length > 0) {
                try {
                    TimeSeekRangeType t = new TimeSeekRangeType(NormalPlayTimeRange.valueOf(params[0]));
                    if (params.length > 1) {
                        t.setBytesRange(BytesRange.valueOf(params[1]));
                    }
                    setValue(t);
                    return;
                } catch (InvalidValueException e) {
                    throw new InvalidHeaderException("Invalid TimeSeekRange header value: " + s + "; " + e.getMessage(),
                            e);
                }
            }
        }
        throw new InvalidHeaderException("Invalid TimeSeekRange header value: " + s);
    }

    @Override
    public String getString() {
        TimeSeekRangeType t = getValue();
        String s = t.getNormalPlayTimeRange().getString();
        if (t.getBytesRange() != null)
            s += " " + t.getBytesRange().getString(true);
        return s;
    }
}
