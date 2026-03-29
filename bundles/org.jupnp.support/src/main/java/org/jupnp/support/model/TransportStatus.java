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
package org.jupnp.support.model;

/**
 *
 */
public enum TransportStatus {

    OK,
    ERROR_OCCURRED,
    CUSTOM;

    String value;

    TransportStatus() {
        this.value = name();
    }

    public String getValue() {
        return value;
    }

    public TransportStatus setValue(String value) {
        this.value = value;
        return this;
    }

    public static TransportStatus valueOrCustomOf(String s) {
        try {
            return TransportStatus.valueOf(s);
        } catch (IllegalArgumentException e) {
            return TransportStatus.CUSTOM.setValue(s);
        }
    }
}
