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

import org.jupnp.model.meta.StateVariable;
import org.jupnp.osgi.util.UPnPTypeUtil;
import org.osgi.service.upnp.UPnPStateVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPnPStateVariableImpl implements UPnPStateVariable {
    private final Logger logger = LoggerFactory.getLogger(UPnPStateVariableImpl.class);

    private StateVariable<?> variable;

    public UPnPStateVariableImpl(StateVariable<?> variable) {
        this.variable = variable;
    }

    @Override
    public String getName() {
        return variable.getName();
    }

    @Override
    public Class getJavaDataType() {
        String type = variable.getTypeDetails().getDatatype().getBuiltin().getDescriptorName();
        Class<?> clazz = UPnPTypeUtil.getUPnPClass(type);

        if (clazz == null) {
            logger.warn("Cannot covert UPnP type {} to UPnP Java type", type);
        }
        return clazz != null ? clazz : variable.getTypeDetails().getDatatype().getClass();
    }

    @Override
    public String getUPnPDataType() {
        return variable.getTypeDetails().getDatatype().getDisplayString();
    }

    @Override
    public Object getDefaultValue() {
        return variable.getTypeDetails().getDefaultValue();
    }

    @Override
    public String[] getAllowedValues() {
        return variable.getTypeDetails().getAllowedValues();
    }

    @Override
    public Number getMinimum() {
        return variable.getTypeDetails().getAllowedValueRange() != null
                ? variable.getTypeDetails().getAllowedValueRange().getMinimum()
                : null;
    }

    @Override
    public Number getMaximum() {
        return variable.getTypeDetails().getAllowedValueRange() != null
                ? variable.getTypeDetails().getAllowedValueRange().getMaximum()
                : null;
    }

    @Override
    public Number getStep() {
        return variable.getTypeDetails().getAllowedValueRange() != null
                ? variable.getTypeDetails().getAllowedValueRange().getStep()
                : null;
    }

    @Override
    public boolean sendsEvents() {
        return variable.getEventDetails().isSendEvents();
    }
}
