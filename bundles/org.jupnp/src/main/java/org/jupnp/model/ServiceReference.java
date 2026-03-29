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

import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.UDN;

/**
 * Combines a {@link org.jupnp.model.types.UDN} and a {@link org.jupnp.model.types.ServiceId}.
 * <p>
 * A service reference is useful to remember a service. For example, if a control point has accessed
 * a service one, it can remember the service with {@link org.jupnp.model.meta.Service#getReference()}.
 * Before every action invocation, it can now resolve the reference to an actually registered service with
 * {@link org.jupnp.registry.Registry#getService(ServiceReference)}. If the registry doesn't return
 * a service for the given reference, the service is currently not available.
 * </p>
 * <p>
 * This simplifies implementing disconnect/reconnect behavior in a control point.
 * </p>
 * 
 * @author Christian Bauer
 */
public class ServiceReference {

    public static final String DELIMITER = "/";

    private final UDN udn;
    private final ServiceId serviceId;

    public ServiceReference(String s) {
        String[] split = s.split("/");
        if (split.length == 2) {
            this.udn = UDN.valueOf(split[0]);
            this.serviceId = ServiceId.valueOf(split[1]);
        } else {
            this.udn = null;
            this.serviceId = null;
        }
    }

    public ServiceReference(UDN udn, ServiceId serviceId) {
        this.udn = udn;
        this.serviceId = serviceId;
    }

    public UDN getUdn() {
        return udn;
    }

    public ServiceId getServiceId() {
        return serviceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceReference that = (ServiceReference) o;

        if (!serviceId.equals(that.serviceId)) {
            return false;
        }
        if (!udn.equals(that.udn)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = udn.hashCode();
        result = 31 * result + serviceId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return udn == null || serviceId == null ? "" : udn + DELIMITER + serviceId;
    }
}
