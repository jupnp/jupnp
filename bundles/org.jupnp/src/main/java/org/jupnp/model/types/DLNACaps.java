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
package org.jupnp.model.types;

import java.util.Arrays;

import org.jupnp.model.ModelUtil;

/**
 * An arbitrary list of comma-separated elements, representing DLNA capabilities (whatever that is).
 *
 * @author Christian Bauer
 */
public class DLNACaps {

    final String[] caps;

    public DLNACaps(String[] caps) {
        this.caps = caps;
    }

    public String[] getCaps() {
        return caps;
    }

    public static DLNACaps valueOf(String s) throws InvalidValueException {
        if (s == null || s.isEmpty())
            return new DLNACaps(new String[0]);
        String[] caps = s.split(",");
        String[] trimmed = new String[caps.length];
        for (int i = 0; i < caps.length; i++) {
            trimmed[i] = caps[i].trim();
        }
        return new DLNACaps(trimmed);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DLNACaps dlnaCaps = (DLNACaps) o;

        if (!Arrays.equals(caps, dlnaCaps.caps)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(caps);
    }

    @Override
    public String toString() {
        return ModelUtil.toCommaSeparatedList(getCaps());
    }
}
