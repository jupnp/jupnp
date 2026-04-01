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
package org.jupnp.support.contentdirectory.callback;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.SortCriterion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invokes a "Browse" action, parses the result.
 *
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class Browse extends ActionCallback {

    private static final int DEFAULT_MAX_RESULT = 999;
    public static final String CAPS_WILDCARD = "*";

    public enum Status {
        NO_CONTENT("No Content"),
        LOADING("Loading..."),
        OK("OK");

        private final String defaultMessage;

        Status(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    private final Logger logger = LoggerFactory.getLogger(Browse.class);

    /**
     * Browse with first result 0 and {@link #getDefaultMaxResults()}, filters with {@link #CAPS_WILDCARD}.
     */
    protected Browse(Service<?, ?> service, String containerId, BrowseFlag flag) {
        this(service, containerId, flag, CAPS_WILDCARD, 0, null);
    }

    /**
     * @param maxResults Can be <code>null</code>, then {@link #getDefaultMaxResults()} is used.
     */
    protected Browse(Service<?, ?> service, String objectID, BrowseFlag flag, String filter, long firstResult,
            Long maxResults, SortCriterion... orderBy) {

        super(new ActionInvocation<>(service.getAction("Browse")));

        logger.debug("Creating browse action for object ID: {}", objectID);

        getActionInvocation().setInput("ObjectID", objectID);
        getActionInvocation().setInput("BrowseFlag", flag.toString());
        getActionInvocation().setInput("Filter", filter);
        getActionInvocation().setInput("StartingIndex", new UnsignedIntegerFourBytes(firstResult));
        getActionInvocation().setInput("RequestedCount",
                new UnsignedIntegerFourBytes(maxResults == null ? getDefaultMaxResults() : maxResults));
        getActionInvocation().setInput("SortCriteria", SortCriterion.toString(orderBy));
    }

    @Override
    public void run() {
        updateStatus(Status.LOADING);
        super.run();
    }

    @Override
    public void success(ActionInvocation invocation) {
        logger.debug("Successful browse action, reading output argument values");

        BrowseResult result = new BrowseResult(invocation.getOutput("Result").getValue().toString(),
                (UnsignedIntegerFourBytes) invocation.getOutput("NumberReturned").getValue(),
                (UnsignedIntegerFourBytes) invocation.getOutput("TotalMatches").getValue(),
                (UnsignedIntegerFourBytes) invocation.getOutput("UpdateID").getValue());

        boolean proceed = receivedRaw(invocation, result);

        if (proceed && result.getCountLong() > 0 && !result.getResult().isEmpty()) {

            try {

                DIDLParser didlParser = new DIDLParser();
                DIDLContent didl = didlParser.parse(result.getResult());
                received(invocation, didl);
                updateStatus(Status.OK);

            } catch (Exception e) {
                invocation.setFailure(
                        new ActionException(ErrorCode.ACTION_FAILED, "Can't parse DIDL XML response: " + e, e));
                failure(invocation, null);
            }

        } else {
            received(invocation, new DIDLContent());
            updateStatus(Status.NO_CONTENT);
        }
    }

    /**
     * Some media servers will crash if there is no limit on the maximum number of results.
     *
     * @return The default limit.
     */
    public long getDefaultMaxResults() {
        return DEFAULT_MAX_RESULT;
    }

    public boolean receivedRaw(ActionInvocation<?> actionInvocation, BrowseResult browseResult) {
        return true;
    }

    public abstract void received(ActionInvocation<?> actionInvocation, DIDLContent didl);

    public abstract void updateStatus(Status status);
}
