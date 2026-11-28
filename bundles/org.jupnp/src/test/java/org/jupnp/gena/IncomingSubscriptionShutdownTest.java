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
package org.jupnp.gena;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.mock.MockUpnpServiceConfiguration;
import org.jupnp.model.Namespace;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.OutgoingSubscribeResponseMessage;
import org.jupnp.model.message.header.CallbackHeader;
import org.jupnp.model.message.header.NTEventHeader;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.protocol.sync.ReceivingSubscribe;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryImpl;
import org.jupnp.registry.RegistryMaintainer;
import org.jupnp.transport.Router;
import org.jupnp.util.URIUtil;

/**
 * The service of a local device belongs to the application and outlives the UPnP stack, an incoming subscription
 * must not stay registered as a listener on it when the registry is shut down.
 */
class IncomingSubscriptionShutdownTest {

    @Test
    void subscriptionEndsOnRegistryShutdown() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Register local device and its service
        LocalDevice device = GenaSampleData.createTestDevice(GenaSampleData.LocalTestService.class);
        upnpService.getRegistry().addDevice(device);

        Namespace ns = upnpService.getConfiguration().getNamespace();

        LocalService<?> service = SampleData.getFirstService(device);
        URL callbackURL = URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), ns.getEventCallbackPath(service));

        StreamRequestMessage subscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.SUBSCRIBE,
                ns.getEventSubscriptionPath(service));

        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.CALLBACK, new CallbackHeader(callbackURL));
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.NT, new NTEventHeader());

        ReceivingSubscribe subscribeProt = new ReceivingSubscribe(upnpService, subscribeRequestMessage);
        subscribeProt.run();
        OutgoingSubscribeResponseMessage subscribeResponseMessage = subscribeProt.getOutputMessage();
        String subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();

        // Initial event, followed by an event for a "modified" state of the service
        subscribeProt.responseSent(subscribeResponseMessage);
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
        assertEquals(2, upnpService.getRouter().getSentStreamRequestMessages().size());

        upnpService.getRegistry().shutdown();
        assertNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));

        // The service is still there and its state changes again, nobody must be listening anymore
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", true, false);
        assertEquals(2, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    @Test
    void continuesShutdownWhenApplicationCallbacksThrow() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        LocalDevice device = GenaSampleData.createTestDevice();
        upnpService.getRegistry().addDevice(device);
        LocalService<?> service = SampleData.getFirstService(device);
        AtomicInteger ended = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            LocalGENASubscription subscription = new LocalGENASubscription(service, 1800, List.of()) {
                @Override
                public void established() {
                }

                @Override
                public void eventReceived() {
                    fail("The local subscription should have been removed");
                }

                @Override
                public void ended(CancelReason reason) {
                    ended.incrementAndGet();
                    throw new IllegalStateException("Application callback failed");
                }
            };
            subscription.registerOnService();
            upnpService.getRegistry().addLocalSubscription(subscription);
        }

        assertDoesNotThrow(upnpService::shutdown);
        assertEquals(2, ended.get());
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
    }

    @Test
    void localEventCanRequestShutdownWhileAnotherThreadStopsTheService() throws Exception {
        CountDownLatch eventStarted = new CountDownLatch(1);
        CountDownLatch shutdownStarted = new CountDownLatch(1);
        AtomicInteger routerShutdowns = new AtomicInteger();
        UpnpServiceImpl upnpService = new TestUpnpService() {
            @Override
            protected void shutdownRegistry() {
                shutdownStarted.countDown();
                super.shutdownRegistry();
            }

            @Override
            protected void shutdownRouter() {
                super.shutdownRouter();
                routerShutdowns.incrementAndGet();
            }
        };
        upnpService.startup();
        LocalDevice device = GenaSampleData.createTestDevice();
        upnpService.getRegistry().addDevice(device);
        LocalService<?> service = SampleData.getFirstService(device);
        LocalGENASubscription subscription = new LocalGENASubscription(service, 1800, List.of()) {
            @Override
            public void established() {
            }

            @Override
            public void ended(CancelReason reason) {
            }

            @Override
            public void eventReceived() {
                eventStarted.countDown();
                try {
                    assertTrue(shutdownStarted.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail(e);
                }
                upnpService.shutdown();
            }
        };
        upnpService.getRegistry().addLocalSubscription(subscription);
        subscription.registerOnService();

        CompletableFuture<Void> event = CompletableFuture.runAsync(
                () -> service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true));
        assertTrue(eventStarted.await(5, TimeUnit.SECONDS));
        CompletableFuture<Void> shutdown = CompletableFuture.runAsync(upnpService::shutdown);
        CompletableFuture.allOf(event, shutdown).get(5, TimeUnit.SECONDS);

        assertEquals(1, routerShutdowns.get());
        assertNull(upnpService.getRegistry().getLocalSubscription(subscription.getSubscriptionId()));
    }

    @Test
    void startsFreshServiceOnceAfterConcurrentStartupRequestsDuringShutdown() throws Exception {
        CountDownLatch shutdownStarted = new CountDownLatch(1);
        CountDownLatch finishShutdown = new CountDownLatch(1);
        AtomicInteger registriesCreated = new AtomicInteger();
        UpnpServiceImpl upnpService = new TestUpnpService() {
            @Override
            protected Registry createRegistry(ProtocolFactory protocol) {
                registriesCreated.incrementAndGet();
                return super.createRegistry(protocol);
            }

            @Override
            protected void shutdownRegistry() {
                shutdownStarted.countDown();
                try {
                    assertTrue(finishShutdown.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail(e);
                }
                super.shutdownRegistry();
            }
        };
        upnpService.startup();
        Registry oldRegistry = upnpService.getRegistry();
        Router oldRouter = upnpService.getRouter();

        CompletableFuture<Void> shutdown = CompletableFuture.runAsync(upnpService::shutdown);
        try {
            assertTrue(shutdownStarted.await(5, TimeUnit.SECONDS));
            CompletableFuture.runAsync(() -> {
                upnpService.startup();
                upnpService.startup();
            }).get(5, TimeUnit.SECONDS);
            assertSame(oldRegistry, upnpService.getRegistry());
            assertSame(oldRouter, upnpService.getRouter());
        } finally {
            finishShutdown.countDown();
        }
        shutdown.get(5, TimeUnit.SECONDS);

        assertEquals(2, registriesCreated.get());
        assertNotSame(oldRegistry, upnpService.getRegistry());
        assertNotSame(oldRouter, upnpService.getRouter());
        assertSame(upnpService.getRegistry(), upnpService.getControlPoint().getRegistry());
        upnpService.shutdown();
    }

    @Test
    void localSubscriptionCallbackEndsOnRegistryShutdown() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Register local device and its service
        LocalDevice device = GenaSampleData.createTestDevice(GenaSampleData.LocalTestService.class);
        upnpService.getRegistry().addDevice(device);

        LocalService<?> service = SampleData.getFirstService(device);

        List<CancelReason> endedReasons = new ArrayList<>();
        List<Long> receivedSequences = new ArrayList<>();

        SubscriptionCallback callback = new SubscriptionCallback(service) {

            @Override
            protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
                    String defaultMsg) {
                fail(defaultMsg);
            }

            @Override
            public void established(GENASubscription subscription) {
            }

            @Override
            public void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus) {
                assertFalse(Thread.holdsLock(subscription));
                endedReasons.add(reason);
            }

            @Override
            public void eventReceived(GENASubscription subscription) {
                receivedSequences.add(subscription.getCurrentSequence().getValue());
            }

            @Override
            public void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
            }

            @Override
            protected void invalidMessage(RemoteGENASubscription remoteGENASubscription, UnsupportedDataException e) {
            }
        };

        upnpService.getControlPoint().execute(callback);

        // Initial event, followed by an event for a "modified" state of the service
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
        assertEquals(List.of(0L, 1L), receivedSequences);
        assertTrue(endedReasons.isEmpty());

        upnpService.getRegistry().shutdown();
        assertEquals(List.of(CancelReason.DEVICE_WAS_REMOVED), endedReasons);
        assertNull(callback.getSubscription());

        // The service is still there and its state changes again, nobody must be listening anymore
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", true, false);
        assertEquals(List.of(0L, 1L), receivedSequences);
    }

    private static class TestUpnpService extends UpnpServiceImpl {
        TestUpnpService() {
            super(new MockUpnpServiceConfiguration());
        }

        @Override
        protected Registry createRegistry(ProtocolFactory protocol) {
            return new RegistryImpl(this) {
                @Override
                protected RegistryMaintainer createRegistryMaintainer() {
                    return null;
                }
            };
        }

        @Override
        protected Router createRouter(ProtocolFactory protocol, Registry registry) {
            return new MockRouter(getConfiguration(), protocol);
        }
    }
}
