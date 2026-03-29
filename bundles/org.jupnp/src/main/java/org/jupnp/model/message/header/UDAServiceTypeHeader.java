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

import java.net.URI;

import org.jupnp.model.types.UDAServiceType;

/**
 * @author Christian Bauer
 */
public class UDAServiceTypeHeader extends ServiceTypeHeader {

    public UDAServiceTypeHeader() {
    }

    public UDAServiceTypeHeader(URI uri) {
        super(uri);
    }

    public UDAServiceTypeHeader(UDAServiceType value) {
        super(value);
    }

    @Override
    public void setString(String s) throws InvalidHeaderException {
        try {
            setValue(UDAServiceType.valueOf(s));
        } catch (Exception e) {
            throw new InvalidHeaderException("Invalid UDA service type header value, " + e.getMessage(), e);
        }
    }
}
