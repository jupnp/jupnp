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
package org.jupnp.ssdp;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.mock.MockUpnpServiceConfiguration;
import org.jupnp.model.ExpirationDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.resource.Resource;

class RegistryExpirationTest {

    @Test
    void addAndExpire() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(false, true);
        upnpService.startup();

        RemoteDevice rd = SampleData.createRemoteDevice(SampleData.createRemoteDeviceIdentity(1));
        upnpService.getRegistry().addDevice(rd);

        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        Thread.sleep(3000);

        assertEquals(0, upnpService.getRegistry().getRemoteDevices().size());

        upnpService.shutdown();
    }

    @Test
    void overrideAgeThenAddAndExpire() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(new MockUpnpServiceConfiguration(true) {

            @Override
            public Integer getRemoteDeviceMaxAgeSeconds() {
                return 0;
            }
        });
        upnpService.startup();

        RemoteDevice rd = SampleData.createRemoteDevice(SampleData.createRemoteDeviceIdentity(1));
        upnpService.getRegistry().addDevice(rd);

        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        Thread.sleep(3000);

        // Still registered!
        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        // Update should not change the expiration time
        upnpService.getRegistry().update(rd.getIdentity());

        Thread.sleep(3000);

        // Still registered!
        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        upnpService.shutdown();
    }

    @Test
    void addAndUpdateAndExpire() throws Exception {

        MockUpnpService upnpService = new MockUpnpService(false, true);
        upnpService.startup();

        RemoteDevice rd = SampleData.createRemoteDevice(SampleData.createRemoteDeviceIdentity(2));

        // Add it to registry
        upnpService.getRegistry().addDevice(rd);
        Thread.sleep(1000);
        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        // Update it in registry
        upnpService.getRegistry().addDevice(rd);
        Thread.sleep(1000);
        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        // Update again
        upnpService.getRegistry().update(rd.getIdentity());
        Thread.sleep(1000);
        assertEquals(1, upnpService.getRegistry().getRemoteDevices().size());

        // Wait for expiration
        Thread.sleep(3000);
        assertEquals(0, upnpService.getRegistry().getRemoteDevices().size());

        upnpService.shutdown();
    }

    @Test
    void addResourceAndExpire() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(false, true);
        upnpService.startup();

        Resource<String> resource = new Resource<>(URI.create("/this/is/a/test"), "foo");
        upnpService.getRegistry().addResource(resource, 2);

        assertEquals(1, upnpService.getRegistry().getResources().size());

        Thread.sleep(4000);

        assertEquals(0, upnpService.getRegistry().getResources().size());

        upnpService.shutdown();
    }

    @Test
    void addResourceAndMaintain() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(false, true);
        upnpService.startup();

        final TestRunnable testRunnable = new TestRunnable();

        Resource<String> resource = new Resource<>(URI.create("/this/is/a/test"), "foo") {
            @Override
            public void maintain(List<Runnable> pendingExecutions, ExpirationDetails expirationDetails) {
                if (expirationDetails.getSecondsUntilExpiration() == 1) {
                    pendingExecutions.add(testRunnable);
                }
            }
        };
        upnpService.getRegistry().addResource(resource, 2);

        assertEquals(1, upnpService.getRegistry().getResources().size());

        Thread.sleep(6000);

        assertTrue(testRunnable.wasExecuted);

        upnpService.shutdown();
    }

    protected static class TestRunnable implements Runnable {
        boolean wasExecuted = false;

        @Override
        public void run() {
            wasExecuted = true;
        }
    }
}
