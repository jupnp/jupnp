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
package org.jupnp.transport.impl.jetty12;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FutureCallback;
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
 * {@link UpnpStream} implementation based on the Jetty 12.x core API.
 *
 * @author Holger Friedrich - initial contribution
 */
public class Jetty12UpnpStream extends UpnpStream {

    private final Logger logger = LoggerFactory.getLogger(Jetty12UpnpStream.class);

    protected final Request request;
    protected final Response response;
    protected final Callback callback;

    protected Jetty12UpnpStream(ProtocolFactory protocolFactory, Request request, Response response,
            Callback callback) {
        super(protocolFactory);
        this.request = request;
        this.response = response;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            StreamRequestMessage requestMessage = readRequestMessage();
            logger.trace("Processing new request message: {}", requestMessage);

            StreamResponseMessage responseMessage = process(requestMessage);

            if (responseMessage != null) {
                logger.trace("Preparing HTTP response message: {}", responseMessage);
                writeResponseMessage(responseMessage);
            } else {
                // If it's null, it's 404
                logger.trace("Sending HTTP response status: {}", HttpURLConnection.HTTP_NOT_FOUND);
                response.setStatus(HttpURLConnection.HTTP_NOT_FOUND);
            }

            callback.succeeded();
            responseSent(responseMessage);
        } catch (Exception e) {
            logger.info("Exception occurred during UPnP stream processing", e);
            responseException(e);
            callback.failed(e);
        }
    }

    protected StreamRequestMessage readRequestMessage() throws IOException {
        // Extract what we need from the HTTP request
        String requestMethod = request.getMethod();
        String requestURI = request.getHttpURI().getPathQuery();

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
        for (HttpField field : request.getHeaders()) {
            headers.add(field.getName(), field.getValue());
        }
        requestMessage.setHeaders(headers);

        // Body
        byte[] bodyBytes;
        if (UpnpRequest.Method.GET.getHttpName().equals(requestMethod)) {
            // No body expected for a GET request
            bodyBytes = new byte[0];
        } else {
            try (InputStream is = Request.asInputStream(request)) {
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

    protected void writeResponseMessage(StreamResponseMessage responseMessage) throws Exception {
        logger.trace("Sending HTTP response status: {}", responseMessage.getOperation().getStatusCode());

        response.setStatus(responseMessage.getOperation().getStatusCode());

        // Headers
        for (Map.Entry<String, List<String>> entry : responseMessage.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                response.getHeaders().add(entry.getKey(), value);
            }
        }

        // Body
        byte[] responseBodyBytes = responseMessage.hasBody() ? responseMessage.getBodyBytes() : null;

        if (responseBodyBytes != null && responseBodyBytes.length > 0) {
            logger.trace("Response message has body, writing bytes to stream...");
            FutureCallback writeCallback = new FutureCallback();
            response.write(true, ByteBuffer.wrap(responseBodyBytes), writeCallback);
            writeCallback.get();
        }
    }

    protected Connection createConnection() {
        return new Jetty12ServerConnection(request);
    }

    /**
     * UPnP {@link Connection} implementation backed by a Jetty 12.x server {@link Request}.
     */
    protected static class Jetty12ServerConnection implements Connection {

        protected final Request request;

        public Jetty12ServerConnection(Request request) {
            this.request = request;
        }

        @Override
        public boolean isOpen() {
            return request.getConnectionMetaData().getConnection().getEndPoint().isOpen();
        }

        @Override
        public InetAddress getRemoteAddress() {
            return getInetAddress(request.getConnectionMetaData().getRemoteSocketAddress());
        }

        @Override
        public InetAddress getLocalAddress() {
            return getInetAddress(request.getConnectionMetaData().getLocalSocketAddress());
        }

        private InetAddress getInetAddress(SocketAddress socketAddress) {
            if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
                return inetSocketAddress.getAddress();
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return "" + hashCode();
    }
}
