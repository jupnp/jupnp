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
package org.jupnp.tool.cli;

import java.util.Set;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

/**
 * @author Jochen Hiller - Initial contribution
 */
public class MainCommandMulticastResponsePortValidator implements IParameterValidator {

    private static final Set<String> PARAMETER_NAMES = Set.of("--multicastResponsePort", "-m");

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (PARAMETER_NAMES.contains(name)) {
            String errorMsg = "Parameter " + name + " ";
            try {
                int port = Integer.parseInt(value);
                if (port < 0 || port > 65535) {
                    throw new ParameterException(errorMsg + "must be between 0..65535");
                }
            } catch (NumberFormatException e) {
                // must be valid port number
                throw new ParameterException(errorMsg + "is not a valid number)");
            }
        }
    }
}
