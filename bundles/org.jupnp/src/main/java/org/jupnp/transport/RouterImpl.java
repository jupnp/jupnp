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

import java.net.BindException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.protocol.ProtocolCreationException;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.protocol.ReceivingAsync;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.NoNetworkException;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.transport.spi.UpnpStream;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of network message router.
 * <p>
 * Initializes and starts listening for data on the network when enabled.
 * </p>
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - added multicast response port
 */
public class RouterImpl implements Router {

    private final Logger logger = LoggerFactory.getLogger(Router.class);

    protected UpnpServiceConfiguration configuration;
    protected ProtocolFactory protocolFactory;

    protected volatile boolean enabled;
    protected ReentrantReadWriteLock routerLock = new ReentrantReadWriteLock(true);
    protected Lock readLock = routerLock.readLock();
    protected Lock writeLock = routerLock.writeLock();

    // These are created/destroyed when the router is enabled/disabled
    protected NetworkAddressFactory networkAddressFactory;
    protected StreamClient streamClient;
    protected final Map<NetworkInterface, MulticastReceiver> multicastReceivers = new HashMap<>();
    protected final Map<InetAddress, DatagramIO> datagramIOs = new HashMap<>();
    protected final Map<InetAddress, StreamServer> streamServers = new HashMap<>();

    protected RouterImpl() {
    }

    /**
     * @param configuration The configuration used by this router.
     * @param protocolFactory The protocol factory used by this router.
     */
    public RouterImpl(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory) {
        this.configuration = configuration;
        this.protocolFactory = protocolFactory;
    }

    public boolean enable(EnableRouter event) throws RouterException {
        return enable();
    }

    public boolean disable(DisableRouter event) throws RouterException {
        return disable();
    }

    @Override
    public UpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    /**
     * Initializes listening services: First an instance of {@link org.jupnp.transport.spi.MulticastReceiver}
     * is bound to each network interface. Then an instance of {@link org.jupnp.transport.spi.DatagramIO} and
     * {@link org.jupnp.transport.spi.StreamServer} is bound to each bind address returned by the network
     * address factory, respectively. There is only one instance of
     * {@link org.jupnp.transport.spi.StreamClient} created and managed by this router.
     */
    @Override
    public boolean enable() throws RouterException {
        lock(writeLock);
        try {
            if (!enabled) {
                try {
                    logger.debug("Starting networking services...");
                    networkAddressFactory = getConfiguration().createNetworkAddressFactory();

                    startInterfaceBasedTransports(networkAddressFactory.getNetworkInterfaces());
                    startAddressBasedTransports(networkAddressFactory.getBindAddresses());

                    // The transports possibly removed some unusable network interfaces/addresses
                    if (!networkAddressFactory.hasUsableNetwork()) {
                        throw new NoNetworkException(
                                "No usable network interface and/or addresses available, check the log for errors.");
                    }

                    // Start the HTTP client last, we don't even have to try if there is no network
                    streamClient = getConfiguration().createStreamClient();

                    enabled = true;
                    return true;
                } catch (InitializationException e) {
                    handleStartFailure(e);
                }
            }
            return false;
        } finally {
            unlock(writeLock);
        }
    }

    @Override
    public boolean disable() throws RouterException {
        lock(writeLock);
        try {
            if (enabled) {
                logger.debug("Disabling network services...");

                if (streamClient != null) {
                    logger.debug("Stopping stream client connection management/pool");
                    streamClient.stop();
                    streamClient = null;
                }

                for (Map.Entry<InetAddress, StreamServer> entry : streamServers.entrySet()) {
                    logger.debug("Stopping stream server on address: {}", entry.getKey());
                    entry.getValue().stop();
                }
                streamServers.clear();

                for (Map.Entry<NetworkInterface, MulticastReceiver> entry : multicastReceivers.entrySet()) {
                    logger.debug("Stopping multicast receiver on interface: {}", entry.getKey().getDisplayName());
                    entry.getValue().stop();
                }
                multicastReceivers.clear();

                for (Map.Entry<InetAddress, DatagramIO> entry : datagramIOs.entrySet()) {
                    logger.debug("Stopping datagram I/O on address: {}", entry.getKey());
                    entry.getValue().stop();
                }
                datagramIOs.clear();

                networkAddressFactory = null;
                enabled = false;
                return true;
            }
            return false;
        } finally {
            unlock(writeLock);
        }
    }

