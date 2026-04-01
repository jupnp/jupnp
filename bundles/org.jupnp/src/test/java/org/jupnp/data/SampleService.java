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
package org.jupnp.data;

import java.lang.reflect.Constructor;
import java.net.URI;

import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.Service;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;

/**
 * @author Christian Bauer
 */
public abstract class SampleService {

    public abstract ServiceType getServiceType();

    public abstract ServiceId getServiceId();

    public abstract URI getDescriptorURI();

    public abstract URI getControlURI();

    public abstract URI getEventSubscriptionURI();

    public abstract Action[] getActions();

    public abstract StateVariable[] getStateVariables();

    public <S extends Service> S newInstanceLocal(Constructor<S> ctor) {
        try {
            return ctor.newInstance(getServiceType(), getServiceId(), getActions(), getStateVariables());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <S extends Service> S newInstanceRemote(Constructor<S> ctor) {
        try {
            return ctor.newInstance(getServiceType(), getServiceId(), getDescriptorURI(), getControlURI(),
                    getEventSubscriptionURI(), getActions(), getStateVariables());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
