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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.controlpoint.ControlPointImpl;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.registry.RegistryImpl;
import org.jupnp.registry.RegistryMaintainer;

class LocalSubscriptionCallbackTest {

    private MockUpnpService upnpService;
    private LocalDevice device;
    private final List<GENASubscription> established = new ArrayList<>();
    private final List<GENASubscription> events = new ArrayList<>();
    private final List<GENASubscription> ended = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        upnpService = new MockUpnpService();
        upnpService.startup();
        device = GenaSampleData.createTestDevice();
    }

    @Test
    void skipsInitialEventWhenSubscriptionEndsBeforeEstablishment() {
        RegistryImpl registry = new TestRegistry(upnpService) {
            @Override
            public void addLocalSubscription(LocalGENASubscription subscription) {
                super.addLocalSubscription(subscription);
                removeLocalSubscription(subscription);
                subscription.end(CancelReason.DEVICE_WAS_REMOVED);
            }
        };
        SubscriptionCallback callback = newCallback(registry);

        callback.run();

        assertTrue(established.isEmpty());
        assertTrue(events.isEmpty());
        assertEquals(1, ended.size());
        assertNull(callback.getSubscription());
    }

    @Test
    void endingAnOldSubscriptionKeepsItsReplacement() {
        AtomicReference<Runnable> onRemove = new AtomicReference<>();
        RegistryImpl registry = new TestRegistry(upnpService) {
            @Override
            public boolean removeLocalSubscription(LocalGENASubscription subscription) {
                boolean removed = super.removeLocalSubscription(subscription);
                Runnable replace = onRemove.getAndSet(null);
                if (replace != null) {
                    replace.run();
                }
                return removed;
            }
        };
        SubscriptionCallback callback = newCallback(registry);
        callback.run();
        GENASubscription first = callback.getSubscription();
        onRemove.set(callback::run);

        callback.end();

        assertEquals(2, established.size());
        GENASubscription replacement = established.get(1);
        assertSame(replacement, callback.getSubscription());
        assertEquals(List.of(first), ended);
        assertEquals(List.of(first, replacement), events);

        callback.end();
        assertNull(registry.getLocalSubscription(replacement.getSubscriptionId()));
        assertEquals(List.of(first, replacement), ended);
    }

    private SubscriptionCallback newCallback(RegistryImpl registry) {
        registry.addDevice(device);
        SubscriptionCallback callback = new SubscriptionCallback(device.getServices()[0]) {
            @Override
            protected void failed(GENASubscription sub, UpnpResponse response, Exception error, String message) {
                fail(message, error);
            }

            @Override
            protected void established(GENASubscription sub) {
                established.add(sub);
            }

            @Override
            protected void eventReceived(GENASubscription sub) {
                events.add(sub);
            }

            @Override
            protected void ended(GENASubscription sub, CancelReason reason, UpnpResponse response) {
                ended.add(sub);
            }

            @Override
            protected void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
            }

            @Override
            protected void invalidMessage(RemoteGENASubscription sub, UnsupportedDataException error) {
                fail(error);
            }
        };
        callback.setControlPoint(
                new ControlPointImpl(upnpService.getConfiguration(), upnpService.getProtocolFactory(), registry));
        return callback;
    }

    private static class TestRegistry extends RegistryImpl {
        TestRegistry(MockUpnpService upnpService) {
            super(upnpService);
        }

        @Override
        protected RegistryMaintainer createRegistryMaintainer() {
            return null;
        }
    }
}
