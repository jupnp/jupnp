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
package org.jupnp;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.jupnp.transport.Router;

/**
 * Primary interface of the jUPnP Core UPnP stack.
 * <p>
 * An implementation can either start immediately when constructed or offer an additional
 * method that starts the UPnP stack on-demand. Implementations are not required to be
 * restartable after shutdown.
 * </p>
 * <p>
 * Implementations are always thread-safe and can be shared and called concurrently.
 * </p>
 *
 * @author Christian Bauer
 */
public interface UpnpService {

    UpnpServiceConfiguration getConfiguration();

    ControlPoint getControlPoint();

    ProtocolFactory getProtocolFactory();

    Registry getRegistry();

    Router getRouter();

    /**
     * Stopping the UPnP stack.
     * <p>
     * Clients are required to stop the UPnP stack properly. Notifications for
     * disappearing devices will be multicast'ed, existing event subscriptions cancelled.
     * </p>
     */
    void shutdown();

    class Start {

    }

    class Shutdown {

    }

    void startup();
}
