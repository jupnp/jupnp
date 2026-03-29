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

import java.util.Map;

import org.jupnp.model.action.ActionArgumentValue;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class TransportInfo {

    private TransportState currentTransportState = TransportState.NO_MEDIA_PRESENT;
    private TransportStatus currentTransportStatus = TransportStatus.OK;
    private String currentSpeed = "1";

    public TransportInfo() {
    }

    public TransportInfo(Map<String, ActionArgumentValue<?>> args) {
        this(TransportState.valueOrCustomOf((String) args.get("CurrentTransportState").getValue()),
                TransportStatus.valueOrCustomOf((String) args.get("CurrentTransportStatus").getValue()),
                (String) args.get("CurrentSpeed").getValue());
    }

    public TransportInfo(TransportState currentTransportState) {
        this.currentTransportState = currentTransportState;
    }

    public TransportInfo(TransportState currentTransportState, String currentSpeed) {
        this.currentTransportState = currentTransportState;
        this.currentSpeed = currentSpeed;
    }

    public TransportInfo(TransportState currentTransportState, TransportStatus currentTransportStatus) {
        this.currentTransportState = currentTransportState;
        this.currentTransportStatus = currentTransportStatus;
    }

    public TransportInfo(TransportState currentTransportState, TransportStatus currentTransportStatus,
            String currentSpeed) {
        this.currentTransportState = currentTransportState;
        this.currentTransportStatus = currentTransportStatus;
        this.currentSpeed = currentSpeed;
    }

    public TransportState getCurrentTransportState() {
        return currentTransportState;
    }

    public TransportStatus getCurrentTransportStatus() {
        return currentTransportStatus;
    }

    public String getCurrentSpeed() {
        return currentSpeed;
    }
}
