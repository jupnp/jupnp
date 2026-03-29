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

import java.util.HashMap;
import java.util.Map;

import org.jupnp.model.ModelUtil;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public enum StorageMedium {

    UNKNOWN,
    DV,
    MINI_DV("MINI-DV"),
    VHS,
    W_VHS("W-VHS"),
    S_VHS("S-VHS"),
    D_VHS("D-VHS"),
    VHSC,
    VIDEO8,
    HI8,
    CD_ROM("CD-ROM"),
    CD_DA("CD-DA"),
    CD_R("CD-R"),
    CD_RW("CD-RW"),
    VIDEO_CD("VIDEO-CD"),
    SACD,
    MD_AUDIO("M-AUDIO"),
    MD_PICTURE("MD-PICTURE"),
    DVD_ROM("DVD-ROM"),
    DVD_VIDEO("DVD-VIDEO"),
    DVD_R("DVD-R"),
    DVD_PLUS_RW("DVD+RW"),
    DVD_MINUS_RW("DVD-RW"),
    DVD_RAM("DVD-RAM"),
    DVD_AUDIO("DVD-AUDIO"),
    DAT,
    LD,
    HDD,
    MICRO_MV("MICRO_MV"),
    NETWORK,
    NONE,
    NOT_IMPLEMENTED,
    VENDOR_SPECIFIC;

    private static final Map<String, StorageMedium> byProtocolString = new HashMap<>() {
        private static final long serialVersionUID = 1635734229355713298L;

        {
            for (StorageMedium e : StorageMedium.values()) {
                put(e.protocolString, e);
            }
        }
    };

    private final String protocolString;

    StorageMedium() {
        this(null);
    }

    StorageMedium(String protocolString) {
        this.protocolString = protocolString == null ? this.name() : protocolString;
    }

    @Override
    public String toString() {
        return protocolString;
    }

    public static StorageMedium valueOrExceptionOf(String s) {
        StorageMedium sm = byProtocolString.get(s);
        if (sm != null) {
            return sm;
        }
        throw new IllegalArgumentException("Invalid storage medium string: " + s);
    }

    public static StorageMedium valueOrVendorSpecificOf(String s) {
        StorageMedium sm = byProtocolString.get(s);
        return sm != null ? sm : StorageMedium.VENDOR_SPECIFIC;
    }

    public static StorageMedium[] valueOfCommaSeparatedList(String s) {
        String[] strings = ModelUtil.fromCommaSeparatedList(s);
        if (strings == null) {
            return new StorageMedium[0];
        }
        StorageMedium[] result = new StorageMedium[strings.length];
        for (int i = 0; i < strings.length; i++) {
            result[i] = valueOrVendorSpecificOf(strings[i]);
        }
        return result;
    }
}
