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
package org.jupnp.support.lastchange;

import java.net.URI;
import java.util.Map;

import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.InvalidValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class EventedValueURI extends EventedValue<URI> {

    private final Logger logger = LoggerFactory.getLogger(EventedValueURI.class);

    public EventedValueURI(URI value) {
        super(value);
    }

    public EventedValueURI(Map.Entry<String, String>[] attributes) {
        super(attributes);
    }

    @Override
    protected URI valueOf(String s) {
        try {
            // These URIs are really defined as 'string' datatype in AVTransport1.0.pdf, but we can try
            // to parse whatever devices give us, like the Roku which sends "unknown url".
            return super.valueOf(s);
        } catch (InvalidValueException e) {
            logger.debug("Ignoring invalid URI in evented value '{}'", s, e);
            return null;
        }
    }

    @Override
    protected Datatype<?> getDatatype() {
        return Datatype.Builtin.URI.getDatatype();
    }
}
