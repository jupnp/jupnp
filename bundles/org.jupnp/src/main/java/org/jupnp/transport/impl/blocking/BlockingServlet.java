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
package org.jupnp.transport.impl.blocking;

import java.io.IOException;

import org.jupnp.model.message.Connection;
import org.jupnp.transport.Router;
import org.jupnp.transport.impl.ServletConnection;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Http servlet implementation that uses the {@link Router}'s executor to process the current request and blocks until
 * processing is done (Synchronous).
 * 
 * @author Ivan Iliev
 * 
 */
public class BlockingServlet extends HttpServlet {

    private static final long serialVersionUID = 3124088565842038644L;

    private final Logger logger = LoggerFactory.getLogger(BlockingServlet.class);

    private final Router router;

    private int mCounter = 0;

    private final ServletStreamServerConfigurationImpl configuration;

    public BlockingServlet(Router router, ServletStreamServerConfigurationImpl configuration) {
        this.router = router;
        this.configuration = configuration;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        final long startTime = System.currentTimeMillis();
        final int counter = mCounter++;
        logger.trace("{}",
                String.format("HttpServlet.service(): id: %3d, request URI: %s", counter, req.getRequestURI()));
        logger.trace("Handling Servlet request synchronously: {}", req);

        FauxAsyncContext asyncContext = new FauxAsyncContext(req, resp);
        asyncContext.setTimeout(configuration.getAsyncTimeoutSeconds() * 1000);

        BlockingServletUpnpStream stream = new BlockingServletUpnpStream(router.getProtocolFactory(), asyncContext) {
            @Override
            protected Connection createConnection() {
                return new ServletConnection(getRequest());
            }
        };

        router.received(stream);

        // block until we timeout or the processing thread signals
        // completion
        asyncContext.waitForTimeoutOrCompletion();

        long duration = System.currentTimeMillis() - startTime;

        if (asyncContext.isCompleted()) {
            logger.trace("{}", String.format("BlockingServlet completed: id: %3d, duration: %,4d", counter, duration));
        } else {
            // set internal server error as response code when timeout
            // as per AsyncContext specification
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.warn("{}", String.format("BlockingServlet timed out: id: %3d, duration: %,4d, request: %s", counter,
                    duration, req));
        }
    }
}
