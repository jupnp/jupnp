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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.controlpoint.event.ExecuteAction;
import org.jupnp.controlpoint.event.Search;
import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation.
 * <p>
 * This implementation uses the executor returned by
 * {@link org.jupnp.UpnpServiceConfiguration#getSyncProtocolExecutorService()}.
 * </p>
 *
 * @author Christian Bauer
 */
public class ControlPointImpl implements ControlPoint {

    private final Logger logger = LoggerFactory.getLogger(ControlPointImpl.class);

    protected UpnpServiceConfiguration configuration;
    protected ProtocolFactory protocolFactory;
    protected Registry registry;

    protected ControlPointImpl() {
    }

    public ControlPointImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory,
            Registry registry) {
        logger.trace("Creating ControlPoint: {}", getClass().getName());

        this.configuration = configuration;
        this.protocolFactory = protocolFactory;
        this.registry = registry;
    }

    @Override
    public UpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    @Override
    public Registry getRegistry() {
        return registry;
    }

    public void search(Search search) {
        search(search.getSearchType(), search.getMxSeconds());
    }

    @Override
    public void search() {
        search(new STAllHeader(), MXHeader.DEFAULT_VALUE);
    }

    @Override
    public void search(UpnpHeader searchType) {
        search(searchType, MXHeader.DEFAULT_VALUE);
    }

    @Override
    public void search(int mxSeconds) {
        search(new STAllHeader(), mxSeconds);
    }

    @Override
    public void search(UpnpHeader searchType, int mxSeconds) {
        logger.trace("Sending asynchronous search for: {}", searchType.getString());
        getConfiguration().getAsyncProtocolExecutor()
                .execute(getProtocolFactory().createSendingSearch(searchType, mxSeconds));
    }

    public void execute(ExecuteAction executeAction) {
        execute(executeAction.getCallback());
    }

    @Override
    public Future execute(ActionCallback callback) {
        logger.trace("Invoking action in background: {}", callback);
        callback.setControlPoint(this);
        ExecutorService executor = getConfiguration().getSyncProtocolExecutorService();
        return executor.submit(callback);
    }

    @Override
    public void execute(SubscriptionCallback callback) {
        logger.trace("Invoking subscription in background: {}", callback);
        callback.setControlPoint(this);
        getConfiguration().getSyncProtocolExecutorService().execute(callback);
    }
}
