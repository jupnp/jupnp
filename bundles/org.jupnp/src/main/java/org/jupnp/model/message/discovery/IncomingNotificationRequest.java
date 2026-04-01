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

import java.net.URL;

import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.header.DeviceUSNHeader;
import org.jupnp.model.message.header.InterfaceMacHeader;
import org.jupnp.model.message.header.LocationHeader;
import org.jupnp.model.message.header.MaxAgeHeader;
import org.jupnp.model.message.header.NTSHeader;
import org.jupnp.model.message.header.ServiceUSNHeader;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.message.header.USNRootDeviceHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.types.NamedDeviceType;
import org.jupnp.model.types.NamedServiceType;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.model.types.UDN;

/**
 * @author Christian Bauer
 */
public class IncomingNotificationRequest extends IncomingDatagramMessage<UpnpRequest> {

    public IncomingNotificationRequest(IncomingDatagramMessage<UpnpRequest> source) {
        super(source);
    }

    public boolean isAliveMessage() {
        NTSHeader nts = getHeaders().getFirstHeader(UpnpHeader.Type.NTS, NTSHeader.class);
        return nts != null && nts.getValue().equals(NotificationSubtype.ALIVE);
    }

    public boolean isByeByeMessage() {
        NTSHeader nts = getHeaders().getFirstHeader(UpnpHeader.Type.NTS, NTSHeader.class);
        return nts != null && nts.getValue().equals(NotificationSubtype.BYEBYE);
    }

    public URL getLocationURL() {
        LocationHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION, LocationHeader.class);
        if (header != null) {
            return header.getValue();
        }
        return null;
    }

    /**
     * @return The UDN value after parsing various USN header values, or <code>null</code>.
     */
    public UDN getUDN() {
        // This processes the headers as specified in UDA 1.0, tables in section 1.1.12

        UpnpHeader<UDN> udnHeader = getHeaders().getFirstHeader(UpnpHeader.Type.USN, USNRootDeviceHeader.class);
        if (udnHeader != null) {
            return udnHeader.getValue();
        }

        udnHeader = getHeaders().getFirstHeader(UpnpHeader.Type.USN, UDNHeader.class);
        if (udnHeader != null) {
            return udnHeader.getValue();
        }

        UpnpHeader<NamedDeviceType> deviceTypeHeader = getHeaders().getFirstHeader(UpnpHeader.Type.USN,
                DeviceUSNHeader.class);
        if (deviceTypeHeader != null) {
            return deviceTypeHeader.getValue().getUdn();
        }

        UpnpHeader<NamedServiceType> serviceTypeHeader = getHeaders().getFirstHeader(UpnpHeader.Type.USN,
                ServiceUSNHeader.class);
        if (serviceTypeHeader != null) {
            return serviceTypeHeader.getValue().getUdn();
        }

        return null;
    }

    public Integer getMaxAge() {
        MaxAgeHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE, MaxAgeHeader.class);
        if (header != null) {
            return header.getValue();
        }
        return null;
    }

    public byte[] getInterfaceMacHeader() {
        InterfaceMacHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.EXT_IFACE_MAC,
                InterfaceMacHeader.class);
        if (header != null) {
            return header.getValue();
        }
        return null;
    }
}
