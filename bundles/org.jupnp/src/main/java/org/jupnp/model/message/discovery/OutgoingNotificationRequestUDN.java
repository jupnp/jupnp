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
package org.jupnp.model.message.discovery;

import org.jupnp.model.Location;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.types.NotificationSubtype;

/**
 * @author Christian Bauer
 */
public class OutgoingNotificationRequestUDN extends OutgoingNotificationRequest {

    public OutgoingNotificationRequestUDN(Location location, LocalDevice device, NotificationSubtype type) {
        super(location, device, type);

        getHeaders().add(UpnpHeader.Type.NT, new UDNHeader(device.getIdentity().getUdn()));
        getHeaders().add(UpnpHeader.Type.USN, new UDNHeader(device.getIdentity().getUdn()));
    }
}
