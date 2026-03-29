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

import org.jupnp.model.types.BytesRange;
import org.jupnp.model.types.InvalidValueException;

/**
 *
 * @author Christian Bauer
 * @author Mario Franco
 */
public class ContentRangeHeader extends UpnpHeader<BytesRange> {

    public static final String PREFIX = "bytes ";

    public ContentRangeHeader() {
    }

    public ContentRangeHeader(BytesRange value) {
        setValue(value);
    }

    public ContentRangeHeader(String s) {
        setString(s);
    }

    @Override
    public void setString(String s) throws InvalidHeaderException {
        try {
            setValue(BytesRange.valueOf(s, PREFIX));
        } catch (InvalidValueException e) {
            throw new InvalidHeaderException("Invalid Range Header: " + e.getMessage(), e);
        }
    }

    @Override
    public String getString() {
        return getValue().getString(true, PREFIX);
    }
}
