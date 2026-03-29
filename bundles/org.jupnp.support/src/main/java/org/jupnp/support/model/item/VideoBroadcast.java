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
package org.jupnp.support.model.item;

import static org.jupnp.support.model.DIDLObject.Property.UPNP;

import java.net.URI;

import org.jupnp.support.model.Res;
import org.jupnp.support.model.container.Container;

/**
 * @author Christian Bauer
 */
public class VideoBroadcast extends VideoItem {

    public static final Class CLASS = new Class("object.item.videoItem.videoBroadcast");

    public VideoBroadcast() {
        setClazz(CLASS);
    }

    public VideoBroadcast(Item other) {
        super(other);
    }

    public VideoBroadcast(String id, Container parent, String title, String creator, Res... resource) {
        this(id, parent.getId(), title, creator, resource);
    }

    public VideoBroadcast(String id, String parentID, String title, String creator, Res... resource) {
        super(id, parentID, title, creator, resource);
        setClazz(CLASS);
    }

    public URI getIcon() {
        return getFirstPropertyValue(UPNP.ICON.class);
    }

    public VideoBroadcast setIcon(URI icon) {
        replaceFirstProperty(new UPNP.ICON(icon));
        return this;
    }

    public String getRegion() {
        return getFirstPropertyValue(UPNP.REGION.class);
    }

    public VideoBroadcast setRegion(String region) {
        replaceFirstProperty(new UPNP.REGION(region));
        return this;
    }

    public Integer getChannelNr() {
        return getFirstPropertyValue(UPNP.CHANNEL_NR.class);
    }

    public VideoBroadcast setChannelNr(Integer channelNr) {
        replaceFirstProperty(new UPNP.CHANNEL_NR(channelNr));
        return this;
    }
}
