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
package org.jupnp.transport.impl.jetty;

import static org.eclipse.jetty.http.HttpHeader.CONNECTION;

import java.util.concurrent.Callable;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentProvider;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpOperation;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.transport.spi.AbstractStreamClient;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation based on <a href="http://www.eclipse.org/jetty/">Jetty 9.2.x</a>.
 * <p>
 *
 * @author Victor Toni - initial contribution
 */
public class JettyStreamClientImpl extends AbstractStreamClient<StreamClientConfigurationImpl, Request> {

    private final Logger logger = LoggerFactory.getLogger(StreamClient.class);

    protected final StreamClientConfigurationImpl configuration;
    protected final HttpClient httpClient;
    protected final HttpFields defaultHttpFields = new HttpFields();

    public JettyStreamClientImpl(StreamClientConfigurationImpl configuration) throws InitializationException {
        this.configuration = configuration;

        httpClient = new HttpClient();

        // These are some safety settings, we should never run into these timeouts as we
        // do our own expiration checking
        httpClient.setConnectTimeout((getConfiguration().getTimeoutSeconds() + 5) * 1000);
        httpClient.setMaxConnectionsPerDestination(2);

        int cpus = Runtime.getRuntime().availableProcessors();
        int maxThreads = 5 * cpus;

        final QueuedThreadPool queuedThreadPool = createThreadPool("jupnp-jetty-client", 5, maxThreads, 60000);

        httpClient.setExecutor(queuedThreadPool);

        if (getConfiguration().getSocketBufferSize() != -1) {
            httpClient.setRequestBufferSize(getConfiguration().getSocketBufferSize());
            httpClient.setResponseBufferSize(getConfiguration().getSocketBufferSize());
        }

        try {
            httpClient.start();
        } catch (final Exception e) {
            logger.error("Failed to instantiate HTTP client", e);
            throw new InitializationException("Failed to instantiate HTTP client", e);
        }
    }

    @Override
    public StreamClientConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    protected Request createRequest(StreamRequestMessage requestMessage) {
        final UpnpRequest upnpRequest = requestMessage.getOperation();

        logger.trace("Creating HTTP request. URI: '{}' method: '{}'", upnpRequest.getURI(), upnpRequest.getMethod());
        Request request;
        switch (upnpRequest.getMethod()) {
            case GET:
            case SUBSCRIBE:
            case UNSUBSCRIBE:
            case POST:
            case NOTIFY:
                try {
                    request = httpClient.newRequest(upnpRequest.getURI()).method(upnpRequest.getHttpMethodName());
                } catch (IllegalArgumentException e) {
                    logger.debug("Cannot create request because URI '{}' is invalid", upnpRequest.getURI(), e);
                    return null;
                }
                break;
            default:
                throw new RuntimeException("Unknown HTTP method: " + upnpRequest.getHttpMethodName());
        }
        switch (upnpRequest.getMethod()) {
            case POST:
            case NOTIFY:
                request.content(createContentProvider(requestMessage));
                break;
            default:
        }

        // prepare default headers
        request.getHeaders().add(defaultHttpFields);

        // FIXME: what about HTTP2 ?
        if (requestMessage.getOperation().getHttpMinorVersion() == 0) {
            request.version(HttpVersion.HTTP_1_0);
        } else {
            request.version(HttpVersion.HTTP_1_1);
            // This closes the http connection immediately after the call.
            //
            // Even though jetty client is able to close connections properly,
            // it still takes ~30 seconds to do so. This may cause too many
            // connections for installations with many upnp devices.
            request.header(CONNECTION, "close");
        }

        // Add the default user agent if not already set on the message
        if (!requestMessage.getHeaders().containsKey(UpnpHeader.Type.USER_AGENT)) {
            request.agent(getConfiguration().getUserAgentValue(requestMessage.getUdaMajorVersion(),
                    requestMessage.getUdaMinorVersion()));
        }

        // Headers
        HeaderUtil.add(request, requestMessage.getHeaders());

        return request;
    }

    @Override
    protected Callable<StreamResponseMessage> createCallable(final StreamRequestMessage requestMessage,
            final Request request) {
        return () -> {
            logger.trace("Sending HTTP request: {}", requestMessage);
            try {
                final ContentResponse httpResponse = request.send();

                logger.trace("Received HTTP response: {}", httpResponse.getReason());

                // Status
                final UpnpResponse responseOperation = new UpnpResponse(httpResponse.getStatus(),
                        httpResponse.getReason());

                // Message
                final StreamResponseMessage responseMessage = new StreamResponseMessage(responseOperation);

                // Headers
                responseMessage.setHeaders(new UpnpHeaders(HeaderUtil.get(httpResponse)));

                // Body
                final byte[] bytes = httpResponse.getContent();
                if (bytes == null || 0 == bytes.length) {
                    logger.trace("HTTP response message has no entity");

                    return responseMessage;
                }

                if (responseMessage.isContentTypeMissingOrText()) {
                    logger.trace("HTTP response message contains text entity");
                } else {
                    logger.trace("HTTP response message contains binary entity");
                }

                responseMessage.setBodyCharacters(bytes);

                return responseMessage;
            } catch (final RuntimeException e) {
                logger.error("Request: {} failed", request, e);
                throw e;
            }
        };
    }

    @Override
    protected void abort(Request request) {
        request.abort(new Exception("Request aborted by API"));
    }

    @Override
    protected boolean logExecutionException(Throwable t) {
        if (t == null) {
            return false;
        }

        if (t instanceof IllegalStateException) {
            // TODO: Document when/why this happens and why we can ignore it, violating the
            // logging rules of the StreamClient#sendRequest() method
            logger.trace("Illegal state: {}", t.getMessage());
            return true;
        }

        String msg = t.getMessage();
        if (msg != null && msg.contains("HTTP protocol violation")) {
            SpecificationViolationReporter.report(msg);
            return true;
        }

        return false;
    }

    @Override
    public void stop() {
        logger.trace("Shutting down HTTP client connection manager/pool");
        try {
            httpClient.stop();
        } catch (Exception e) {
            logger.info("Shutting down of HTTP client throwed exception", e);
        }
    }

    protected <O extends UpnpOperation> ContentProvider.Typed createContentProvider(final UpnpMessage<O> upnpMessage) {
        if (upnpMessage.getBodyType().equals(UpnpMessage.BodyType.STRING)) {
            logger.trace("Preparing HTTP request entity as String");
            return new StringContentProvider(upnpMessage.getBodyString(), upnpMessage.getContentTypeCharset());
        } else {
            logger.trace("Preparing HTTP request entity as byte[]");
            return new BytesContentProvider(upnpMessage.getBodyBytes());
        }
    }

    private QueuedThreadPool createThreadPool(String consumerName, int minThreads, int maxThreads,
            int keepAliveTimeout) {
        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(maxThreads, minThreads, keepAliveTimeout);
        queuedThreadPool.setName(consumerName);
        queuedThreadPool.setDaemon(true);
        return queuedThreadPool;
    }
}
