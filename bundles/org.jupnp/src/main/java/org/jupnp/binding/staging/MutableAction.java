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
package org.jupnp.binding.staging;

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;

/**
 * @author Christian Bauer
 */
public class MutableAction {

    public String name;
    public List<MutableActionArgument> arguments = new ArrayList<>();

    public Action build() {
        return new Action(name, createActionArguments());
    }

    public ActionArgument[] createActionArguments() {
        ActionArgument[] array = new ActionArgument[arguments.size()];
        int i = 0;
        for (MutableActionArgument argument : arguments) {
            array[i++] = argument.build();
        }
        return array;
    }
}
