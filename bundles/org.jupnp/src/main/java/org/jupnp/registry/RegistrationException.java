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
package org.jupnp.registry;

import java.util.List;

import org.jupnp.model.ValidationError;

/**
 * A recoverable error, thrown if device metadata could not be registered.
 *
 * @author Christian Bauer
 */
public class RegistrationException extends RuntimeException {

    public List<ValidationError> errors;

    public RegistrationException(String s) {
        super(s);
    }

    public RegistrationException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public RegistrationException(String s, List<ValidationError> errors) {
        super(s);
        this.errors = errors;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
