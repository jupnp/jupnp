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

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.ModelUtil;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public enum RecordQualityMode {

    EP("0:EP"),
    LP("1:LP"),
    SP("2:SP"),
    BASIC("0:BASIC"),
    MEDIUM("1:MEDIUM"),
    HIGH("2:HIGH"),
    NOT_IMPLEMENTED("NOT_IMPLEMENTED");

    private final String protocolString;

    RecordQualityMode(String protocolString) {
        this.protocolString = protocolString;
    }

    @Override
    public String toString() {
        return protocolString;
    }

    public static RecordQualityMode valueOrExceptionOf(String s) {
        for (RecordQualityMode recordQualityMode : values()) {
            if (recordQualityMode.protocolString.equals(s)) {
                return recordQualityMode;
            }
        }
        throw new IllegalArgumentException("Invalid record quality mode string: " + s);
    }

    public static RecordQualityMode[] valueOfCommaSeparatedList(String s) {
        String[] strings = ModelUtil.fromCommaSeparatedList(s);
        if (strings == null) {
            return new RecordQualityMode[0];
        }
        List<RecordQualityMode> result = new ArrayList<>();
        for (String rqm : strings) {
            for (RecordQualityMode recordQualityMode : values()) {
                if (recordQualityMode.protocolString.equals(rqm)) {
                    result.add(recordQualityMode);
                }
            }
        }
        return result.toArray(new RecordQualityMode[result.size()]);
    }
}
