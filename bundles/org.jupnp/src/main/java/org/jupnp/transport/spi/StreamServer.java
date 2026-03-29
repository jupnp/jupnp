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

import java.net.InetAddress;

import org.jupnp.transport.Router;

/**
 * Service for receiving TCP (HTTP) streams, one per bound IP address.
 * <p>
 * This service typically listens on a socket for TCP connections.
 * <p>
 * This listening loop is started with the <code>run()</code> method, this service is
 * <code>Runnable</code>. Then {@link Router#received(UpnpStream)} is called with a custom
 * {@link UpnpStream}. This will start processing of the request and <code>run()</code> the
 * {@link UpnpStream} (which is also <code>Runnable</code>) in a separate thread,
 * freeing up the receiving thread immediately.
 * </p>
 * <p>
 * The {@link UpnpStream} then creates a {@link org.jupnp.model.message.StreamRequestMessage}
 * and calls the {@link UpnpStream#process(org.jupnp.model.message.StreamRequestMessage)}
 * method. The {@link UpnpStream} then returns the response to the network client.
 * </p>
 * <p>
 * In pseudo-code:
 * </p>
 * 
 * <pre>
 * MyStreamServer implements StreamServer {
 *      run() {
 *          while (not stopped) {
 *              Connection con = listenToSocketAndBlock();
 *              router.received( new MyUpnpStream(con) );
 *          }
 *      }
 * }
 *
 * MyUpnpStream(con) extends UpnpStream {
 *      run() {
 *          try {
 *              StreamRequestMessage request = // ... Read request
 *              StreamResponseMessage response = process(request);
 *              // ... Send response
 *              responseSent(response))
 *          } catch (Exception e) {
 *              responseException(e);
 *          }
 *      }
 * }
 * </pre>
 * <p>
 * An implementation has to be thread-safe.
 * </p>
 *
 * @param <C> The type of the service's configuration.
 *
 * @author Christian Bauer
 */
public interface StreamServer<C extends StreamServerConfiguration> extends Runnable {

    /**
     * Configures the service and starts any listening sockets.
     *
     * @param bindAddress The address to bind any sockets on.
     * @param router The router which handles the incoming {@link org.jupnp.transport.spi.UpnpStream}.
     * @throws InitializationException If the service could not be initialized or started.
     */
    void init(InetAddress bindAddress, Router router) throws InitializationException;

    /**
     * This method will be called potentially right after
     * {@link #init(java.net.InetAddress, org.jupnp.transport.Router)}, the
     * actual assigned local port must be available before the server is started.
     *
     * @return The TCP port this service is listening on, e.g. the actual ephemeral port.
     */
    int getPort();

    /**
     * Stops the service, closes any listening sockets.
     */
    void stop();

    /**
     * @return This service's configuration.
     */
    C getConfiguration();
}
