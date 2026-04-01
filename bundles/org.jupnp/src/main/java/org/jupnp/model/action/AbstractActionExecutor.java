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

import java.util.HashMap;
import java.util.Map;

import org.jupnp.model.Command;
import org.jupnp.model.ServiceManager;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared procedures for action executors based on an actual service implementation instance.
 *
 * @author Christian Bauer
 */
public abstract class AbstractActionExecutor implements ActionExecutor {

    private final Logger logger = LoggerFactory.getLogger(AbstractActionExecutor.class);

    protected Map<ActionArgument<LocalService>, StateVariableAccessor> outputArgumentAccessors = new HashMap<>();

    protected AbstractActionExecutor() {
    }

    protected AbstractActionExecutor(Map<ActionArgument<LocalService>, StateVariableAccessor> outputArgumentAccessors) {
        this.outputArgumentAccessors = outputArgumentAccessors;
    }

    public Map<ActionArgument<LocalService>, StateVariableAccessor> getOutputArgumentAccessors() {
        return outputArgumentAccessors;
    }

    /**
     * Obtains the service implementation instance from the {@link org.jupnp.model.ServiceManager}, handles exceptions.
     */
    @Override
    public void execute(final ActionInvocation<LocalService> actionInvocation) {

        logger.trace("Invoking on local service: {}", actionInvocation);

        final LocalService service = actionInvocation.getAction().getService();

        try {

            if (service.getManager() == null) {
                throw new IllegalStateException("Service has no implementation factory, can't get service instance");
            }

            service.getManager().execute(new Command() {
                @Override
                public void execute(ServiceManager serviceManager) throws Exception {
                    AbstractActionExecutor.this.execute(actionInvocation, serviceManager.getImplementation());
                }

                @Override
                public String toString() {
                    return "Action invocation: " + actionInvocation.getAction();
                }
            });

        } catch (ActionException e) {
            logger.trace("ActionException thrown by service, wrapping in invocation and returning", e);
            actionInvocation.setFailure(e);
        } catch (InterruptedException e) {
            logger.trace("InterruptedException thrown by service, wrapping in invocation and returning", e);
            actionInvocation.setFailure(new ActionCancelledException(e));
        } catch (Exception e) {
            Throwable rootCause = Exceptions.unwrap(e);
            logger.trace("Execution has thrown, wrapping root cause in ActionException and returning", e);
            actionInvocation.setFailure(new ActionException(ErrorCode.ACTION_FAILED,
                    rootCause.getMessage() != null ? rootCause.getMessage() : rootCause.toString(), rootCause));
        }
    }

    protected abstract void execute(ActionInvocation<LocalService> actionInvocation, Object serviceImpl)
            throws Exception;

    /**
     * Reads the output arguments after an action execution using accessors.
     *
     * @param action The action of which the output arguments are read.
     * @param instance The instance on which the accessors will be invoked.
     * @return <code>null</code> if the action has no output arguments, a single instance if it has one, an
     *         <code>Object[]</code> otherwise.
     * @throws Exception
     */
    protected Object readOutputArgumentValues(Action<LocalService> action, Object instance) throws Exception {
        Object[] results = new Object[action.getOutputArguments().length];
        logger.trace("Attempting to retrieve output argument values using accessor: {}", results.length);

        int i = 0;
        for (ActionArgument outputArgument : action.getOutputArguments()) {
            logger.trace("Calling acccessor method for: {}", outputArgument);

            StateVariableAccessor accessor = getOutputArgumentAccessors().get(outputArgument);
            if (accessor != null) {
                logger.trace("Calling accessor to read output argument value: {}", accessor);
                results[i++] = accessor.read(instance);
            } else {
                throw new IllegalStateException("No accessor bound for: " + outputArgument);
            }
        }

        if (results.length == 1) {
            return results[0];
        }
        return results.length > 0 ? results : null;
    }

    /**
     * Sets the output argument value on the {@link org.jupnp.model.action.ActionInvocation}, considers string
     * conversion.
     */
    protected void setOutputArgumentValue(ActionInvocation<LocalService> actionInvocation,
            ActionArgument<LocalService> argument, Object result) throws ActionException {

        LocalService service = actionInvocation.getAction().getService();

        if (result != null) {
            try {
                if (service.isStringConvertibleType(result)) {
                    logger.trace(
                            "Result of invocation matches convertible type, setting toString() single output argument value");
                    actionInvocation.setOutput(new ActionArgumentValue<>(argument, result.toString()));
                } else {
                    logger.trace("Result of invocation is Object, setting single output argument value");
                    actionInvocation.setOutput(new ActionArgumentValue<>(argument, result));
                }
            } catch (InvalidValueException e) {
                throw new ActionException(ErrorCode.ARGUMENT_VALUE_INVALID,
                        "Wrong type or invalid value for '" + argument.getName() + "': " + e.getMessage(), e);
            }
        } else {

            logger.trace("Result of invocation is null, not setting any output argument value(s)");
        }
    }
}
