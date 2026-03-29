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

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.message.header.InvalidHeaderException;
import org.jupnp.model.types.PragmaType;

/**
 * DLNA Pragma tokens:
 * - getIfoFileURI.dlna.org
 * - ifoFileURI.dlna.org
 * 
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class PragmaHeader extends DLNAHeader<List<PragmaType>> {

    public PragmaHeader() {
        setValue(new ArrayList<>());
    }

    @Override
    public void setString(String s) {
        if (!s.isEmpty()) {
            if (s.endsWith(";")) {
                s = s.substring(0, s.length() - 1);
            }
            String[] list = s.split("\\s*;\\s*");
            List<PragmaType> value = new ArrayList<>();
            for (String pragma : list) {
                value.add(PragmaType.valueOf(pragma));
            }
            return;
        }
        throw new InvalidHeaderException("Invalid Pragma header value: " + s);
    }

    @Override
    public String getString() {
        List<PragmaType> v = getValue();
        StringBuilder sb = new StringBuilder();
        for (PragmaType pragma : v) {
            sb.append(sb.length() == 0 ? "" : ",");
            sb.append(pragma.getString());
        }
        return sb.toString();
    }
}
