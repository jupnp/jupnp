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

import java.io.IOException;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.binding.*;
import org.jupnp.binding.annotations.*;
import org.jupnp.model.*;
import org.jupnp.model.meta.*;
import org.jupnp.model.types.*;

public class BinaryLightServer implements Runnable {

    public static void main(String[] args) {
        // Start a user thread that runs the UPnP stack
        Thread serverThread = new Thread(new BinaryLightServer());
        serverThread.setDaemon(false);
        serverThread.start();
    }

    @Override
    public void run() {
        try {
            final UpnpService upnpService = new UpnpServiceImpl();

            Runtime.getRuntime().addShutdownHook(new Thread(upnpService::shutdown));

            // Add the bound local device to the registry
            upnpService.getRegistry().addDevice(createDevice());
        } catch (Exception e) {
            System.err.println("Exception occurred: " + e);
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    LocalDevice createDevice() throws ValidationException, LocalServiceBindingException, IOException {
        DeviceIdentity identity = new DeviceIdentity(UDN.uniqueSystemIdentifier("Demo Binary Light"));
        DeviceType type = new UDADeviceType("BinaryLight", 1);
        DeviceDetails details = new DeviceDetails("Friendly Binary Light", new ManufacturerDetails("ACME"),
                new ModelDetails("BinLight2000", "A demo light with on/off switch.", "v1"));
        Icon icon = new Icon("image/png", 48, 48, 8, getClass().getResource("icon.png"));

        LocalService<SwitchPower> switchPowerService = new AnnotationLocalServiceBinder().read(SwitchPower.class);
        switchPowerService.setManager(new DefaultServiceManager<>(switchPowerService, SwitchPower.class));

        return new LocalDevice(identity, type, details, icon, switchPowerService);

        /*
         * Several services can be bound to the same device:
         * return new LocalDevice(identity, type, details, icon, new LocalService[] {switchPowerService,
         * myOtherService});
         */
    }
}
