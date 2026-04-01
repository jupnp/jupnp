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
package org.jupnp.model.meta;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.ValidationError;
import org.jupnp.model.ValidationException;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;

/**
 * The metadata of a service discovered on a remote device.
 * <p>
 * Includes the URI's for getting the service's descriptor, calling its
 * actions, and subscribing to events.
 * </p>
 * 
 * @author Christian Bauer
 */
public class RemoteService extends Service<RemoteDevice, RemoteService> {

    private final URI descriptorURI;
    private final URI controlURI;
    private final URI eventSubscriptionURI;

    public RemoteService(ServiceType serviceType, ServiceId serviceId, URI descriptorURI, URI controlURI,
            URI eventSubscriptionURI) throws ValidationException {
        this(serviceType, serviceId, descriptorURI, controlURI, eventSubscriptionURI, null, null);
    }

    public RemoteService(ServiceType serviceType, ServiceId serviceId, URI descriptorURI, URI controlURI,
            URI eventSubscriptionURI, Action<RemoteService>[] actions, StateVariable<RemoteService>[] stateVariables)
            throws ValidationException {
        super(serviceType, serviceId, actions, stateVariables);

        this.descriptorURI = descriptorURI;
        this.controlURI = controlURI;
        this.eventSubscriptionURI = eventSubscriptionURI;

        List<ValidationError> errors = validateThis();
        if (!errors.isEmpty()) {
            throw new ValidationException("Validation of device graph failed, call getErrors() on exception", errors);
        }
    }

    @Override
    public Action getQueryStateVariableAction() {
        return new QueryStateVariableAction(this);
    }

    public URI getDescriptorURI() {
        return descriptorURI;
    }

    public URI getControlURI() {
        return controlURI;
    }

    public URI getEventSubscriptionURI() {
        return eventSubscriptionURI;
    }

    public List<ValidationError> validateThis() {
        List<ValidationError> errors = new ArrayList<>();

        if (getDescriptorURI() == null) {
            errors.add(new ValidationError(getClass(), "descriptorURI", "Descriptor location (SCPDURL) is required"));
        }

        if (getControlURI() == null) {
            errors.add(new ValidationError(getClass(), "controlURI", "Control URL is required"));
        }

        if (getEventSubscriptionURI() == null) {
            errors.add(new ValidationError(getClass(), "eventSubscriptionURI", "Event subscription URL is required"));
        }

        return errors;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") Descriptor: " + getDescriptorURI();
    }
}
