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

import org.jupnp.model.Constants;
import org.jupnp.model.Location;
import org.jupnp.model.message.header.InterfaceMacHeader;
import org.jupnp.model.message.header.RootDeviceHeader;
import org.jupnp.model.message.header.USNRootDeviceHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.types.NotificationSubtype;

/**
 * @author Christian Bauer
 */
public class OutgoingNotificationRequestRootDevice extends OutgoingNotificationRequest {

    public OutgoingNotificationRequestRootDevice(Location location, LocalDevice device, NotificationSubtype type) {
        super(location, device, type);

        getHeaders().add(UpnpHeader.Type.NT, new RootDeviceHeader());
        getHeaders().add(UpnpHeader.Type.USN, new USNRootDeviceHeader(device.getIdentity().getUdn()));

        if ("true".equals(System.getProperty(Constants.SYSTEM_PROPERTY_ANNOUNCE_MAC_ADDRESS))
                && location.getNetworkAddress().getHardwareAddress() != null) {
            getHeaders().add(UpnpHeader.Type.EXT_IFACE_MAC,
                    new InterfaceMacHeader(location.getNetworkAddress().getHardwareAddress()));
        }
    }
}
