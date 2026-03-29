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
package org.jupnp.model.types;

/**
 * Notification message types for SSDP.
 *
 * @author Christian Bauer
 */
public enum NotificationSubtype {

    ALIVE("ssdp:alive"),
    UPDATE("ssdp:update"),
    BYEBYE("ssdp:byebye"),
    ALL("ssdp:all"),
    DISCOVER("ssdp:discover"),
    PROPCHANGE("upnp:propchange");

    private final String headerString;

    NotificationSubtype(String headerString) {
        this.headerString = headerString;
    }

    public String getHeaderString() {
        return headerString;
    }
}
