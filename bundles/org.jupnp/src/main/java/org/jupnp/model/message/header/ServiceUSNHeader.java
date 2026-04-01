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

import org.jupnp.model.types.NamedServiceType;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;

/**
 * @author Christian Bauer
 */
public class ServiceUSNHeader extends UpnpHeader<NamedServiceType> {

    public ServiceUSNHeader() {
    }

    public ServiceUSNHeader(UDN udn, ServiceType serviceType) {
        setValue(new NamedServiceType(udn, serviceType));
    }

    public ServiceUSNHeader(NamedServiceType value) {
        setValue(value);
    }

    @Override
    public void setString(String s) throws InvalidHeaderException {
        try {
            setValue(NamedServiceType.valueOf(s));
        } catch (Exception e) {
            throw new InvalidHeaderException("Invalid service USN header value, " + e.getMessage(), e);
        }
    }

    @Override
    public String getString() {
        return getValue().toString();
    }
}
