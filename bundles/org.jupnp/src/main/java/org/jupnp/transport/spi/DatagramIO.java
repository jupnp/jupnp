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
package org.jupnp.transport.spi;

import java.net.DatagramPacket;
import java.net.InetAddress;

import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.transport.Router;

/**
 * Service for receiving (unicast only) and sending UDP datagrams, one per bound IP address.
 * <p>
 * This service typically listens on a socket for UDP unicast datagrams, with
 * an ephemeral port.
 * </p>
 * <p>
 * This listening loop is started with the <code>run()</code> method,
 * this service is <code>Runnable</code>. Any received datagram is then converted into an
 * {@link org.jupnp.model.message.IncomingDatagramMessage} and
 * handled by the
 * {@link org.jupnp.transport.Router#received(org.jupnp.model.message.IncomingDatagramMessage)}
 * method. This conversion is the job of the {@link org.jupnp.transport.spi.DatagramProcessor}.
 * </p>
 * <p>
 * Clients of this service use it to send UDP datagrams, either to a unicast
 * or multicast destination. Any {@link org.jupnp.model.message.OutgoingDatagramMessage} can
 * be converted and written into a datagram with the {@link org.jupnp.transport.spi.DatagramProcessor}.
 * </p>
 * <p>
 * An implementation has to be thread-safe.
 * </p>
 *
 * @param <C> The type of the service's configuration.
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - added multicast response port
 */
public interface DatagramIO<C extends DatagramIOConfiguration> extends Runnable {

    /**
     * Configures the service and starts any listening sockets.
     *
     * @param bindAddress The port to bind any sockets on. 0 means choosing an ephemeral port
     * @param router The router which handles received {@link org.jupnp.model.message.IncomingDatagramMessage}s.
     * @param datagramProcessor Reads and writes datagrams.
     * @throws InitializationException If the service could not be initialized or started.
     */
    void init(InetAddress bindAddress, int bindPort, Router router, DatagramProcessor datagramProcessor)
            throws InitializationException;

    /**
     * Stops the service, closes any listening sockets.
     */
    void stop();

    /**
     * @return This service's configuration.
     */
    C getConfiguration();

    /**
     * Sends a datagram after conversion with
     * {@link org.jupnp.transport.spi.DatagramProcessor#write(org.jupnp.model.message.OutgoingDatagramMessage)}.
     *
     * @param message The message to send.
     */
    void send(OutgoingDatagramMessage message);

    /**
     * The actual sending of a UDP datagram.
     * <p>
     * Recoverable errors should be logged, if appropriate only with debug level. Any
     * non-recoverable errors should be thrown as <code>RuntimeException</code>s.
     * </p>
     *
     * @param datagram The UDP datagram to send.
     */
    void send(DatagramPacket datagram);
}
