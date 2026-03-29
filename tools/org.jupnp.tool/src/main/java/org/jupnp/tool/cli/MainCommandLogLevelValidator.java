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
public class MainCommandLogLevelValidator implements IParameterValidator {

    private static final Set<String> PARAMETER_NAMES = Set.of("--loglevel", "-l");
    private static final Set<String> VALID_VALUES = Set.of("OFF", "ERROR", "WARN", "INFO", "DEBUG", "TRACE");

    @Override
    public void validate(String name, String value) throws ParameterException {
        if (PARAMETER_NAMES.contains(name) && VALID_VALUES.stream().noneMatch(v -> v.equalsIgnoreCase(value))) {
            throw new ParameterException(
                    "Parameter " + name + " must be {" + String.join("|", VALID_VALUES) + "} (found " + value + ")");
        }
    }
}
