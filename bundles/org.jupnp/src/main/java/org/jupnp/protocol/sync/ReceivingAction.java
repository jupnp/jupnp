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
package org.jupnp.protocol.sync;

import org.jupnp.UpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.action.ActionCancelledException;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.RemoteActionInvocation;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.control.IncomingActionRequestMessage;
import org.jupnp.model.message.control.OutgoingActionResponseMessage;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.resource.ServiceControlResource;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.transport.RouterException;
import org.jupnp.util.Exceptions;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles reception of control messages, invoking actions on local services.
 * <p>
 * Actions are invoked through the {@link org.jupnp.model.action.ActionExecutor} returned
 * by the registered {@link org.jupnp.model.meta.LocalService#getExecutor(org.jupnp.model.meta.Action)}
 * method.
 * </p>
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public class ReceivingAction extends ReceivingSync<StreamRequestMessage, StreamResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(ReceivingAction.class);

    public ReceivingAction(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    @Override
    protected StreamResponseMessage executeSync() throws RouterException {

        ContentTypeHeader contentTypeHeader = getInputMessage().getHeaders()
                .getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class);

        // Special rules for action messages! UDA 1.0 says:
        // 'If the CONTENT-TYPE header specifies an unsupported value (other then "text/xml") the
        // device must return an HTTP status code "415 Unsupported Media Type".'
        if (contentTypeHeader != null && !contentTypeHeader.isUDACompliantXML()) {
            SpecificationViolationReporter.report("Received invalid Content-Type '{}': {}", contentTypeHeader,
                    getInputMessage());
            return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.UNSUPPORTED_MEDIA_TYPE));
        }

        if (contentTypeHeader == null) {
            SpecificationViolationReporter.report("Received without Content-Type: {}", getInputMessage());
        }

        ServiceControlResource resource = getUpnpService().getRegistry().getResource(ServiceControlResource.class,
                getInputMessage().getUri());

        if (resource == null) {
            logger.trace("No local resource found: {}", getInputMessage());
            return null;
        }

        logger.trace("Found local action resource matching relative request URI: {}", getInputMessage().getUri());

        RemoteActionInvocation invocation;
        OutgoingActionResponseMessage responseMessage;

        try {

            // Throws ActionException if the action can't be found
            IncomingActionRequestMessage requestMessage = new IncomingActionRequestMessage(getInputMessage(),
                    resource.getModel());

            logger.trace("Created incoming action request message: {}", requestMessage);
            invocation = new RemoteActionInvocation(requestMessage.getAction(), getRemoteClientInfo());

            // Throws UnsupportedDataException if the body can't be read
            logger.trace("Reading body of request message");
            getUpnpService().getConfiguration().getSoapActionProcessor().readBody(requestMessage, invocation);

            logger.trace("Executing on local service: {}", invocation);
            resource.getModel().getExecutor(invocation.getAction()).execute(invocation);

            if (invocation.getFailure() == null) {
                responseMessage = new OutgoingActionResponseMessage(invocation.getAction());
            } else {

                if (invocation.getFailure() instanceof ActionCancelledException) {
                    logger.trace("Action execution was cancelled, returning 404 to client");
                    // A 404 status is appropriate for this situation: The resource is gone/not available and it's
                    // a temporary condition. Most likely the cancellation happened because the client connection
                    // has been dropped, so it doesn't really matter what we return here anyway.
                    return null;
                } else {
                    responseMessage = new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR,
                            invocation.getAction());
                }
            }

        } catch (ActionException e) {
            logger.trace("Error executing local action", e);

            invocation = new RemoteActionInvocation(e, getRemoteClientInfo());
            responseMessage = new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);

        } catch (UnsupportedDataException e) {
            logger.warn("Error reading action request XML body", e);

            invocation = new RemoteActionInvocation(
                    Exceptions.unwrap(e) instanceof ActionException ? (ActionException) Exceptions.unwrap(e)
                            : new ActionException(ErrorCode.ACTION_FAILED, e.getMessage()),
                    getRemoteClientInfo());
            responseMessage = new OutgoingActionResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);

        }

        try {

            logger.trace("Writing body of response message");
            getUpnpService().getConfiguration().getSoapActionProcessor().writeBody(responseMessage, invocation);

            logger.trace("Returning finished response message: {}", responseMessage);
            return responseMessage;

        } catch (UnsupportedDataException e) {
            logger.warn("Failure writing body of response message, sending '500 Internal Server Error' without body",
                    e);
            return new StreamResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
