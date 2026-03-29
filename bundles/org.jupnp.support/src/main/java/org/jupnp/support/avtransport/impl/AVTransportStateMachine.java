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
package org.jupnp.support.avtransport.impl;

import java.net.URI;

import org.jupnp.support.avtransport.impl.state.AbstractState;
import org.jupnp.support.model.SeekMode;
import org.jupnp.util.statemachine.StateMachine;

public interface AVTransportStateMachine extends StateMachine<AbstractState<?>> {

    void setTransportURI(URI uri, String uriMetaData);

    void setNextTransportURI(URI uri, String uriMetaData);

    void stop();

    void play(String speed);

    void pause();

    void record();

    void seek(SeekMode unit, String target);

    void next();

    void previous();
}
