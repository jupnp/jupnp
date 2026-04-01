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

import org.jupnp.model.types.InvalidValueException;
import org.jupnp.support.avtransport.lastchange.AVTransportVariable.TransportPlaySpeed;

/**
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class DLNAPlaySpeedAttribute extends DLNAAttribute<TransportPlaySpeed[]> {

    public DLNAPlaySpeedAttribute() {
        setValue(new TransportPlaySpeed[] {});
    }

    public DLNAPlaySpeedAttribute(TransportPlaySpeed[] speeds) {
        setValue(speeds);
    }

    public DLNAPlaySpeedAttribute(String[] speeds) {
        TransportPlaySpeed[] sp = new TransportPlaySpeed[speeds.length];
        try {
            for (int i = 0; i < speeds.length; i++) {
                sp[i] = new TransportPlaySpeed(speeds[i]);
            }
        } catch (InvalidValueException invalidValueException) {
            throw new InvalidDLNAProtocolAttributeException("Can't parse DLNA play speeds.");
        }
        setValue(sp);
    }

    @Override
    public void setString(String s, String cf) {
        TransportPlaySpeed[] value = null;
        if (s != null && !s.isEmpty()) {
            String[] speeds = s.split(",");
            try {
                value = new TransportPlaySpeed[speeds.length];
                for (int i = 0; i < speeds.length; i++) {
                    value[i] = new TransportPlaySpeed(speeds[i]);
                }
            } catch (InvalidValueException invalidValueException) {
                value = null;
            }
        }
        if (value == null) {
            throw new InvalidDLNAProtocolAttributeException("Can't parse DLNA play speeds from: " + s);
        }
        setValue(value);
    }

    @Override
    public String getString() {
        StringBuilder sb = new StringBuilder();
        for (TransportPlaySpeed speed : getValue()) {
            if (speed.getValue().equals("1")) {
                continue;
            }
            sb.append(sb.length() == 0 ? "" : ",").append(speed);
        }
        return sb.toString();
    }
}
