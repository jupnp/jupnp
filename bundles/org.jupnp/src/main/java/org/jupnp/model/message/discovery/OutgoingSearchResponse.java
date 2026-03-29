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
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.EXTHeader;
import org.jupnp.model.message.header.InterfaceMacHeader;
import org.jupnp.model.message.header.LocationHeader;
import org.jupnp.model.message.header.MaxAgeHeader;
import org.jupnp.model.message.header.ServerHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;

/**
 * @author Christian Bauer
 */
public class OutgoingSearchResponse extends OutgoingDatagramMessage<UpnpResponse> {

    public OutgoingSearchResponse(IncomingDatagramMessage request, Location location, LocalDevice device) {

        super(new UpnpResponse(UpnpResponse.Status.OK), request.getSourceAddress(), request.getSourcePort());

        getHeaders().add(UpnpHeader.Type.MAX_AGE, new MaxAgeHeader(device.getIdentity().getMaxAgeSeconds()));
        getHeaders().add(UpnpHeader.Type.LOCATION, new LocationHeader(location.getURL()));
        getHeaders().add(UpnpHeader.Type.SERVER, new ServerHeader());
        getHeaders().add(UpnpHeader.Type.EXT, new EXTHeader());

        if ("true".equals(System.getProperty(Constants.SYSTEM_PROPERTY_ANNOUNCE_MAC_ADDRESS))
                && location.getNetworkAddress().getHardwareAddress() != null) {
            getHeaders().add(UpnpHeader.Type.EXT_IFACE_MAC,
                    new InterfaceMacHeader(location.getNetworkAddress().getHardwareAddress()));
        }
    }
}
