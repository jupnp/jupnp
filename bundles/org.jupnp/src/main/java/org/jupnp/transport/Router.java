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
package org.jupnp.transport;

import java.net.InetAddress;
import java.util.List;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.UpnpStream;

/**
 * Interface of the network transport layer.
 * <p>
 * Encapsulates the transport layer and provides methods to the upper layers for
 * sending UPnP stream (HTTP) {@link org.jupnp.model.message.StreamRequestMessage}s,
 * sending (UDP) datagram {@link org.jupnp.model.message.OutgoingDatagramMessage}s,
 * as well as broadcasting bytes to all LAN participants.
 * </p>
 * <p>
 * A router also maintains listening sockets and services, for incoming UDP unicast/multicast
 * {@link org.jupnp.model.message.IncomingDatagramMessage} and TCP
 * {@link org.jupnp.transport.spi.UpnpStream}s. An implementation of this interface
 * handles these messages, e.g. by selecting and executing the right protocol.
 * </p>
 * <p>
 * An implementation must be thread-safe, and can be accessed concurrently. If the Router is
 * disabled, it doesn't listen on the network for incoming messages and does not send outgoing
 * messages.
 * </p>
 *
 * @see org.jupnp.protocol.ProtocolFactory
 *
 * @author Christian Bauer
 */
public interface Router {

    /**
     * @return The configuration used by this router.
     */
    UpnpServiceConfiguration getConfiguration();

    /**
     * @return The protocol factory used by this router.
     */
    ProtocolFactory getProtocolFactory();

    /**
     * Starts all sockets and listening threads for datagrams and streams.
     *
     * @return <code>true</code> if the router was enabled. <code>false</code> if it's already running.
     */
    boolean enable() throws RouterException;

    /**
     * Unbinds all sockets and stops all listening threads for datagrams and streams.
     *
     * @return <code>true</code> if the router was disabled. <code>false</code> if it wasn't running.
     */
    boolean disable() throws RouterException;

    /**
     * Disables the router and releases all other resources.
     */
    void shutdown() throws RouterException;

    /**
     *
     * @return <code>true</code> if the router is currently enabled.
     */
    boolean isEnabled() throws RouterException;

    /**
     * Called by the {@link #enable()} method before it returns.
     *
     * @param e The cause of the failure.
     * @throws InitializationException if the exception was not recoverable.
     */
    void handleStartFailure(InitializationException e) throws InitializationException;

    /**
     * @param preferredAddress A preferred stream server bound address or <code>null</code>.
     * @return An empty list if no stream server is currently active, otherwise a single network
     *         address if the preferred address is active, or a list of all active bound
     *         stream servers.
     */
    List<NetworkAddress> getActiveStreamServers(InetAddress preferredAddress) throws RouterException;

    /**
     * <p>
     * This method is called internally by the transport layer when a datagram, either unicast or
     * multicast, has been received. An implementation of this interface has to handle the received
     * message, e.g. selecting and executing a UPnP protocol. This method should not block until
     * the execution completes, the calling thread should be free to handle the next reception as
     * soon as possible.
     * </p>
     * 
     * @param msg The received datagram message.
     */
    void received(IncomingDatagramMessage msg);

    /**
     * <p>
     * This method is called internally by the transport layer when a TCP stream connection has
     * been made and a response has to be returned to the sender. An implementation of this interface
     * has to handle the received stream connection and return a response, e.g. selecting and executing
     * a UPnP protocol. This method should not block until the execution completes, the calling thread
     * should be free to process the next reception as soon as possible. Typically this means starting
     * a new thread of execution in this method.
     * </p>
     * 
     * @param stream
     */
    void received(UpnpStream stream);

    /**
     * <p>
     * Call this method to send a UDP datagram message.
     * </p>
     * 
     * @param msg The UDP datagram message to send.
     * @throws RouterException if a recoverable error, such as thread interruption, occurs.
     */
    void send(OutgoingDatagramMessage msg) throws RouterException;

    /**
     * <p>
     * Call this method to send a TCP (HTTP) stream message.
     * </p>
     * 
     * @param msg The TCP (HTTP) stream message to send.
     * @return The response received from the server.
     * @throws RouterException if a recoverable error, such as thread interruption, occurs.
     */
    StreamResponseMessage send(StreamRequestMessage msg) throws RouterException;

    /**
     * <p>
     * Call this method to broadcast a UDP message to all hosts on the network.
     * </p>
     * 
     * @param bytes The byte payload of the UDP datagram.
     * @throws RouterException if a recoverable error, such as thread interruption, occurs.
     */
    void broadcast(byte[] bytes) throws RouterException;
}
