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

import java.util.concurrent.Future;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;

/**
 * Unified API for the asynchronous execution of network searches, actions, event subscriptions.
 *
 * @author Christian Bauer
 */
public interface ControlPoint {

    UpnpServiceConfiguration getConfiguration();

    ProtocolFactory getProtocolFactory();

    Registry getRegistry();

    void search();

    void search(UpnpHeader searchType);

    void search(int mxSeconds);

    void search(UpnpHeader searchType, int mxSeconds);

    Future execute(ActionCallback callback);

    void execute(SubscriptionCallback callback);
}
