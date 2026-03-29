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

import org.jupnp.model.Validatable;
import org.jupnp.model.ValidationError;

/**
 * Version of the UPnP Device Architecture (UDA), defaults to 1.0.
 *
 * @author Christian Bauer
 */
public class UDAVersion implements Validatable {

    private int major = 1;
    private int minor = 0;

    public UDAVersion() {
    }

    public UDAVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (getMajor() != 1) {
            errors.add(new ValidationError(getClass(), "major", "UDA major spec version must be 1"));
        }
        if (getMajor() < 0) {
            errors.add(new ValidationError(getClass(), "minor", "UDA minor spec version must be equal or greater 0"));
        }

        return errors;
    }
}
