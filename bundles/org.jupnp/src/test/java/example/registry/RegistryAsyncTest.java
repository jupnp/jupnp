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
package example.registry;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.junit.jupiter.api.Test;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;

class RegistryAsyncTest {

    private static final int DEVICE_COUNT = 20;
    private static final String[] UDNS = new String[DEVICE_COUNT];

    @Test
    void addMultipleLocalDevices() throws InterruptedException {

        for (int i = 0; i < UDNS.length; i++) {
            UDNS[i] = "my-device-" + i;
        }

        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        final Registry registry = upnpService.getRegistry();

        new Thread(new RegistryClient(registry, 0)).start();
        new Thread(new RegistryClient(registry, 1)).start();
        new Thread(new RegistryClient(registry, 2)).start();
        new Thread(new RegistryClient(registry, 3)).start();

        Thread.sleep(5000);

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

        assertNull(deadlockedThreads);

        assertEquals(DEVICE_COUNT, registry.getLocalDevices().size() + registry.getRemoteDevices().size());

        for (LocalDevice localDevice : registry.getLocalDevices()) {
            RemoteDevice remoteDevice = registry.getRemoteDevice(localDevice.getIdentity().getUdn(), true);
            assertNull(remoteDevice);
        }
    }

    private static LocalDevice createLocalDevice(String udn) throws ValidationException {
        DeviceIdentity identity = new DeviceIdentity(new UDN(udn));
        return new LocalDevice(identity);
    }

    private static RemoteDevice createRemoteDevice(String udn) throws ValidationException {
        RemoteDeviceIdentity identity = new RemoteDeviceIdentity(new UDN(udn), 16, null, null, null);
        return new RemoteDevice(identity);
    }

    private static class RegistryClient implements Runnable {

        private final Registry registry;
        private final int threadNumber;

        public RegistryClient(Registry registry, int threadNumber) {
            this.registry = registry;
            this.threadNumber = threadNumber;
        }

        @Override
        public void run() {
            try {
                /*
                 * Each thread has a different starting point and tries to add each device multiple times as both local
                 * and
                 * remote device.
                 */
                for (int i = threadNumber * 7; i < 100; i++) {
                    if (i + threadNumber % 2 == 0) {
                        LocalDevice localDevice = createLocalDevice(UDNS[i % DEVICE_COUNT]);
                        registry.addDevice(localDevice);
                    } else {
                        RemoteDevice remoteDevice = createRemoteDevice(UDNS[i % DEVICE_COUNT]);
                        registry.addDevice(remoteDevice);
                    }

                }
            } catch (ValidationException e) {
            }
        }
    }
}