    @Override
    public void shutdown() throws RouterException {
        disable();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void handleStartFailure(InitializationException e) throws InitializationException {
        if (e instanceof NoNetworkException) {
            logger.info("Unable to initialize network router, no network found.");
        } else {
            logger.error("Unable to initialize network router", e);
        }
    }

    @Override
    public List<NetworkAddress> getActiveStreamServers(InetAddress preferredAddress) throws RouterException {
        lock(readLock);
        try {
            if (enabled && !streamServers.isEmpty()) {
                List<NetworkAddress> streamServerAddresses = new ArrayList<>();

                StreamServer preferredServer;
                if (preferredAddress != null && (preferredServer = streamServers.get(preferredAddress)) != null) {
                    streamServerAddresses.add(new NetworkAddress(preferredAddress, preferredServer.getPort(),
                            networkAddressFactory.getHardwareAddress(preferredAddress)

                    ));
                    return streamServerAddresses;
                }

                for (Map.Entry<InetAddress, StreamServer> entry : streamServers.entrySet()) {
                    byte[] hardwareAddress = networkAddressFactory.getHardwareAddress(entry.getKey());
                    streamServerAddresses
                            .add(new NetworkAddress(entry.getKey(), entry.getValue().getPort(), hardwareAddress));
                }
                return streamServerAddresses;
            } else {
                return List.of();
            }
        } finally {
            unlock(readLock);
        }
    }

    /**
     * Obtains the asynchronous protocol {@code Executor} and runs the protocol created
     * by the {@link org.jupnp.protocol.ProtocolFactory} for the given message.
     * <p>
     * If the factory doesn't create a protocol, the message is dropped immediately without
     * creating another thread or consuming further resources. This means we can filter the
     * datagrams in the protocol factory and e.g. completely disable discovery or only
     * allow notification message from some known services we'd like to work with.
     * </p>
     *
     * @param msg The received datagram message.
     */
    @Override
    public void received(IncomingDatagramMessage msg) {
        if (!enabled) {
            logger.debug("Router disabled, ignoring incoming message: {}", msg);
            return;
        }
        try {
            ReceivingAsync protocol = getProtocolFactory().createReceivingAsync(msg);
            if (protocol == null) {
                logger.trace("No protocol, ignoring received message: {}", msg);
                return;
            }
            logger.debug("Received asynchronous message: {}", msg);
            getConfiguration().getRemoteListenerExecutor().execute(protocol);
        } catch (ProtocolCreationException e) {
            logger.warn("Handling received datagram failed", e);
        }
    }

    /**
     * Obtains the synchronous protocol {@code Executor} and runs the
     * {@link org.jupnp.transport.spi.UpnpStream} directly.
     *
     * @param stream The received {@link org.jupnp.transport.spi.UpnpStream}.
     */
    @Override
    public void received(UpnpStream stream) {
        if (!enabled) {
            logger.debug("Router disabled, ignoring incoming: {}", stream);
            return;
        }
        logger.debug("Received synchronous stream: {}", stream);
        getConfiguration().getSyncProtocolExecutorService().execute(stream);
    }

    /**
     * Sends the UDP datagram on all bound {@link org.jupnp.transport.spi.DatagramIO}s.
     *
     * @param msg The UDP datagram message to send.
     */
    @Override
    public void send(OutgoingDatagramMessage msg) throws RouterException {
        lock(readLock);
        try {
            if (enabled) {
                for (DatagramIO datagramIO : datagramIOs.values()) {
                    datagramIO.send(msg);
                }
            } else {
                logger.debug("Router disabled, not sending datagram: {}", msg);
            }
        } finally {
            unlock(readLock);
        }
    }

    /**
     * Sends the TCP stream request with the {@link org.jupnp.transport.spi.StreamClient}.
     *
     * @param msg The TCP (HTTP) stream message to send.
     * @return The return value of the {@link org.jupnp.transport.spi.StreamClient#sendRequest(StreamRequestMessage)}
     *         method or <code>null</code> if no <code>StreamClient</code> is available.
     */
    @Override
    public StreamResponseMessage send(StreamRequestMessage msg) throws RouterException {
        lock(readLock);
        try {
            if (enabled) {
                if (streamClient == null) {
                    logger.debug("No StreamClient available, not sending: {}", msg);
                    return null;
                }
                logger.debug("Sending via TCP unicast stream: {}", msg);
                try {
                    return streamClient.sendRequest(msg);
                } catch (InterruptedException e) {
                    throw new RouterException("Sending stream request was interrupted", e);
                }
            } else {
                logger.debug("Router disabled, not sending stream request: {}", msg);
                return null;
            }
        } finally {
            unlock(readLock);
        }
    }

    /**
     * Sends the given bytes as a broadcast on all bound {@link org.jupnp.transport.spi.DatagramIO}s,
     * using source port 9.
     * <p>
     * TODO: Support source port parameter
     * </p>
     *
     * @param bytes The byte payload of the UDP datagram.
     */
    @Override
    public void broadcast(byte[] bytes) throws RouterException {
        lock(readLock);
        try {
            if (enabled) {
                for (Map.Entry<InetAddress, DatagramIO> entry : datagramIOs.entrySet()) {
                    InetAddress broadcast = networkAddressFactory.getBroadcastAddress(entry.getKey());
                    if (broadcast != null) {
                        logger.debug("Sending UDP datagram to broadcast address: {}", broadcast.getHostAddress());
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, broadcast, 9);
                        entry.getValue().send(packet);
                    }
                }
            } else {
                logger.debug("Router disabled, not broadcasting bytes: {}", bytes.length);
            }
        } finally {
            unlock(readLock);
        }
    }

