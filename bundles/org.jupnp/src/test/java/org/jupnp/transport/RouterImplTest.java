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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.data.SampleData;
import org.jupnp.model.Location;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.discovery.OutgoingSearchRequest;
import org.jupnp.model.message.discovery.OutgoingSearchResponseRootDevice;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * Tests for {@link RouterImpl}.
 *
 * @author Holger Friedrich - Initial contribution
 */
class RouterImplTest {

    /**
     * When enabling fails after the multicast receivers were already started (e.g. the only stream server can't
     * be initialized and its address is removed), the receivers must be stopped. Otherwise they keep receiving
     * datagrams for a router that never got enabled, and disable() - which only cleans up an enabled router -
     * would never stop them.
     */
    @Test
    void enableFailureStopsAlreadyStartedMulticastReceivers() throws Exception {
        AtomicInteger receiverStops = new AtomicInteger();
        TestConfiguration configuration = new TestConfiguration(loopbackInterface(), receiverStops) {
            @Override
            public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
                return proxy(StreamServer.class, (proxy, method, args) -> {
                    if ("init".equals(method.getName())) {
                        throw new InitializationException("stream server not available", new BindException("in use"));
                    }
                    return null;
                });
            }
        };
        try {
            RouterImpl router = new RouterImpl(configuration, null);

            assertFalse(router.enable());
            assertFalse(router.isEnabled());
            assertEquals(1, receiverStops.get(), "multicast receiver started before the failure must be stopped");

            // a later disable() of the never enabled router must neither fail nor stop anything twice
            assertFalse(router.disable());
            assertEquals(1, receiverStops.get());
        } finally {
            configuration.shutdown();
        }
    }

    /**
     * Any failure after the transports were started must stop them, and a transport failing to stop must neither
     * prevent the others from being stopped nor hide the original failure.
     */
    @Test
    void failingStopDoesNotPreventCleanupAfterUnexpectedEnableFailure() throws Exception {
        AtomicInteger receiverStops = new AtomicInteger();
        RuntimeException failure = new IllegalStateException("stream client not available");
        TestConfiguration configuration = new TestConfiguration(loopbackInterface(), receiverStops) {
            @Override
            public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
                return proxy(StreamServer.class, (proxy, method, args) -> {
                    if ("stop".equals(method.getName())) {
                        throw new IllegalStateException("stream server can't be stopped");
                    }
                    return null;
                });
            }

            @Override
            public StreamClient createStreamClient() {
                throw failure;
            }
        };
        try {
            RouterImpl router = new RouterImpl(configuration, null);

            assertSame(failure, assertThrows(IllegalStateException.class, router::enable));
            assertFalse(router.isEnabled());
            assertEquals(1, receiverStops.get(),
                    "multicast receiver must be stopped despite the failing stream server");
        } finally {
            configuration.shutdown();
        }
    }

    /**
     * Multicast datagrams have to be sent on all bound addresses to reach every network. A unicast datagram, such
     * as a search response, must be sent only once and from the address the search was received on. Otherwise its
     * destination, which is the unverified source address of the search, receives one copy for each bound address.
     */
    @Test
    void unicastDatagramIsSentOnceAndMulticastDatagramOnAllBoundAddresses() throws Exception {
        List<InetAddress> bindAddresses = List.of(InetAddress.getByName("10.99.0.1"),
                InetAddress.getByName("10.99.1.1"), InetAddress.getByName("10.99.2.1"));
        List<InetAddress> sentFrom = new ArrayList<>();
        TestConfiguration configuration = new TestConfiguration(loopbackInterface(), new AtomicInteger()) {
            @Override
            protected List<InetAddress> getBindAddresses() {
                return bindAddresses;
            }

            @Override
            public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
                return proxy(StreamServer.class, (proxy, method, args) -> null);
            }

            @Override
            public StreamClient createStreamClient() {
                return proxy(StreamClient.class, (proxy, method, args) -> null);
            }

            @Override
            public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
                AtomicReference<InetAddress> bindAddress = new AtomicReference<>();
                return proxy(DatagramIO.class, (proxy, method, args) -> {
                    if ("init".equals(method.getName())) {
                        bindAddress.set((InetAddress) args[0]);
                    } else if ("send".equals(method.getName())) {
                        sentFrom.add(bindAddress.get());
                    }
                    return null;
                });
            }
        };
        try {
            RouterImpl router = new RouterImpl(configuration, null);
            assertTrue(router.enable());

            router.send(new OutgoingSearchRequest(new STAllHeader(), 1));
            assertEquals(3, sentFrom.size());
            assertEquals(Set.copyOf(bindAddresses), Set.copyOf(sentFrom));

            LocalDevice localDevice = SampleData.createLocalDevice();
            for (InetAddress receivedOnAddress : bindAddresses) {
                sentFrom.clear();
                router.send(createSearchResponse(receivedOnAddress, localDevice));
                assertEquals(List.of(receivedOnAddress), sentFrom);
            }

            // no datagram I/O is bound to the address the search was received on
            sentFrom.clear();
            router.send(createSearchResponse(InetAddress.getByName("10.99.3.1"), localDevice));
            assertEquals(1, sentFrom.size());

            assertTrue(router.disable());
        } finally {
            configuration.shutdown();
        }
    }

    private static OutgoingSearchResponseRootDevice createSearchResponse(InetAddress receivedOnAddress,
            LocalDevice localDevice) throws Exception {
        IncomingDatagramMessage<UpnpRequest> searchRequest = new IncomingDatagramMessage<>(
                new UpnpRequest(UpnpRequest.Method.MSEARCH), InetAddress.getByName("10.99.0.2"), 1900,
                receivedOnAddress);
        return new OutgoingSearchResponseRootDevice(searchRequest,
                new Location(new NetworkAddress(receivedOnAddress, 8080), "/desc"), localDevice);
    }

    private static NetworkInterface loopbackInterface() throws SocketException {
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        assumeTrue(networkInterface != null, "loopback interface required for this test");
        return networkInterface;
    }

    /**
     * One network interface and the loopback bind address (unless overridden), a multicast receiver counting its
     * stops and no datagram I/O.
     */
    private static class TestConfiguration extends DefaultUpnpServiceConfiguration {
        private final NetworkInterface networkInterface;
        private final AtomicInteger receiverStops;

        TestConfiguration(NetworkInterface networkInterface, AtomicInteger receiverStops) {
            this.networkInterface = networkInterface;
            this.receiverStops = receiverStops;
        }

        protected List<InetAddress> getBindAddresses() {
            return List.of(InetAddress.getLoopbackAddress());
        }

        @Override
        public NetworkAddressFactory createNetworkAddressFactory() {
            List<NetworkInterface> interfaces = new ArrayList<>(List.of(networkInterface));
            List<InetAddress> bindAddresses = new ArrayList<>(getBindAddresses());
            return proxy(NetworkAddressFactory.class, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "getNetworkInterfaces":
                        return interfaces.iterator();
                    case "getBindAddresses":
                        return bindAddresses.iterator();
                    case "hasUsableNetwork":
                        return !bindAddresses.isEmpty();
                    case "getMulticastResponsePort":
                    case "getMulticastPort":
                    case "getStreamListenPort":
                        return 0;
                    default:
                        return null;
                }
            });
        }

        @Override
        public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
            return proxy(MulticastReceiver.class, (proxy, method, args) -> {
                if ("stop".equals(method.getName())) {
                    receiverStops.incrementAndGet();
                }
                return null;
            });
        }

        @Override
        public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
            return null;
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(RouterImplTest.class.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        } else if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        return type.getSimpleName() + "Proxy";
                    }
                    return handler.invoke(proxy, method, args);
                }));
    }
}
