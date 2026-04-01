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
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.DeviceUSNHeader;
import org.jupnp.model.message.header.InterfaceMacHeader;
import org.jupnp.model.message.header.LocationHeader;
import org.jupnp.model.message.header.MaxAgeHeader;
import org.jupnp.model.message.header.ServiceUSNHeader;
import org.jupnp.model.message.header.UDNHeader;
import org.jupnp.model.message.header.USNRootDeviceHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.types.NamedDeviceType;
import org.jupnp.model.types.NamedServiceType;
import org.jupnp.model.types.UDN;

/**
 * @author Christian Bauer
 */
public class IncomingSearchResponse extends IncomingDatagramMessage<UpnpResponse> {

    public IncomingSearchResponse(IncomingDatagramMessage<UpnpResponse> source) {
        super(source);
    }

    public boolean isSearchResponseMessage() {
        UpnpHeader st = getHeaders().getFirstHeader(UpnpHeader.Type.ST);
        UpnpHeader usn = getHeaders().getFirstHeader(UpnpHeader.Type.USN);
        UpnpHeader ext = getHeaders().getFirstHeader(UpnpHeader.Type.EXT); // Has no value!
        return st != null && st.getValue() != null && usn != null && usn.getValue() != null && ext != null;
    }

    public UDN getRootDeviceUDN() {
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

    public URL getLocationURL() {
        LocationHeader header = getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION, LocationHeader.class);
        if (header != null) {
            return header.getValue();
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
