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
package org.jupnp.util.statemachine;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class StateMachineInvocationHandler implements InvocationHandler {

    private final Logger logger = LoggerFactory.getLogger(StateMachineInvocationHandler.class);

    public static final String METHOD_ON_ENTRY = "onEntry";
    public static final String METHOD_ON_EXIT = "onExit";

    final Class<?> initialStateClass;
    final Map<Class<?>, Object> stateObjects = new ConcurrentHashMap<>();
    Object currentState;

    StateMachineInvocationHandler(List<Class<?>> stateClasses, Class<?> initialStateClass,
            Class<?>[] constructorArgumentTypes, Object[] constructorArguments) {

        logger.debug("Creating state machine with initial state: {}", initialStateClass);

        this.initialStateClass = initialStateClass;

        for (Class<?> stateClass : stateClasses) {
            try {

                Object state = constructorArgumentTypes != null
                        ? stateClass.getConstructor(constructorArgumentTypes).newInstance(constructorArguments)
                        : stateClass.getDeclaredConstructor().newInstance();

                logger.debug("Adding state instance: {}", state.getClass().getName());
                stateObjects.put(stateClass, state);

            } catch (NoSuchMethodException e) {
                throw new RuntimeException("State " + stateClass.getName() + " has the wrong constructor", e);
            } catch (Exception e) {
                throw new RuntimeException("State " + stateClass.getName() + " can't be instantiated", e);
            }
        }

        if (!stateObjects.containsKey(initialStateClass)) {
            throw new RuntimeException("Initial state not in list of states: " + initialStateClass);
        }

        currentState = stateObjects.get(initialStateClass);
        synchronized (this) {
            invokeEntryMethod(currentState);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        synchronized (this) {

            if (StateMachine.METHOD_CURRENT_STATE.equals(method.getName()) && method.getParameterTypes().length == 0) {
                return currentState;
            }

            if (StateMachine.METHOD_FORCE_STATE.equals(method.getName()) && method.getParameterTypes().length == 1
                    && args.length == 1 && args[0] != null && args[0] instanceof Class) {
                Object forcedState = stateObjects.get((Class<?>) args[0]);
                if (forcedState == null) {
                    throw new TransitionException("Can't force to invalid state: " + args[0]);
                }
                logger.debug("Forcing state machine into state: {}", forcedState.getClass().getName());
                invokeExitMethod(currentState);
                currentState = forcedState;
                invokeEntryMethod(forcedState);
                return null;
            }

            Method signalMethod = getMethodOfCurrentState(method);
            logger.debug("Invoking signal method of current state: {}", signalMethod);
            Object methodReturn = signalMethod.invoke(currentState, args);

            if (methodReturn != null && methodReturn instanceof Class) {
                Class<?> nextStateClass = (Class<?>) methodReturn;
                if (stateObjects.containsKey(nextStateClass)) {
                    logger.debug("Executing transition to next state: {}", nextStateClass.getName());
                    invokeExitMethod(currentState);
                    currentState = stateObjects.get(nextStateClass);
                    invokeEntryMethod(currentState);
                }
            }
            return methodReturn;
        }
    }

    private Method getMethodOfCurrentState(Method method) {
        try {
            return currentState.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new TransitionException("State '" + currentState.getClass().getName() + "' doesn't support signal '"
                    + method.getName() + "'");
        }
    }

    private void invokeEntryMethod(Object state) {
        logger.debug("Trying to invoke entry method of state: {}", state.getClass().getName());
        try {
            Method onEntryMethod = state.getClass().getMethod(METHOD_ON_ENTRY);
            onEntryMethod.invoke(state);
        } catch (NoSuchMethodException e) {
            logger.debug("No entry method found on state: {}", state.getClass().getName());
            // That's OK, just don't call it
        } catch (Exception e) {
            throw new TransitionException("State '" + state.getClass().getName() + "' entry method threw exception", e);
        }
    }

    private void invokeExitMethod(Object state) {
        logger.debug("Trying to invoking exit method of state: {}", state.getClass().getName());
        try {
            Method onExitMethod = state.getClass().getMethod(METHOD_ON_EXIT);
            onExitMethod.invoke(state);
        } catch (NoSuchMethodException e) {
            logger.debug("No exit method found on state: {}", state.getClass().getName());
            // That's OK, just don't call it
        } catch (Exception e) {
            throw new TransitionException("State '" + state.getClass().getName() + "' exit method threw exception", e);
        }
    }
}
