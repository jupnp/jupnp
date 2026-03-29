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

import java.util.List;

import org.jupnp.support.model.Res;
import org.jupnp.support.model.StorageMedium;
import org.jupnp.support.model.container.Container;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class Movie extends VideoItem {

    public static final Class CLASS = new Class("object.item.videoItem.movie");

    public Movie() {
        setClazz(CLASS);
    }

    public Movie(Item other) {
        super(other);
    }

    public Movie(String id, Container parent, String title, String creator, Res... resource) {
        this(id, parent.getId(), title, creator, resource);
    }

    public Movie(String id, String parentID, String title, String creator, Res... resource) {
        super(id, parentID, title, creator, resource);
        setClazz(CLASS);
    }

    public StorageMedium getStorageMedium() {
        return getFirstPropertyValue(UPNP.STORAGE_MEDIUM.class);
    }

    public Movie setStorageMedium(StorageMedium storageMedium) {
        replaceFirstProperty(new UPNP.STORAGE_MEDIUM(storageMedium));
        return this;
    }

    public Integer getDVDRegionCode() {
        return getFirstPropertyValue(UPNP.DVD_REGION_CODE.class);
    }

    public Movie setDVDRegionCode(Integer dvdRegionCode) {
        replaceFirstProperty(new UPNP.DVD_REGION_CODE(dvdRegionCode));
        return this;
    }

    public String getChannelName() {
        return getFirstPropertyValue(UPNP.CHANNEL_NAME.class);
    }

    public Movie setChannelName(String channelName) {
        replaceFirstProperty(new UPNP.CHANNEL_NAME(channelName));
        return this;
    }

    public String getFirstScheduledStartTime() {
        return getFirstPropertyValue(UPNP.SCHEDULED_START_TIME.class);
    }

    public String[] getScheduledStartTimes() {
        List<String> list = getPropertyValues(UPNP.SCHEDULED_START_TIME.class);
        return list.toArray(new String[list.size()]);
    }

    public Movie setScheduledStartTimes(String[] strings) {
        removeProperties(UPNP.SCHEDULED_START_TIME.class);
        for (String s : strings) {
            addProperty(new UPNP.SCHEDULED_START_TIME(s));
        }
        return this;
    }

    public String getFirstScheduledEndTime() {
        return getFirstPropertyValue(UPNP.SCHEDULED_END_TIME.class);
    }

    public String[] getScheduledEndTimes() {
        List<String> list = getPropertyValues(UPNP.SCHEDULED_END_TIME.class);
        return list.toArray(new String[list.size()]);
    }

    public Movie setScheduledEndTimes(String[] strings) {
        removeProperties(UPNP.SCHEDULED_END_TIME.class);
        for (String s : strings) {
            addProperty(new UPNP.SCHEDULED_END_TIME(s));
        }
        return this;
    }
}
