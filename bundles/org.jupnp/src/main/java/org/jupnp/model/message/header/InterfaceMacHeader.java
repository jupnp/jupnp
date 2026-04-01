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

import org.jupnp.util.io.HexBin;

/**
 * Custom header for jUPnP, used to transfer the MAC ethernet address for Wake-on-LAN.
 *
 * @author Christian Bauer
 */
public class InterfaceMacHeader extends UpnpHeader<byte[]> {

    public InterfaceMacHeader() {
    }

    public InterfaceMacHeader(byte[] value) {
        setValue(value);
    }

    public InterfaceMacHeader(String s) {
        setString(s);
    }

    @Override
    public void setString(String s) throws InvalidHeaderException {
        byte[] bytes = HexBin.stringToBytes(s, ":");
        setValue(bytes);
        if (bytes.length != 6) {
            throw new InvalidHeaderException("Invalid MAC address: " + s);
        }
    }

    @Override
    public String getString() {
        return HexBin.bytesToString(getValue(), ":");
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") '" + getString() + "'";
    }
}
