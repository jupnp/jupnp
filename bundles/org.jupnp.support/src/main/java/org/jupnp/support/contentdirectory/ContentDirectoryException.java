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
package org.jupnp.support.contentdirectory;

import org.jupnp.model.action.ActionException;
import org.jupnp.model.types.ErrorCode;

/**
 * @author Alessio Gaeta
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class ContentDirectoryException extends ActionException {

    private static final long serialVersionUID = 4098130203774699299L;

    public ContentDirectoryException(int errorCode, String message) {
        super(errorCode, message);
    }

    public ContentDirectoryException(int errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public ContentDirectoryException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ContentDirectoryException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ContentDirectoryException(ContentDirectoryErrorCode errorCode, String message) {
        super(errorCode.getCode(), errorCode.getDescription() + ". " + message + ".");
    }

    public ContentDirectoryException(ContentDirectoryErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getDescription());
    }
}
