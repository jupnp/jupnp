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
package org.jupnp.common.util;

import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;

public class BundleUtil {
    private static final Object[][] states = { { Integer.valueOf(Bundle.UNINSTALLED), "UNINSTALLED" },
            { Integer.valueOf(Bundle.INSTALLED), "INSTALLED" }, { Integer.valueOf(Bundle.RESOLVED), "RESOLVED" },
            { Integer.valueOf(Bundle.STARTING), "STARTING" }, { Integer.valueOf(Bundle.STOPPING), "STOPPING" },
            { Integer.valueOf(Bundle.ACTIVE), "ACTIVE" } };

    private static Map<Integer, String> stateLookup;

    private static Map<Integer, String> getStateLookup() {
        if (stateLookup == null) {
            stateLookup = new Hashtable<>();

            for (Object[] state : states) {
                stateLookup.put((Integer) state[0], (String) state[1]);
            }
        }

        return stateLookup;
    }

    public static String getBundleState(Bundle bundle) {
        String state = getStateLookup().get(Integer.valueOf(bundle.getState()));
        return state != null ? state : Integer.valueOf(bundle.getState()).toString();
    }
}
