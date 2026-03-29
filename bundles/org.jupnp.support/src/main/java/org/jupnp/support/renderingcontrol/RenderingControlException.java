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
package org.jupnp.support.renderingcontrol;

import org.jupnp.model.action.ActionException;
import org.jupnp.model.types.ErrorCode;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class RenderingControlException extends ActionException {

    private static final long serialVersionUID = 7082222250964828818L;

    public RenderingControlException(int errorCode, String message) {
        super(errorCode, message);
    }

    public RenderingControlException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public RenderingControlException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RenderingControlException(ErrorCode errorCode) {
        super(errorCode);
    }

    public RenderingControlException(RenderingControlErrorCode errorCode, String message) {
        super(errorCode.getCode(), errorCode.getDescription() + ". " + message + ".");
    }

    public RenderingControlException(RenderingControlErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDescription());
    }
}
