/*
 * Copyright (C) 2011-2024 4th Line GmbH, Switzerland and others
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
import org.jupnp.support.model.dlna.types.NormalPlayTime;

/**
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class RealTimeInfoHeader extends DLNAHeader<NormalPlayTime> {

    public static final String PREFIX = "DLNA.ORG_TLAG=";

    public RealTimeInfoHeader() {
    }

    @Override
    public void setString(String s) {
        if (!s.isEmpty() && s.startsWith(PREFIX)) {
            try {
                s = s.substring(PREFIX.length());
                setValue(s.equals("*") ? null : NormalPlayTime.valueOf(s));
                return;
            } catch (Exception e) {
                // no need to take any precaution measure
            }
        }
        throw new InvalidHeaderException("Invalid RealTimeInfo header value: " + s);
    }

    @Override
    public String getString() {
        NormalPlayTime v = getValue();
        if (v == null) {
            return PREFIX + "*";
        }
        return PREFIX + v.getString();
    }
}
