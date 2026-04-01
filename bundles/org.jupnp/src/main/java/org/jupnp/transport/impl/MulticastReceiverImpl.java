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

import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;

import org.jupnp.model.UnsupportedDataException;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation based on a UDP <code>MulticastSocket</code>.
 * <p>
 * Thread-safety is guaranteed through synchronization of methods of this service and
 * by the thread-safe underlying socket.
 * </p>
 * 
 * @author Christian Bauer
 */
public class MulticastReceiverImpl implements MulticastReceiver<MulticastReceiverConfigurationImpl> {

    private final Logger logger = LoggerFactory.getLogger(MulticastReceiver.class);

    protected final MulticastReceiverConfigurationImpl configuration;

    protected Router router;
    protected NetworkAddressFactory networkAddressFactory;
    protected DatagramProcessor datagramProcessor;

    protected NetworkInterface multicastInterface;
    protected InetSocketAddress multicastAddress;
    protected MulticastSocket socket;

    public MulticastReceiverImpl(MulticastReceiverConfigurationImpl configuration) {
        this.configuration = configuration;
    }

    @Override
    public MulticastReceiverConfigurationImpl getConfiguration() {
        return configuration;
    }

    @Override
    public synchronized void init(NetworkInterface networkInterface, Router router,
            NetworkAddressFactory networkAddressFactory, DatagramProcessor datagramProcessor)
            throws InitializationException {

        this.router = router;
        this.networkAddressFactory = networkAddressFactory;
        this.datagramProcessor = datagramProcessor;
        this.multicastInterface = networkInterface;

        try {

            logger.debug("Creating wildcard socket (for receiving multicast datagrams) on port: {}",
                    configuration.getPort());
            multicastAddress = new InetSocketAddress(configuration.getGroup(), configuration.getPort());

            socket = new MulticastSocket(configuration.getPort());
            socket.setReuseAddress(true);
            socket.setReceiveBufferSize(32768); // Keep a backlog of incoming datagrams if we are not fast enough

            logger.debug("Joining multicast group: {} on network interface: {}", multicastAddress,
                    multicastInterface.getDisplayName());
            socket.joinGroup(multicastAddress, multicastInterface);

        } catch (Exception e) {
            throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + e);
        }
    }

    @Override
    public synchronized void stop() {
        if (socket != null && !socket.isClosed()) {
            try {
                logger.debug("Leaving multicast group");
                socket.leaveGroup(multicastAddress, multicastInterface);
                // Well this doesn't work and I have no idea why I get "java.net.SocketException: Can't assign requested
                // address"
            } catch (Exception e) {
                logger.debug("Could not leave multicast group", e);
            }
            // So... just close it and ignore the log messages
            socket.close();
        }
    }

    @Override
    public void run() {

        logger.debug("Entering blocking receiving loop, listening for UDP datagrams on: {}", socket.getLocalAddress());
        while (true) {

            try {
                byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
                DatagramPacket datagram = new DatagramPacket(buf, buf.length);

                socket.receive(datagram);

                InetAddress receivedOnLocalAddress = networkAddressFactory.getLocalAddress(multicastInterface,
                        multicastAddress.getAddress() instanceof Inet6Address, datagram.getAddress());

                logger.debug("UDP datagram received from: {}:{} on local interface: {} and address: {}",
                        datagram.getAddress().getHostAddress(), datagram.getPort(), multicastInterface.getDisplayName(),
                        receivedOnLocalAddress.getHostAddress());

                router.received(datagramProcessor.read(receivedOnLocalAddress, datagram));

            } catch (SocketException e) {
                logger.debug("Socket closed");
                break;
            } catch (UnsupportedDataException e) {
                logger.info("Could not read datagram: {}", e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            if (!socket.isClosed()) {
                logger.debug("Closing multicast socket");
                socket.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
