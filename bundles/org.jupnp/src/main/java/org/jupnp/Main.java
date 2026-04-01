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
package org.jupnp;

import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;

/**
 * Runs a simple UPnP discovery procedure.
 */
public class Main {

    public static void main(String[] args) throws Exception {

        // UPnP discovery is asynchronous, we need a callback
        RegistryListener listener = new RegistryListener() {

            @Override
            public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
                System.out.println("Discovery started: " + device.getDisplayString());
            }

            @Override
            public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception e) {
                System.out.println("Discovery failed: " + device.getDisplayString() + " => " + e);
            }

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                System.out.println("Remote device available: " + device.getDisplayString());
            }

            @Override
            public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
                System.out.println("Remote device updated: " + device.getDisplayString());
            }

            @Override
            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                System.out.println("Remote device removed: " + device.getDisplayString());
            }

            @Override
            public void localDeviceAdded(Registry registry, LocalDevice device) {
                System.out.println("Local device added: " + device.getDisplayString());
            }

            @Override
            public void localDeviceRemoved(Registry registry, LocalDevice device) {
                System.out.println("Local device removed: " + device.getDisplayString());
            }

            @Override
            public void beforeShutdown(Registry registry) {
                System.out.println("Before shutdown, the registry has devices: " + registry.getDevices().size());
            }

            @Override
            public void afterShutdown() {
                System.out.println("Shutdown of registry complete!");
            }
        };

        // This will create necessary network resources for UPnP right away
        System.out.println("Starting jUPnP...");
        UpnpService upnpService = new UpnpServiceImpl(new DefaultUpnpServiceConfiguration());
        upnpService.startup();
        upnpService.getRegistry().addListener(listener);

        // Send a search message to all devices and services, they should respond soon
        System.out.println("Sending SEARCH message to all devices...");
        upnpService.getControlPoint().search(new STAllHeader());

        // Let's wait 10 seconds for them to respond
        System.out.println("Waiting 10 seconds before shutting down...");
        Thread.sleep(10000);

        // Release all resources and advertise BYEBYE to other UPnP devices
        System.out.println("Stopping jUPnP...");
        upnpService.shutdown();
    }
}
