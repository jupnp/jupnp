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
package org.jupnp.model.action;

import org.jupnp.model.types.ErrorCode;

/**
 * Thrown (or encapsulated in {@link org.jupnp.model.action.ActionInvocation}) when an action execution failed.
 *
 * @author Christian Bauer
 */
public class ActionException extends Exception {

    private int errorCode;

    public ActionException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ActionException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ActionException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getDescription());
    }

    public ActionException(ErrorCode errorCode, String message) {
        this(errorCode, message, true);
    }

    public ActionException(ErrorCode errorCode, String message, boolean concatMessages) {
        this(errorCode.getCode(), concatMessages ? errorCode.getDescription() + ". " + message + "." : message);
    }

    public ActionException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode.getCode(), errorCode.getDescription() + ". " + message + ".", cause);
    }

    public int getErrorCode() {
        return errorCode;
    }
}
