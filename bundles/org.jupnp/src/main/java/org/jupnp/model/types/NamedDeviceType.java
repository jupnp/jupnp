/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.model.types;

import java.util.regex.Pattern;

/**
 * Combines a {@link UDN} with a {@link DeviceType}, string representation
 * is separated by double-colon.
 *
 * @author Christian Bauer
 */
public class NamedDeviceType {

    public static final Pattern PATTERN_DELIMITER = Pattern.compile("::");

    private UDN udn;
    private DeviceType deviceType;

    public NamedDeviceType(UDN udn, DeviceType deviceType) {
        this.udn = udn;
        this.deviceType = deviceType;
    }

    public UDN getUdn() {
        return udn;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public static NamedDeviceType valueOf(String s) throws InvalidValueException {
        String[] strings = PATTERN_DELIMITER.split(s, 3);
        if (strings.length != 2) {
            throw new InvalidValueException("Can't parse UDN::DeviceType from: " + s);
        }

        UDN udn;
        try {
            udn = UDN.valueOf(strings[0]);
        } catch (Exception e) {
            throw new InvalidValueException("Can't parse UDN: " + strings[0]);
        }

        DeviceType deviceType = DeviceType.valueOf(strings[1]);
        return new NamedDeviceType(udn, deviceType);
    }

    @Override
    public String toString() {
        return getUdn().toString() + "::" + getDeviceType().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof NamedDeviceType)) {
            return false;
        }

        NamedDeviceType that = (NamedDeviceType) o;

        if (!deviceType.equals(that.deviceType)) {
            return false;
        }
        if (!udn.equals(that.udn)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = udn.hashCode();
        result = 31 * result + deviceType.hashCode();
        return result;
    }
}
