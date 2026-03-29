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

import org.jupnp.model.meta.ActionArgument;
import org.jupnp.osgi.util.UPnPTypeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: This class is unused?
 *
 * @author Bruce Green
 */
public class UPnPActionArgumentImpl extends UPnPStateVariableImpl {

    private final Logger logger = LoggerFactory.getLogger(UPnPActionArgumentImpl.class);

    private ActionArgument<?> argument;

    public UPnPActionArgumentImpl(ActionArgument<?> argument) {
        super(argument.getAction().getService().getStateVariable(argument.getRelatedStateVariableName()));
        this.argument = argument;
    }

    @Override
    public String getName() {
        return argument.getName();
    }

    @Override
    public Class getJavaDataType() {
        String type = argument.getDatatype().getBuiltin().getDescriptorName();
        Class<?> clazz = UPnPTypeUtil.getUPnPClass(type);
        if (clazz == null) {
            logger.warn("Cannot covert UPnP type {} to UPnP Java type", type);
        }
        return clazz != null ? clazz : argument.getDatatype().getClass();
    }

    @Override
    public String getUPnPDataType() {
        return argument.getDatatype().getDisplayString();
    }
}
