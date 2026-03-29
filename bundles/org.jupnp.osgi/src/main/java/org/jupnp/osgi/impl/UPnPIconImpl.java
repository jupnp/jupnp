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
package org.jupnp.osgi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.RemoteDevice;
import org.osgi.service.upnp.UPnPIcon;

/**
 * @author Bruce Green
 */
public class UPnPIconImpl implements UPnPIcon {
    private Icon icon;

    public UPnPIconImpl(Icon icon) {
        this.icon = icon;
    }

    @Override
    public String getMimeType() {
        return icon.getMimeType().toString();
    }

    @Override
    public int getWidth() {
        return icon.getWidth();
    }

    @Override
    public int getHeight() {
        return icon.getHeight();
    }

    @Override
    public int getSize() {
        return -1;
    }

    @Override
    public int getDepth() {
        return icon.getDepth();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        InputStream in = null;
        Device device = icon.getDevice();

        if (device instanceof RemoteDevice) {
            URL url = ((RemoteDevice) icon.getDevice()).normalizeURI(icon.getUri());

            in = url.openStream();
        }

        return in;
    }
}
