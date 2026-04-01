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

import org.jupnp.model.types.UDN;

/**
 * @author Christian Bauer
 */
public class USNRootDeviceHeader extends UpnpHeader<UDN> {

    public static final String ROOT_DEVICE_SUFFIX = "::upnp:rootdevice";

    public USNRootDeviceHeader() {
    }

    public USNRootDeviceHeader(UDN udn) {
        setValue(udn);
    }

    @Override
    public void setString(String s) throws InvalidHeaderException {
        if (!s.startsWith(UDN.PREFIX) || !s.endsWith(ROOT_DEVICE_SUFFIX)) {
            throw new InvalidHeaderException("Invalid root device USN header value, must start with '" + UDN.PREFIX
                    + "' and end with '" + ROOT_DEVICE_SUFFIX + "' but is '" + s + "'");
        }
        UDN udn = new UDN(s.substring(UDN.PREFIX.length(), s.length() - ROOT_DEVICE_SUFFIX.length()));
        setValue(udn);
    }

    @Override
    public String getString() {
        return getValue().toString() + ROOT_DEVICE_SUFFIX;
    }
}
