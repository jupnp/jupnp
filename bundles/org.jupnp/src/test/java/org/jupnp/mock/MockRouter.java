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
package org.jupnp.mock;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.Router;
import org.jupnp.transport.RouterException;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.UpnpStream;

/**
 * <p>
 * This is not a real network transport layer, it collects all messages instead and makes
 * them available for testing with {@link #getOutgoingDatagramMessages()},
 * {@link #getSentStreamRequestMessages()}, etc. Mock responses for TCP (HTTP) stream requests
 * can be returned by overriding {@link #getStreamResponseMessage(org.jupnp.model.message.StreamRequestMessage)}
 * or {@link #getStreamResponseMessages()} if you know the order of requests.
 * </p>
 *
 * @author Christian Bauer
 */
public class MockRouter implements Router {

    public int counter = -1;
    public List<IncomingDatagramMessage> incomingDatagramMessages = new ArrayList<>();
    public List<OutgoingDatagramMessage> outgoingDatagramMessages = new ArrayList<>();
    public List<UpnpStream> receivedUpnpStreams = new ArrayList<>();
    public List<StreamRequestMessage> sentStreamRequestMessages = new ArrayList<>();
    public List<byte[]> broadcastedBytes = new ArrayList<>();

    protected UpnpServiceConfiguration configuration;
    protected ProtocolFactory protocolFactory;

    public MockRouter(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory) {
        this.configuration = configuration;
        this.protocolFactory = protocolFactory;
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
    public boolean enable() throws RouterException {
        return false;
    }

    @Override
    public boolean disable() throws RouterException {
        return false;
    }

    @Override
    public void shutdown() throws RouterException {
    }

    @Override
    public boolean isEnabled() throws RouterException {
        return false;
    }

    @Override
    public void handleStartFailure(InitializationException e) throws InitializationException {
    }

    @Override
    public List<NetworkAddress> getActiveStreamServers(InetAddress preferredAddress) throws RouterException {
        // Simulate an active stream server, otherwise the notification/search response
        // protocols won't even run
        try {
            return Arrays.asList(new NetworkAddress(InetAddress.getByName("127.0.0.1"),
                    NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void received(IncomingDatagramMessage msg) {
        incomingDatagramMessages.add(msg);
    }

    @Override
    public void received(UpnpStream stream) {
        receivedUpnpStreams.add(stream);
    }

    @Override
    public void send(OutgoingDatagramMessage msg) throws RouterException {
        outgoingDatagramMessages.add(msg);
    }

    @Override
    public StreamResponseMessage send(StreamRequestMessage msg) throws RouterException {
        sentStreamRequestMessages.add(msg);
        counter++;
        return getStreamResponseMessages() != null ? getStreamResponseMessages()[counter]
                : getStreamResponseMessage(msg);
    }

    @Override
    public void broadcast(byte[] bytes) {
        broadcastedBytes.add(bytes);
    }

    public void resetStreamRequestMessageCounter() {
        counter = -1;
    }

    public List<IncomingDatagramMessage> getIncomingDatagramMessages() {
        return incomingDatagramMessages;
    }

    public List<OutgoingDatagramMessage> getOutgoingDatagramMessages() {
        return outgoingDatagramMessages;
    }

    public List<UpnpStream> getReceivedUpnpStreams() {
        return receivedUpnpStreams;
    }

    public List<StreamRequestMessage> getSentStreamRequestMessages() {
        return sentStreamRequestMessages;
    }

    public List<byte[]> getBroadcastedBytes() {
        return broadcastedBytes;
    }

    public StreamResponseMessage[] getStreamResponseMessages() {
        return null;
    }

    public StreamResponseMessage getStreamResponseMessage(StreamRequestMessage request) {
        return null;
    }
}
