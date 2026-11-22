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

import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;

/**
 * Removing a device from the registry must end the subscriptions to all services of its device graph, not only those
 * of the root device.
 */
class SubscriptionDeviceRemovalTest {

    private static final String SUBSCRIPTION_ID = "uuid:1234";

    @Test
    void removeRemoteDeviceEndsSubscriptionOfRootDeviceService() {
        RemoteDevice rd = SampleData.createRemoteDevice();
        assertOutgoingSubscriptionEndsWithDevice(rd, rd.getServices()[0]);
    }

    @Test
    void removeRemoteDeviceEndsSubscriptionOfEmbeddedDeviceService() {
        RemoteDevice rd = SampleData.createRemoteDevice();
        assertOutgoingSubscriptionEndsWithDevice(rd, rd.getEmbeddedDevices()[0].getServices()[0]);
    }

    @Test
    void removeLocalDeviceEndsSubscriptionOfRootDeviceService() throws Exception {
        LocalDevice ld = SampleData.createLocalDevice();
        assertIncomingSubscriptionEndsWithDevice(ld, ld.getServices()[0]);
    }

    @Test
    void removeLocalDeviceEndsSubscriptionOfEmbeddedDeviceService() throws Exception {
        LocalDevice ld = SampleData.createLocalDevice();
        assertIncomingSubscriptionEndsWithDevice(ld, ld.getEmbeddedDevices()[0].getServices()[0]);
    }

    private void assertOutgoingSubscriptionEndsWithDevice(RemoteDevice rd, RemoteService service) {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        upnpService.getRegistry().addDevice(rd);

        final List<CancelReason> ended = new ArrayList<>();
        RemoteGENASubscription subscription = new RemoteGENASubscription(service, 1800) {
            @Override
            public void ended(CancelReason reason, UpnpResponse responseStatus) {
                ended.add(reason);
            }

            @Override
            public void failed(UpnpResponse responseStatus) {
            }

            @Override
            public void established() {
            }

            @Override
            public void eventReceived() {
            }

            @Override
            public void eventsMissed(int numberOfMissedEvents) {
            }

            @Override
            public void invalidMessage(UnsupportedDataException e) {
            }
        };
        subscription.setSubscriptionId(SUBSCRIPTION_ID);
        upnpService.getRegistry().addRemoteSubscription(subscription);

        assertTrue(upnpService.getRegistry().removeDevice(rd));

        assertNull(upnpService.getRegistry().getRemoteSubscription(SUBSCRIPTION_ID));
        assertEquals(List.of(CancelReason.DEVICE_WAS_REMOVED), ended);

        upnpService.shutdown();
    }

    private void assertIncomingSubscriptionEndsWithDevice(LocalDevice ld, LocalService service) throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        upnpService.getRegistry().addDevice(ld);

        final List<CancelReason> ended = new ArrayList<>();
        LocalGENASubscription subscription = new LocalGENASubscription(service, new ArrayList<>()) {
            @Override
            public void ended(CancelReason reason) {
                ended.add(reason);
            }

            @Override
            public void established() {
            }

            @Override
            public void eventReceived() {
            }
        };
        subscription.setSubscriptionId(SUBSCRIPTION_ID);
        upnpService.getRegistry().addLocalSubscription(subscription);

        assertTrue(upnpService.getRegistry().removeDevice(ld));

        assertNull(upnpService.getRegistry().getLocalSubscription(SUBSCRIPTION_ID));
        assertEquals(List.of(CancelReason.DEVICE_WAS_REMOVED), ended);

        upnpService.shutdown();
    }
}
