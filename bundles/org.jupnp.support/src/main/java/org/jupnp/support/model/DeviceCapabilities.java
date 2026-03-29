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

import java.util.Map;

import org.jupnp.model.ModelUtil;
import org.jupnp.model.action.ActionArgumentValue;

/**
 * @author Christian Bauer
 */
public class DeviceCapabilities {

    private StorageMedium[] playMedia;
    private StorageMedium[] recMedia = new StorageMedium[] { StorageMedium.NOT_IMPLEMENTED };
    private RecordQualityMode[] recQualityModes = new RecordQualityMode[] { RecordQualityMode.NOT_IMPLEMENTED };

    public DeviceCapabilities(Map<String, ActionArgumentValue<?>> args) {
        this(StorageMedium.valueOfCommaSeparatedList((String) args.get("PlayMedia").getValue()),
                StorageMedium.valueOfCommaSeparatedList((String) args.get("RecMedia").getValue()),
                RecordQualityMode.valueOfCommaSeparatedList((String) args.get("RecQualityModes").getValue()));
    }

    public DeviceCapabilities(StorageMedium[] playMedia) {
        this.playMedia = playMedia;
    }

    public DeviceCapabilities(StorageMedium[] playMedia, StorageMedium[] recMedia,
            RecordQualityMode[] recQualityModes) {
        this.playMedia = playMedia;
        this.recMedia = recMedia;
        this.recQualityModes = recQualityModes;
    }

    public StorageMedium[] getPlayMedia() {
        return playMedia;
    }

    public StorageMedium[] getRecMedia() {
        return recMedia;
    }

    public RecordQualityMode[] getRecQualityModes() {
        return recQualityModes;
    }

    public String getPlayMediaString() {
        return ModelUtil.toCommaSeparatedList(playMedia);
    }

    public String getRecMediaString() {
        return ModelUtil.toCommaSeparatedList(recMedia);
    }

    public String getRecQualityModesString() {
        return ModelUtil.toCommaSeparatedList(recQualityModes);
    }
}
