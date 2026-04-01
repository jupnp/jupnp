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

import java.net.URL;

import org.jupnp.UpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.action.ActionCancelledException;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.control.IncomingActionResponseMessage;
import org.jupnp.model.message.control.OutgoingActionRequestMessage;
import org.jupnp.model.meta.Device;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.protocol.SendingSync;
import org.jupnp.transport.RouterException;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sending control message, transforming a local {@link org.jupnp.model.action.ActionInvocation}.
 * <p>
 * Writes the outgoing message's body with the {@link org.jupnp.transport.spi.SOAPActionProcessor}.
 * This protocol will return <code>null</code> if no response was received from the control target host.
 * In all other cases, even if only the processing of message content failed, this protocol will
 * return an {@link org.jupnp.model.message.control.IncomingActionResponseMessage}. Any error
 * details of a failed response ({@link org.jupnp.model.message.UpnpResponse#isFailed()}) are
 * available with
 * {@link org.jupnp.model.action.ActionInvocation#setFailure(org.jupnp.model.action.ActionException)}.
 * </p>
 *
 * @author Christian Bauer
 */
public class SendingAction extends SendingSync<OutgoingActionRequestMessage, IncomingActionResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(SendingAction.class);

    protected final ActionInvocation actionInvocation;

    public SendingAction(UpnpService upnpService, ActionInvocation actionInvocation, URL controlURL) {
        super(upnpService, new OutgoingActionRequestMessage(actionInvocation, controlURL));
        this.actionInvocation = actionInvocation;
    }

    @Override
    protected IncomingActionResponseMessage executeSync() throws RouterException {
        return invokeRemote(getInputMessage());
    }

    protected IncomingActionResponseMessage invokeRemote(OutgoingActionRequestMessage requestMessage)
            throws RouterException {
        Device device = actionInvocation.getAction().getService().getDevice();

        logger.trace("Sending outgoing action call '{}' to remote service of: {}",
                actionInvocation.getAction().getName(), device);
        IncomingActionResponseMessage responseMessage = null;
        try {

            StreamResponseMessage streamResponse = sendRemoteRequest(requestMessage);

            if (streamResponse == null) {
                logger.trace("No connection or no no response received, returning null");
                actionInvocation.setFailure(
                        new ActionException(ErrorCode.ACTION_FAILED, "Connection error or no response received"));
                return null;
            }

            responseMessage = new IncomingActionResponseMessage(streamResponse);

            if (responseMessage.isFailedNonRecoverable()) {
                logger.trace("Response was a non-recoverable failure: {}", responseMessage);
                throw new ActionException(ErrorCode.ACTION_FAILED, "Non-recoverable remote execution failure: "
                        + responseMessage.getOperation().getResponseDetails());
            } else if (responseMessage.isFailedRecoverable()) {
                handleResponseFailure(responseMessage);
            } else {
                handleResponse(responseMessage);
            }

            return responseMessage;

        } catch (ActionException e) {
            logger.trace("Remote action invocation failed, returning Internal Server Error message", e);
            actionInvocation.setFailure(e);
            if (responseMessage == null || !responseMessage.getOperation().isFailed()) {
                return new IncomingActionResponseMessage(new UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR));
            } else {
                return responseMessage;
            }
        }
    }

    protected StreamResponseMessage sendRemoteRequest(OutgoingActionRequestMessage requestMessage)
            throws ActionException, RouterException {

        try {
            logger.trace("Writing SOAP request body of: {}", requestMessage);
            getUpnpService().getConfiguration().getSoapActionProcessor().writeBody(requestMessage, actionInvocation);

            logger.trace("Sending SOAP body of message as stream to remote device");
            return getUpnpService().getRouter().send(requestMessage);
        } catch (RouterException e) {
            Throwable cause = Exceptions.unwrap(e);
            if (cause instanceof InterruptedException) {
                logger.trace("Sending action request message was interrupted", e);
                throw new ActionCancelledException((InterruptedException) cause);
            }
            throw e;
        } catch (UnsupportedDataException e) {
            logger.trace("Error writing SOAP body", e);
            throw new ActionException(ErrorCode.ACTION_FAILED, "Error writing request message. " + e.getMessage());
        }
    }

    protected void handleResponse(IncomingActionResponseMessage responseMsg) throws ActionException {

        try {
            logger.trace("Received response for outgoing call, reading SOAP response body: {}", responseMsg);
            getUpnpService().getConfiguration().getSoapActionProcessor().readBody(responseMsg, actionInvocation);
        } catch (UnsupportedDataException e) {
            logger.trace("Error reading SOAP body", e);
            throw new ActionException(ErrorCode.ACTION_FAILED, "Error reading SOAP response message. " + e.getMessage(),
                    false);
        }
    }

    protected void handleResponseFailure(IncomingActionResponseMessage responseMsg) throws ActionException {

        try {
            logger.trace("Received response with Internal Server Error, reading SOAP failure message");
            getUpnpService().getConfiguration().getSoapActionProcessor().readBody(responseMsg, actionInvocation);
        } catch (UnsupportedDataException e) {
            logger.trace("Error reading SOAP body", e);
            throw new ActionException(ErrorCode.ACTION_FAILED,
                    "Error reading SOAP response failure message. " + e.getMessage(), false);
        }
    }

    /* @formatter:off
     * - send request
     *    - UnsupportedDataException: Can't write body
     *
     * - streamResponseMessage is null: No response received, return null to client
     *
     * - streamResponseMessage >= 300 && !(405 || 500): Response was HTTP failure, set on anemic response and return
     *
     * - streamResponseMessage >= 300 && 405: Try request again with different headers
     *    - UnsupportedDataException: Can't write body
     *    - (The whole streamResponse conditions apply again but this time, ignore 405)
     *
     * - streamResponseMessage >= 300 && 500 && lastExecutionFailure != null: Try to read SOAP failure body
     *    - UnsupportedDataException: Can't read body
     *
     * - streamResponseMessage < 300: Response was OK, try to read response body
     *    - UnsupportedDataException: Can't read body
     * @formatter:on
     */
}
