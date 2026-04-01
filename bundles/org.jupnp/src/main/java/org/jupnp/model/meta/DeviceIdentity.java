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
package org.jupnp.model.meta;

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.Constants;
import org.jupnp.model.Validatable;
import org.jupnp.model.ValidationError;
import org.jupnp.model.types.UDN;

/**
 * Unique device name, received and offered during discovery with SSDP.
 *
 * @author Christian Bauer
 */
public class DeviceIdentity implements Validatable {

    private final UDN udn;
    private final Integer maxAgeSeconds;

    public DeviceIdentity(UDN udn, DeviceIdentity template) {
        this.udn = udn;
        this.maxAgeSeconds = template.getMaxAgeSeconds();
    }

    public DeviceIdentity(UDN udn) {
        this.udn = udn;
        this.maxAgeSeconds = Constants.MIN_ADVERTISEMENT_AGE_SECONDS;
    }

    public DeviceIdentity(UDN udn, Integer maxAgeSeconds) {
        this.udn = udn;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public UDN getUdn() {
        return udn;
    }

    public Integer getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DeviceIdentity that = (DeviceIdentity) o;

        if (!udn.equals(that.udn)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return udn.hashCode();
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") UDN: " + getUdn();
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (getUdn() == null) {
            errors.add(new ValidationError(getClass(), "major", "Device has no UDN"));
        }

        return errors;
    }
}
