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
package org.jupnp.controlpoint;

import java.net.URL;

import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.control.IncomingActionResponseMessage;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.protocol.sync.SendingAction;

/**
 * Execute actions on any service.
 * <p>
 * Usage example for asynchronous execution in a background thread:
 * </p>
 * 
 * <pre>
 * Service service = device.findService(new UDAServiceId("SwitchPower"));
 * Action getStatusAction = service.getAction("GetStatus");
 * ActionInvocation getStatusInvocation = new ActionInvocation(getStatusAction);
 *
 * ActionCallback getStatusCallback = new ActionCallback(getStatusInvocation) {
 *
 *      public void success(ActionInvocation invocation) {
 *          ActionArgumentValue status  = invocation.getOutput("ResultStatus");
 *          assertEquals((Boolean) status.getValue(), Boolean.valueOf(false));
 *      }
 *
 *      public void failure(ActionInvocation invocation, UpnpResponse res) {
 *          System.err.println(
 *              createDefaultFailureMessage(invocation, res)
 *          );
 *      }
 * };
 *
 * upnpService.getControlPoint().execute(getStatusCallback)
 * </pre>
 * <p>
 * You can also execute the action synchronously in the same thread using the
 * {@link org.jupnp.controlpoint.ActionCallback.Default} implementation:
 * </p>
 * 
 * <pre>
 * myActionInvocation.setInput("foo", bar);
 * new ActionCallback.Default(myActionInvocation, upnpService.getControlPoint()).run();
 * myActionInvocation.getOutput("baz");
 * </pre>
 *
 * @author Christian Bauer
 */
public abstract class ActionCallback implements Runnable {

    /**
     * Empty implementation of callback methods, simplifies synchronous
     * execution of an {@link org.jupnp.model.action.ActionInvocation}.
     */
    public static final class Default extends ActionCallback {

        public Default(ActionInvocation actionInvocation, ControlPoint controlPoint) {
            super(actionInvocation, controlPoint);
        }

        @Override
        public void success(ActionInvocation invocation) {
        }

        @Override
        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
        }
    }

    protected final ActionInvocation actionInvocation;

    protected ControlPoint controlPoint;

    protected ActionCallback(ActionInvocation actionInvocation, ControlPoint controlPoint) {
        this.actionInvocation = actionInvocation;
        this.controlPoint = controlPoint;
    }

    protected ActionCallback(ActionInvocation actionInvocation) {
        this.actionInvocation = actionInvocation;
    }

    public ActionInvocation getActionInvocation() {
        return actionInvocation;
    }

    public synchronized ControlPoint getControlPoint() {
        return controlPoint;
    }

    public synchronized ActionCallback setControlPoint(ControlPoint controlPoint) {
        this.controlPoint = controlPoint;
        return this;
    }

    @Override
    public void run() {
        Service service = actionInvocation.getAction().getService();

        // Local execution
        if (service instanceof LocalService) {
            LocalService localService = (LocalService) service;

            // Executor validates input inside the execute() call immediately
            localService.getExecutor(actionInvocation.getAction()).execute(actionInvocation);

            if (actionInvocation.getFailure() != null) {
                failure(actionInvocation, null);
            } else {
                success(actionInvocation);
            }

            // Remote execution
        } else if (service instanceof RemoteService) {

            if (getControlPoint() == null) {
                throw new IllegalStateException("Callback must be executed through ControlPoint");
            }

            RemoteService remoteService = (RemoteService) service;

            // Figure out the remote URL where we'd like to send the action request to
            URL controLURL;
            try {
                controLURL = remoteService.getDevice().normalizeURI(remoteService.getControlURI());
            } catch (IllegalArgumentException e) {
                failure(actionInvocation, null, "bad control URL: " + remoteService.getControlURI());
                return;
            }

            // Do it
            SendingAction prot = getControlPoint().getProtocolFactory().createSendingAction(actionInvocation,
                    controLURL);
            prot.run();

            IncomingActionResponseMessage response = prot.getOutputMessage();

            if (response == null) {
                failure(actionInvocation, null);
            } else if (response.getOperation().isFailed()) {
                failure(actionInvocation, response.getOperation());
            } else {
                success(actionInvocation);
            }
        }
    }

    protected String createDefaultFailureMessage(ActionInvocation invocation, UpnpResponse operation) {
        String message = "Error: ";
        final ActionException exception = invocation.getFailure();
        if (exception != null) {
            message = message + exception.getMessage();
        }
        if (operation != null) {
            message = message + " (HTTP response was: " + operation.getResponseDetails() + ")";
        }
        return message;
    }

    protected void failure(ActionInvocation invocation, UpnpResponse operation) {
        failure(invocation, operation, createDefaultFailureMessage(invocation, operation));
    }

    /**
     * Called when the action invocation succeeded.
     *
     * @param invocation The successful invocation, call its <code>getOutput()</code> method for results.
     */
    public abstract void success(ActionInvocation invocation);

    /**
     * Called when the action invocation failed.
     *
     * @param invocation The failed invocation, call its <code>getFailure()</code> method for more details.
     * @param operation If the invocation was on a remote service, the response message, otherwise null.
     * @param defaultMsg A user-friendly error message generated from the invocation exception and response.
     * @see #createDefaultFailureMessage
     */
    public abstract void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg);

    @Override
    public String toString() {
        return "(ActionCallback) " + actionInvocation;
    }
}
