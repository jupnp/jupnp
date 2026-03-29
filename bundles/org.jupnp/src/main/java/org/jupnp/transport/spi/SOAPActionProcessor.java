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
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.control.ActionRequestMessage;
import org.jupnp.model.message.control.ActionResponseMessage;

/**
 * Converts UPnP SOAP messages from/to action invocations.
 * <p>
 * The UPnP protocol layer processes local and remote {@link org.jupnp.model.action.ActionInvocation}
 * instances. The UPnP transport layer accepts and returns {@link org.jupnp.model.message.StreamRequestMessage}s
 * and {@link org.jupnp.model.message.StreamResponseMessage}s. This processor is an adapter between the
 * two layers, reading and writing SOAP content.
 * </p>
 *
 * @author Christian Bauer
 */
public interface SOAPActionProcessor {

    /**
     * Converts the given invocation input into SOAP XML content, setting on the given request message.
     *
     * @param requestMessage The request message on which the SOAP content is set.
     * @param actionInvocation The action invocation from which input argument values are read.
     * @throws org.jupnp.model.UnsupportedDataException
     */
    void writeBody(ActionRequestMessage requestMessage, ActionInvocation actionInvocation)
            throws UnsupportedDataException;

    /**
     * Converts the given invocation output into SOAP XML content, setting on the given response message.
     *
     * @param responseMessage The response message on which the SOAP content is set.
     * @param actionInvocation The action invocation from which output argument values are read.
     * @throws UnsupportedDataException
     */
    void writeBody(ActionResponseMessage responseMessage, ActionInvocation actionInvocation)
            throws UnsupportedDataException;

    /**
     * Converts SOAP XML content of the request message and sets input argument values on the given invocation.
     *
     * @param requestMessage The request message from which SOAP content is read.
     * @param actionInvocation The action invocation on which input argument values are set.
     * @throws UnsupportedDataException
     */
    void readBody(ActionRequestMessage requestMessage, ActionInvocation actionInvocation)
            throws UnsupportedDataException;

    /**
     * Converts SOAP XML content of the response message and sets output argument values on the given invocation.
     *
     * @param responseMsg The response message from which SOAP content is read.
     * @param actionInvocation The action invocation on which output argument values are set.
     * @throws UnsupportedDataException
     */
    void readBody(ActionResponseMessage responseMsg, ActionInvocation actionInvocation) throws UnsupportedDataException;
}
