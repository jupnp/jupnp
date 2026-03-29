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
package org.jupnp.support.connectionmanager;

import org.jupnp.model.action.ActionException;
import org.jupnp.model.types.ErrorCode;

/**
 *
 */
public class ConnectionManagerException extends ActionException {

    private static final long serialVersionUID = 4656849940513543654L;

    public ConnectionManagerException(int errorCode, String message) {
        super(errorCode, message);
    }

    public ConnectionManagerException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ConnectionManagerException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ConnectionManagerException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ConnectionManagerException(ConnectionManagerErrorCode errorCode, String message) {
        super(errorCode.getCode(), errorCode.getDescription() + ". " + message + ".");
    }

    public ConnectionManagerException(ConnectionManagerErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDescription());
    }
}
