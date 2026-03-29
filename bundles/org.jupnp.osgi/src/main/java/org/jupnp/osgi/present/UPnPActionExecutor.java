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
package org.jupnp.osgi.present;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.osgi.util.OSGiDataConverter;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Bruce Green
 */
class UPnPActionExecutor implements ActionExecutor {

    private final Logger logger = LoggerFactory.getLogger(UPnPActionExecutor.class);

    private UPnPAction action;

    public UPnPActionExecutor(UPnPAction action) {
        this.action = action;
    }

    @Override
    public void execute(ActionInvocation<LocalService> actionInvocation) {
        logger.trace("ENTRY {}.{}: {}", this.getClass().getName(), "execute", actionInvocation);

        ActionArgumentValue<LocalService>[] inputs = actionInvocation.getInput();

        Dictionary<String, Object> args = new Hashtable<>();
        for (ActionArgumentValue<LocalService> input : inputs) {
            ActionArgument<?> argument = input.getArgument();

            args.put(argument.getName(), OSGiDataConverter.toOSGiValue(input.getDatatype(), input.getValue()));
        }

        try {
            Dictionary<String, Object> out = action.invoke(args);

            if (out != null) {
                for (String key : Collections.list(out.keys())) {

                    Object value = out.get(key);
                    if (value != null) {
                        UPnPStateVariable variable = action.getStateVariable(key);
                        value = OSGiDataConverter.tojUPnPValue(variable.getUPnPDataType(), value);

                        try {
                            // System.out.printf("*** key: %s value: %s [%s]\n", key, value, value);
                            actionInvocation.setOutput(key, value);
                        } catch (InvalidValueException e) {
                            logger.error("Error executing action {} variable {}.", action.getName(), key);
                            logger.error(e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error executing action ({}).", action.getName());
            logger.error(e.getMessage());
        }
    }
}
