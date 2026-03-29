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
package org.jupnp.controlpoint.event;

import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.types.NotificationSubtype;

/**
 * @author Christian Bauer
 */
public class Search {

    protected UpnpHeader<NotificationSubtype> searchType = new STAllHeader();
    protected int mxSeconds = MXHeader.DEFAULT_VALUE;

    public Search() {
    }

    public Search(UpnpHeader searchType) {
        this.searchType = searchType;
    }

    public Search(UpnpHeader searchType, int mxSeconds) {
        this.searchType = searchType;
        this.mxSeconds = mxSeconds;
    }

    public Search(int mxSeconds) {
        this.mxSeconds = mxSeconds;
    }

    public UpnpHeader getSearchType() {
        return searchType;
    }

    public int getMxSeconds() {
        return mxSeconds;
    }
}
