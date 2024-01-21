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
package org.jupnp.model.message.header;

import java.net.URI;

import org.jupnp.model.types.ServiceType;

/**
 * @author Christian Bauer
 */
public class ServiceTypeHeader extends UpnpHeader<ServiceType> {

    public ServiceTypeHeader() {
    }

    public ServiceTypeHeader(URI uri) {
        setString(uri.toString());
    }

    public ServiceTypeHeader(ServiceType value) {
        setValue(value);
    }

    public void setString(String s) throws InvalidHeaderException {
        try {
            setValue(ServiceType.valueOf(s));
        } catch (RuntimeException ex) {
            throw new InvalidHeaderException("Invalid service type header value, " + ex.getMessage(), ex);
        }
    }

    public String getString() {
        return getValue().toString();
    }
}
