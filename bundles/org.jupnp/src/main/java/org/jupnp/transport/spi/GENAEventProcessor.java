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
package org.jupnp.transport.spi;

import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.message.gena.IncomingEventRequestMessage;
import org.jupnp.model.message.gena.OutgoingEventRequestMessage;

/**
 * Reads and writes GENA XML content.
 *
 * @author Christian Bauer
 */
public interface GENAEventProcessor {

    /**
     * Transforms a collection of {@link org.jupnp.model.state.StateVariableValue}s into an XML message body.
     *
     * @param requestMessage The message to transform.
     * @throws org.jupnp.model.UnsupportedDataException
     */
    void writeBody(OutgoingEventRequestMessage requestMessage) throws UnsupportedDataException;

    /**
     * Transforms an XML message body and adds to a collection of {@link org.jupnp.model.state.StateVariableValue}s.
     *
     * @param requestMessage The message to transform.
     * @throws UnsupportedDataException
     */
    void readBody(IncomingEventRequestMessage requestMessage) throws UnsupportedDataException;
}
