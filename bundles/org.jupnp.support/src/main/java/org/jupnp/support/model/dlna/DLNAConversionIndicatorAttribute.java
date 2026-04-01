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
package org.jupnp.support.model.dlna;

/**
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class DLNAConversionIndicatorAttribute extends DLNAAttribute<DLNAConversionIndicator> {

    public DLNAConversionIndicatorAttribute() {
        setValue(DLNAConversionIndicator.NONE);
    }

    public DLNAConversionIndicatorAttribute(DLNAConversionIndicator indicator) {
        setValue(indicator);
    }

    @Override
    public void setString(String s, String cf) {
        DLNAConversionIndicator value = null;
        try {
            value = DLNAConversionIndicator.valueOf(Integer.parseInt(s));
        } catch (NumberFormatException numberFormatException) {
        }
        if (value == null) {
            throw new InvalidDLNAProtocolAttributeException("Can't parse DLNA play speed integer from: " + s);
        }
        setValue(value);
    }

    @Override
    public String getString() {
        return Integer.toString(getValue().getCode());
    }
}
