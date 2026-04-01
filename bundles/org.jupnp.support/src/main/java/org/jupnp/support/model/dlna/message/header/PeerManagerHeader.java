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

import org.jupnp.model.ServiceReference;
import org.jupnp.model.message.header.InvalidHeaderException;

/**
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class PeerManagerHeader extends DLNAHeader<ServiceReference> {

    public PeerManagerHeader() {
    }

    @Override
    public void setString(String s) {
        if (!s.isEmpty()) {
            try {
                ServiceReference serviceReference = new ServiceReference(s);
                if (serviceReference.getUdn() != null && serviceReference.getServiceId() != null) {
                    setValue(serviceReference);
                    return;
                }
            } catch (Exception e) {
                // no need to take any precaution measure
            }
        }
        throw new InvalidHeaderException("Invalid PeerManager header value: " + s);
    }

    @Override
    public String getString() {
        return getValue().toString();
    }
}
