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
package example.binarylight;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.controlpoint.*;
import org.jupnp.model.action.*;
import org.jupnp.model.message.*;
import org.jupnp.model.message.header.*;
import org.jupnp.model.meta.*;
import org.jupnp.model.types.*;
import org.jupnp.registry.*;

public class BinaryLightClient implements Runnable {

    public static void main(String[] args) {
        // Start a user thread that runs the UPnP stack
        Thread clientThread = new Thread(new BinaryLightClient());
        clientThread.setDaemon(false);
        clientThread.start();
    }

    @Override
    public void run() {
        try {
            UpnpService upnpService = new UpnpServiceImpl();

            // Add a listener for device registration events
            upnpService.getRegistry().addListener(createRegistryListener(upnpService));

            // Broadcast a search message for all devices
            upnpService.getControlPoint().search(new STAllHeader());
        } catch (Exception e) {
            System.err.println("Exception occurred: " + e);
            System.exit(1);
        }
    }

    RegistryListener createRegistryListener(final UpnpService upnpService) {
        return new DefaultRegistryListener() {
            final ServiceId serviceId = new UDAServiceId("SwitchPower");

            @Override
            public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
                Service switchPower;
                if ((switchPower = device.findService(serviceId)) != null) {

                    System.out.println("Service discovered: " + switchPower);
                    executeAction(upnpService, switchPower);
                }
            }

            @Override
            public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
                Service switchPower;
                if ((switchPower = device.findService(serviceId)) != null) {
                    System.out.println("Service disappeared: " + switchPower);
                }
            }
        };
    }

    void executeAction(UpnpService upnpService, Service switchPowerService) {
        ActionInvocation setTargetInvocation = new SetTargetActionInvocation(switchPowerService);

        // Executes asynchronous in the background
        upnpService.getControlPoint().execute(new ActionCallback(setTargetInvocation) {

            @Override
            public void success(ActionInvocation invocation) {
                assert invocation.getOutput().length == 0;
                System.out.println("Successfully called action!");
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                System.err.println(defaultMsg);
            }
        });
    }

    static class SetTargetActionInvocation extends ActionInvocation {
        SetTargetActionInvocation(Service service) {
            super(service.getAction("SetTarget"));
            try {
                // Throws InvalidValueException if the value is of wrong type
                setInput("NewTargetValue", true);
            } catch (InvalidValueException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
    }
}
