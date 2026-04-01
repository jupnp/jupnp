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
package org.jupnp.support.avtransport.impl.state;

import java.net.URI;

import org.jupnp.support.avtransport.lastchange.AVTransportVariable;
import org.jupnp.support.model.AVTransport;
import org.jupnp.support.model.TransportAction;
import org.jupnp.support.model.TransportInfo;
import org.jupnp.support.model.TransportState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class PausedPlay<T extends AVTransport> extends AbstractState<T> {

    private final Logger logger = LoggerFactory.getLogger(PausedPlay.class);

    protected PausedPlay(T transport) {
        super(transport);
    }

    public void onEntry() {
        logger.debug("Setting transport state to PAUSED_PLAYBACK");
        getTransport().setTransportInfo(new TransportInfo(TransportState.PAUSED_PLAYBACK,
                getTransport().getTransportInfo().getCurrentTransportStatus(),
                getTransport().getTransportInfo().getCurrentSpeed()));
        getTransport().getLastChange().setEventedValue(getTransport().getInstanceId(),
                new AVTransportVariable.TransportState(TransportState.PAUSED_PLAYBACK),
                new AVTransportVariable.CurrentTransportActions(getCurrentTransportActions()));
    }

    public abstract Class<? extends AbstractState<?>> setTransportURI(URI uri, String metaData);

    public abstract Class<? extends AbstractState<?>> stop();

    public abstract Class<? extends AbstractState<?>> play(String speed);

    @Override
    public TransportAction[] getCurrentTransportActions() {
        return new TransportAction[] { TransportAction.Stop, TransportAction.Play };
    }
}
