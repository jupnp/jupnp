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
package org.jupnp.protocol.async;

import org.jupnp.UpnpService;
import org.jupnp.model.message.discovery.OutgoingSearchRequest;
import org.jupnp.model.message.header.MXHeader;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.protocol.SendingAsync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sending search request messages using the supplied search type.
 * <p>
 * Sends all search messages 5 times, waits 0 to 500
 * milliseconds between each sending procedure.
 * </p>
 *
 * @author Christian Bauer
 */
public class SendingSearch extends SendingAsync {

    private final Logger logger = LoggerFactory.getLogger(SendingSearch.class);

    private final UpnpHeader searchTarget;
    private final int mxSeconds;

    /**
     * Defaults to {@link org.jupnp.model.message.header.STAllHeader} and an MX of 3 seconds.
     */
    public SendingSearch(UpnpService upnpService) {
        this(upnpService, new STAllHeader());
    }

    /**
     * Defaults to an MX value of 3 seconds.
     */
    public SendingSearch(UpnpService upnpService, UpnpHeader searchTarget) {
        this(upnpService, searchTarget, MXHeader.DEFAULT_VALUE);
    }

    /**
     * @param mxSeconds The time in seconds a host should wait before responding.
     */
    public SendingSearch(UpnpService upnpService, UpnpHeader searchTarget, int mxSeconds) {
        super(upnpService);

        if (!UpnpHeader.Type.ST.isValidHeaderType(searchTarget.getClass())) {
            throw new IllegalArgumentException(
                    "Given search target instance is not a valid header class for type ST: " + searchTarget.getClass());
        }
        this.searchTarget = searchTarget;
        this.mxSeconds = mxSeconds;
    }

    public UpnpHeader getSearchTarget() {
        return searchTarget;
    }

    public int getMxSeconds() {
        return mxSeconds;
    }

    @Override
    protected void execute() throws RouterException {

        logger.trace("Executing search for target: {} with MX seconds: {}", searchTarget.getString(), getMxSeconds());

        OutgoingSearchRequest msg = new OutgoingSearchRequest(searchTarget, getMxSeconds());
        prepareOutgoingSearchRequest(msg);

        for (int i = 0; i < getBulkRepeat(); i++) {
            try {

                getUpnpService().getRouter().send(msg);

                // UDA 1.0 is silent about this but UDA 1.1 recommends "a few hundred milliseconds"
                logger.trace("Sleeping {} milliseconds", getBulkIntervalMilliseconds());
                Thread.sleep(getBulkIntervalMilliseconds());

            } catch (InterruptedException e) {
                // Interruption means we stop sending search messages, e.g. on shutdown of thread pool
                break;
            }
        }
    }

    public int getBulkRepeat() {
        return 3; // UDA 1.0 says "repeat more than once"
    }

    public int getBulkIntervalMilliseconds() {
        return 500; // That should be plenty on an ethernet LAN
    }

    /**
     * Override this to edit the outgoing message, e.g. by adding headers.
     */
    protected void prepareOutgoingSearchRequest(OutgoingSearchRequest message) {
    }
}
