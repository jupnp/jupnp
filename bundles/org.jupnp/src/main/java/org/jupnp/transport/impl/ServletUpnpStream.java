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
package org.jupnp.transport.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jupnp.model.message.Connection;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.spi.UpnpStream;
import org.jupnp.util.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract implementation of a {@link UpnpStream}. This class is extended for each servlet implementations (blocking
 * servlet 2.4 and async servlet 3.0).
 *
 * @author Ivan Iliev - Initial contribution and API
 *
 */
public abstract class ServletUpnpStream extends UpnpStream {

    protected StreamResponseMessage responseMessage;

    protected final Logger logger = LoggerFactory.getLogger(ServletUpnpStream.class);

    protected ServletUpnpStream(ProtocolFactory protocolFactory) {
        super(protocolFactory);
    }

    @Override
    public void run() {
        try {
            StreamRequestMessage requestMessage = readRequestMessage();
            logger.trace("Processing new request message: {}", requestMessage);

            responseMessage = process(requestMessage);

            if (responseMessage != null) {
                logger.trace("Preparing HTTP response message: {}", responseMessage);
                writeResponseMessage(responseMessage);
            } else {
                // If it's null, it's 404
                logger.trace("Sending HTTP response status: {}", HttpURLConnection.HTTP_NOT_FOUND);
                getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
            }

        } catch (Exception e) {
            logger.info("Exception occurred during UPnP stream processing", e);
            if (!getResponse().isCommitted()) {
                logger.trace("Response hasn't been committed, returning INTERNAL SERVER ERROR to client");
                getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } else {
                logger.info("Could not return INTERNAL SERVER ERROR to client, response was already committed");
            }
            responseException(e);
        } finally {
            complete();
        }
    }

    protected StreamRequestMessage readRequestMessage() throws IOException {
        // Extract what we need from the HTTP httpRequest
        String requestMethod = getRequest().getMethod();
        String requestURI = getRequest().getRequestURI();

        logger.trace("Processing HTTP request: {} {} ", requestMethod, requestURI);

        StreamRequestMessage requestMessage;
        try {
            requestMessage = new StreamRequestMessage(UpnpRequest.Method.getByHttpName(requestMethod),
                    URI.create(requestURI));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid request URI: " + requestURI, e);
        }

        if (requestMessage.getOperation().getMethod().equals(UpnpRequest.Method.UNKNOWN)) {
            throw new RuntimeException("Method not supported: " + requestMethod);
        }

        // Connection wrapper
        requestMessage.setConnection(createConnection());

        // Headers
        UpnpHeaders headers = new UpnpHeaders();
        Enumeration<String> headerNames = getRequest().getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            Enumeration<String> headerValues = getRequest().getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                headers.add(headerName, headerValue);
            }
        }
        requestMessage.setHeaders(headers);

        // Body
        byte[] bodyBytes;
        try (InputStream is = getRequest().getInputStream()) {

            // Needed as on some bad HTTP Stack implementations the inputStream may block when trying to read a request
            // without a body (GET)
            if (UpnpRequest.Method.GET.getHttpName().equals(requestMethod)) {
                bodyBytes = new byte[] {};
            } else {
                bodyBytes = IO.readAllBytes(is);
            }
        }
        logger.trace("Reading request body bytes: {}", bodyBytes.length);

        if (bodyBytes.length > 0 && requestMessage.isContentTypeMissingOrText()) {
            logger.trace("Request contains textual entity body, converting then setting string on message");
            requestMessage.setBodyCharacters(bodyBytes);

        } else if (bodyBytes.length > 0) {
            logger.trace("Request contains binary entity body, setting bytes on message");
            requestMessage.setBody(UpnpMessage.BodyType.BYTES, bodyBytes);

        } else {
            logger.trace("Request did not contain entity body");
        }

        return requestMessage;
    }

    protected void writeResponseMessage(StreamResponseMessage responseMessage) throws IOException {
        logger.trace("Sending HTTP response status: {}", responseMessage.getOperation().getStatusCode());

        getResponse().setStatus(responseMessage.getOperation().getStatusCode());

        // Headers
        for (Map.Entry<String, List<String>> entry : responseMessage.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                getResponse().addHeader(entry.getKey(), value);
            }
        }
        // The Date header is recommended in UDA
        getResponse().setDateHeader("Date", System.currentTimeMillis());

        // Body
        byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;
        int contentLength = responseBodyBytes != null ? responseBodyBytes.length : -1;

        if (contentLength > 0) {
            getResponse().setContentLength(contentLength);
            logger.trace("Response message has body, writing bytes to stream...");
            getResponse().getOutputStream().write(responseBodyBytes);
        }
    }

    protected abstract Connection createConnection();

    protected abstract HttpServletRequest getRequest();

    protected abstract HttpServletResponse getResponse();

    protected abstract void complete();
}
