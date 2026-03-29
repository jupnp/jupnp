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

/**
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class SupportedHeader extends DLNAHeader<String[]> {

    public SupportedHeader() {
        setValue(new String[] {});
    }

    @Override
    public void setString(String s) {
        if (!s.isEmpty()) {
            if (s.endsWith(";")) {
                s = s.substring(0, s.length() - 1);
            }
            setValue(s.split("\\s*,\\s*"));
            return;
        }
        throw new InvalidHeaderException("Invalid Supported header value: " + s);
    }

    @Override
    public String getString() {
        String[] v = getValue();
        StringBuilder sb = new StringBuilder(v.length > 0 ? v[0] : "");
        for (int i = 1; i < v.length; i++) {
            sb.append(",");
            sb.append(v[i]);
        }
        return sb.toString();
    }
}
