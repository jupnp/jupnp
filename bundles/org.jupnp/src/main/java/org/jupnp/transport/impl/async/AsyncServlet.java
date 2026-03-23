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

import org.jupnp.model.message.Connection;
import org.jupnp.transport.Router;
import org.jupnp.transport.impl.ServletConnection;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Http servlet implementation that uses the {@link Router}'s executor to process the current request and releases the
 * current thread(asynchronous).
 * 
 * @author Ivan Iliev
 * 
 */
public class AsyncServlet extends HttpServlet {

    /**
     * 
     */
    private static final long serialVersionUID = -5751553619541219814L;

    private final Logger logger = LoggerFactory.getLogger(AsyncServlet.class);

    private final Router router;

    private int mCounter = 0;

    private final ServletStreamServerConfigurationImpl configuration;

    public AsyncServlet(Router router, ServletStreamServerConfigurationImpl configuration) {
        this.router = router;
        this.configuration = configuration;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final long startTime = System.currentTimeMillis();
        final int counter = mCounter++;
        logger.info("{}",
                String.format("HttpServlet.service(): id: %3d, request URI: %s", counter, req.getRequestURI()));
        logger.debug("Handling Servlet request asynchronously: {}", req);

        AsyncContext async = req.startAsync();
        async.setTimeout(configuration.getAsyncTimeoutSeconds() * 1000);

        async.addListener(new AsyncListener() {

            @Override
            public void onTimeout(AsyncEvent arg0) throws IOException {
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("{}", String.format("AsyncListener.onTimeout(): id: %3d, duration: %,4d, request: %s",
                        counter, duration, arg0.getSuppliedRequest()));
            }

            @Override
            public void onStartAsync(AsyncEvent arg0) throws IOException {
                // useless
                logger.debug("{}", String.format("AsyncListener.onStartAsync(): id: %3d, request: %s", counter,
                        arg0.getSuppliedRequest()));
            }

            @Override
            public void onError(AsyncEvent arg0) throws IOException {
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("{}", String.format("AsyncListener.onError(): id: %3d, duration: %,4d, response: %s",
                        counter, duration, arg0.getSuppliedResponse()));
            }

            @Override
            public void onComplete(AsyncEvent arg0) throws IOException {
                long duration = System.currentTimeMillis() - startTime;
                logger.debug("{}", String.format("AsyncListener.onComplete(): id: %3d, duration: %,4d, response: %s",
                        counter, duration, arg0.getSuppliedResponse()));
            }
        });

        AsyncServletUpnpStream stream = new AsyncServletUpnpStream(router.getProtocolFactory(), async, req) {
            @Override
            protected Connection createConnection() {
                return new ServletConnection(getRequest());
            }
        };

        router.received(stream);
    }
}
