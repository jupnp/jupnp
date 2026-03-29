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

import java.net.NetworkInterface;

import org.jupnp.transport.Router;

/**
 * Service for receiving multicast UDP datagrams, one per bound network interface.
 * <p>
 * This services typically listens on a socket for UDP datagrams, the socket has joined
 * the configured multicast group.
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
 * An implementation has to be thread-safe.
 * </p>
 * 
 * @param <C> The type of the service's configuration.
 *
 * @author Christian Bauer
 */
public interface MulticastReceiver<C extends MulticastReceiverConfiguration> extends Runnable {

    /**
     * Configures the service and starts any listening sockets.
     *
     * @param networkInterface The network interface on which to join the multicast group on.
     * @param router The router which handles received {@link org.jupnp.model.message.IncomingDatagramMessage}s.
     * @param networkAddressFactory The network address factory to use for local address lookup given a local interface
     *            and a remote address.
     * @param datagramProcessor Reads and writes datagrams.
     * @throws InitializationException If the service could not be initialized or started.
     */
    void init(NetworkInterface networkInterface, Router router, NetworkAddressFactory networkAddressFactory,
            DatagramProcessor datagramProcessor) throws InitializationException;

    /**
     * Stops the service, closes any listening sockets.
     */
    void stop();

    /**
     * @return This service's configuration.
     */
    C getConfiguration();
}
