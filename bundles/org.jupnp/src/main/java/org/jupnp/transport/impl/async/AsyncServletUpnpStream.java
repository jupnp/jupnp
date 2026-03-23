/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.transport.impl.async;

import java.io.IOException;

import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.impl.ServletUpnpStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Implementation based on Servlet 3.0 API.
 * <p>
 * Concrete implementations must provide a connection wrapper, as this wrapper most likely has to access proprietary
 * APIs to implement connection checking.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class AsyncServletUpnpStream extends ServletUpnpStream implements AsyncListener {

    private final Logger logger = LoggerFactory.getLogger(AsyncServletUpnpStream.class);

    protected final AsyncContext asyncContext;
    protected final HttpServletRequest request;

    protected AsyncServletUpnpStream(ProtocolFactory protocolFactory, AsyncContext asyncContext,
            HttpServletRequest request) {
        super(protocolFactory);
        this.asyncContext = asyncContext;
        this.request = request;
        asyncContext.addListener(this);
    }

    @Override
    protected HttpServletRequest getRequest() {
        return request;
    }

    @Override
    protected HttpServletResponse getResponse() {
        ServletResponse response;
        if ((response = asyncContext.getResponse()) == null) {
            throw new IllegalStateException("Couldn't get response from asynchronous context, already timed out");
        }
        return (HttpServletResponse) response;
    }

    @Override
    protected void complete() {
        try {
            asyncContext.complete();
        } catch (IllegalStateException e) {
            // If Jetty's connection, for whatever reason, is in an illegal state, this will be thrown
            // and we can "probably" ignore it. The request is complete, no matter how it ended.
            logger.info("Error calling servlet container's AsyncContext#complete() method", e);
        }
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
        // This is a completely useless callback, it will only be called on request.startAsync() which
        // then immediately removes the listener... what were they thinking.
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
        logger.trace("Completed asynchronous processing of HTTP request: {}", event.getSuppliedRequest());
        responseSent(responseMessage);
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
        logger.trace("Asynchronous processing of HTTP request timed out: {}", event.getSuppliedRequest());
        responseException(new Exception("Asynchronous request timed out"));
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
        logger.trace("Asynchronous processing of HTTP request error", event.getThrowable());
        responseException(event.getThrowable());
    }

    @Override
    public String toString() {
        return "" + hashCode();
    }
}