    protected void startInterfaceBasedTransports(Iterator<NetworkInterface> interfaces) throws InitializationException {
        while (interfaces.hasNext()) {
            NetworkInterface networkInterface = interfaces.next();

            // We only have the MulticastReceiver as an interface-based transport
            MulticastReceiver multicastReceiver = getConfiguration().createMulticastReceiver(networkAddressFactory);
            if (multicastReceiver == null) {
                logger.info("Configuration did not create a MulticastReceiver for: {}", networkInterface);
            } else {
                logger.debug("Init multicast receiver on interface: {}", networkInterface.getDisplayName());
                multicastReceiver.init(networkInterface, this, networkAddressFactory,
                        getConfiguration().getDatagramProcessor());

                multicastReceivers.put(networkInterface, multicastReceiver);
            }
        }

        for (Map.Entry<NetworkInterface, MulticastReceiver> entry : multicastReceivers.entrySet()) {
            logger.debug("Starting multicast receiver on interface: {}", entry.getKey().getDisplayName());
            getConfiguration().getMulticastReceiverExecutor().execute(entry.getValue());
        }
    }

    protected void startAddressBasedTransports(Iterator<InetAddress> addresses) throws InitializationException {
        while (addresses.hasNext()) {
            InetAddress address = addresses.next();

            // HTTP servers
            StreamServer streamServer = getConfiguration().createStreamServer(networkAddressFactory);
            if (streamServer == null) {
                logger.info("Configuration did not create a StreamServer for: {}", address);
            } else {
                try {
                    logger.debug("Init stream server on address: {}", address);
                    streamServer.init(address, this);
                    streamServers.put(address, streamServer);
                } catch (InitializationException e) {
                    // Try to recover
                    Throwable cause = Exceptions.unwrap(e);
                    if (cause instanceof BindException) {
                        logger.warn("Failed to init StreamServer. Removing unusable address: {}", address, cause);
                        addresses.remove();
                        continue; // Don't try anything else with this address
                    }
                    throw e;
                }
            }

            // Datagram I/O
            DatagramIO datagramIO = getConfiguration().createDatagramIO(networkAddressFactory);
            if (datagramIO == null) {
                logger.info("Configuration did not create a StreamServer for: {}", address);
            } else {
                logger.debug("Init datagram I/O on address: {}", address);
                datagramIO.init(address, networkAddressFactory.getMulticastResponsePort(), this,
                        getConfiguration().getDatagramProcessor());
                datagramIOs.put(address, datagramIO);
            }
        }

        for (Map.Entry<InetAddress, StreamServer> entry : streamServers.entrySet()) {
            logger.debug("Starting stream server on address: {}", entry.getKey());
            getConfiguration().getStreamServerExecutorService().execute(entry.getValue());
        }

        for (Map.Entry<InetAddress, DatagramIO> entry : datagramIOs.entrySet()) {
            logger.debug("Starting datagram I/O on address: {}", entry.getKey());
            getConfiguration().getDatagramIOExecutor().execute(entry.getValue());
        }
    }

    protected void lock(Lock lock, int timeoutMilliseconds) throws RouterException {
        try {
            logger.trace("Trying to obtain lock with timeout milliseconds '{}': {}", timeoutMilliseconds,
                    lock.getClass().getSimpleName());
            if (lock.tryLock(timeoutMilliseconds, TimeUnit.MILLISECONDS)) {
                logger.trace("Acquired router lock: {}", lock.getClass().getSimpleName());
            } else {
                throw new RouterException("Router wasn't available exclusively after waiting " + timeoutMilliseconds
                        + "ms, lock failed: " + lock.getClass().getSimpleName());
            }
        } catch (InterruptedException e) {
            throw new RouterException(
                    "Interruption while waiting for exclusive access: " + lock.getClass().getSimpleName(), e);
        }
    }

    protected void lock(Lock lock) throws RouterException {
        lock(lock, getLockTimeoutMillis());
    }

    protected void unlock(Lock lock) {
        logger.trace("Releasing router lock: {}", lock.getClass().getSimpleName());
        lock.unlock();
    }

    /**
     * @return Defaults to 6 seconds, should be longer than it takes the router to be enabled/disabled.
     */
    protected int getLockTimeoutMillis() {
        return 6000;
    }
}
