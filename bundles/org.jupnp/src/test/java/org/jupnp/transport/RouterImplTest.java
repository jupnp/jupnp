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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.BindException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.jupnp.DefaultUpnpServiceConfiguration;
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

    private static NetworkInterface loopbackInterface() throws SocketException {
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
        assumeTrue(networkInterface != null, "loopback interface required for this test");
        return networkInterface;
    }

    /**
     * One network interface and the loopback bind address, a multicast receiver counting its stops and no
     * datagram I/O.
     */
    private static class TestConfiguration extends DefaultUpnpServiceConfiguration {
        private final NetworkInterface networkInterface;
        private final AtomicInteger receiverStops;

        TestConfiguration(NetworkInterface networkInterface, AtomicInteger receiverStops) {
            this.networkInterface = networkInterface;
            this.receiverStops = receiverStops;
        }

        @Override
        public NetworkAddressFactory createNetworkAddressFactory() {
            List<NetworkInterface> interfaces = new ArrayList<>(List.of(networkInterface));
            List<InetAddress> bindAddresses = new ArrayList<>(List.of(InetAddress.getLoopbackAddress()));
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
