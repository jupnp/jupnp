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
import java.util.Arrays;

import org.jupnp.model.ModelUtil;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class ProtocolInfos extends ArrayList<ProtocolInfo> {

    private static final long serialVersionUID = 5044783488205827065L;

    public ProtocolInfos(ProtocolInfo... info) {
        this.addAll(Arrays.asList(info));
    }

    public ProtocolInfos(String s) {
        String[] infos = ModelUtil.fromCommaSeparatedList(s);
        if (infos != null) {
            for (String info : infos) {
                add(new ProtocolInfo(info));
            }
        }
    }

    @Override
    public String toString() {
        return ModelUtil.toCommaSeparatedList(toArray(new ProtocolInfo[size()]));
    }
}
