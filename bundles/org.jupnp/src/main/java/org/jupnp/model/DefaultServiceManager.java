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
package org.jupnp.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.jupnp.internal.compat.java.beans.PropertyChangeEvent;
import org.jupnp.internal.compat.java.beans.PropertyChangeListener;
import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.util.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation, creates and manages a single instance of a plain Java bean.
 * <p>
 * Creates instance of the defined service class when it is first needed (acts as a factory),
 * manages the instance in a field (it's shared), and synchronizes (locks) all
 * multi-threaded access. A locking attempt will timeout after 500 milliseconds with
 * a runtime exception if another operation is already in progress. Override
 * {@link #getLockTimeoutMillis()} to customize this behavior, e.g. if your service
 * bean is slow and requires more time for typical action executions or state
 * variable reading.
 * </p>
 *
 * @author Christian Bauer
 * @author Jochen Hiller - Changed to use Compact2 compliant Java Beans
 */
public class DefaultServiceManager<T> implements ServiceManager<T> {

    private final Logger logger = LoggerFactory.getLogger(DefaultServiceManager.class);

    protected final LocalService<T> service;
    protected final Class<T> serviceClass;
    protected final ReentrantLock lock = new ReentrantLock(true);

    // Locking!
    protected T serviceImpl;
    protected PropertyChangeSupport propertyChangeSupport;

    protected DefaultServiceManager(LocalService<T> service) {
        this(service, null);
    }

    public DefaultServiceManager(LocalService<T> service, Class<T> serviceClass) {
        this.service = service;
        this.serviceClass = serviceClass;
    }

    // The monitor entry and exit methods

    protected void lock() {
        try {
            if (lock.tryLock(getLockTimeoutMillis(), TimeUnit.MILLISECONDS)) {
                logger.trace("Acquired lock");
            } else {
                throw new RuntimeException("Failed to acquire lock in milliseconds: " + getLockTimeoutMillis());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to acquire lock:" + e);
        }
    }

    protected void unlock() {
        logger.trace("Releasing lock");
        lock.unlock();
    }

    protected int getLockTimeoutMillis() {
        return 500;
    }

    @Override
    public LocalService<T> getService() {
        return service;
    }

    @Override
    public T getImplementation() {
        lock();
        try {
            if (serviceImpl == null) {
                init();
            }
            return serviceImpl;
        } finally {
            unlock();
        }
    }

    @Override
    public PropertyChangeSupport getPropertyChangeSupport() {
        lock();
        try {
            if (propertyChangeSupport == null) {
                init();
            }
            return propertyChangeSupport;
        } finally {
            unlock();
        }
    }

    @Override
    public void execute(Command<T> cmd) throws Exception {
        lock();
        try {
            cmd.execute(this);
        } finally {
            unlock();
        }
    }

    @Override
    public Collection<StateVariableValue> getCurrentState() throws Exception {
        lock();
        try {
            Collection<StateVariableValue> values = readInitialEventedStateVariableValues();
            if (values != null) {
                logger.trace(
                        "Obtained initial state variable values for event, skipping individual state variable accessors");
                return values;
            }
            values = new ArrayList<>();
            for (StateVariable stateVariable : getService().getStateVariables()) {
                if (stateVariable.getEventDetails().isSendEvents()) {
                    StateVariableAccessor accessor = getService().getAccessor(stateVariable);
                    if (accessor == null) {
                        throw new IllegalStateException("No accessor for evented state variable");
                    }
                    values.add(accessor.read(stateVariable, getImplementation()));
                }
            }
            return values;
        } finally {
            unlock();
        }
    }

    protected Collection<StateVariableValue> getCurrentState(String[] variableNames) throws Exception {
        lock();
        try {
            Collection<StateVariableValue> values = new ArrayList<>();
            for (String variableName : variableNames) {
                variableName = variableName.trim();

                StateVariable<LocalService> stateVariable = getService().getStateVariable(variableName);
                if (stateVariable == null || !stateVariable.getEventDetails().isSendEvents()) {
                    logger.trace("Ignoring unknown or non-evented state variable: {}", variableName);
                    continue;
                }

                StateVariableAccessor accessor = getService().getAccessor(stateVariable);
                if (accessor == null) {
                    logger.warn("Ignoring evented state variable without accessor: {}", variableName);
                    continue;
                }
                values.add(accessor.read(stateVariable, getImplementation()));
            }
            return values;
        } finally {
            unlock();
        }
    }

    protected void init() {
        logger.trace("No service implementation instance available, initializing...");
        try {
            // The actual instance we ware going to use and hold a reference to (1:1 instance for manager)
            serviceImpl = createServiceInstance();

            // How the implementation instance will tell us about property changes
            propertyChangeSupport = createPropertyChangeSupport(serviceImpl);
            propertyChangeSupport.addPropertyChangeListener(createPropertyChangeListener(serviceImpl));

        } catch (Exception e) {
            throw new RuntimeException("Could not initialize implementation", e);
        }
    }

    protected T createServiceInstance() throws Exception {
        if (serviceClass == null) {
            throw new IllegalStateException(
                    "Subclass has to provide service class or override createServiceInstance()");
        }
        try {
            // Use this constructor if possible
            return serviceClass.getConstructor(LocalService.class).newInstance(getService());
        } catch (NoSuchMethodException e) {
            logger.trace("Creating new service implementation instance with no-arg constructor: {}",
                    serviceClass.getName());
            return serviceClass.getDeclaredConstructor().newInstance();
        }
    }

    protected PropertyChangeSupport createPropertyChangeSupport(T serviceImpl) throws Exception {
        Method m;
        if ((m = Reflections.getGetterMethod(serviceImpl.getClass(), "propertyChangeSupport")) != null
                && PropertyChangeSupport.class.isAssignableFrom(m.getReturnType())) {
            logger.trace("Service implementation instance offers PropertyChangeSupport, using that: {}",
                    serviceImpl.getClass().getName());
            return (PropertyChangeSupport) m.invoke(serviceImpl);
        }
        logger.trace("Creating new PropertyChangeSupport for service implementation: {}",
                serviceImpl.getClass().getName());
        return new PropertyChangeSupport(serviceImpl);
    }

    protected PropertyChangeListener createPropertyChangeListener(T serviceImpl) throws Exception {
        return new DefaultPropertyChangeListener();
    }

    protected Collection<StateVariableValue> readInitialEventedStateVariableValues() throws Exception {
        return null;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") Implementation: " + serviceImpl;
    }

    protected class DefaultPropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent pce) {
            logger.trace("Property change event on local service: {}", pce.getPropertyName());

            // Prevent recursion
            if (pce.getPropertyName().equals(EVENTED_STATE_VARIABLES)) {
                return;
            }

            String[] variableNames = ModelUtil.fromCommaSeparatedList(pce.getPropertyName());
            if (logger.isTraceEnabled()) {
                logger.trace("Changed variable names: {}", Arrays.toString(variableNames));
            }

            try {
                Collection<StateVariableValue> currentValues = getCurrentState(variableNames);

                if (!currentValues.isEmpty()) {
                    getPropertyChangeSupport().firePropertyChange(EVENTED_STATE_VARIABLES, null, currentValues);
                }

            } catch (Exception e) {
                // TODO: Is it OK to only log this error? It means we keep running although we couldn't send events?
                logger.error("Error reading state of service after state variable update event", e);
            }
        }
    }
}
