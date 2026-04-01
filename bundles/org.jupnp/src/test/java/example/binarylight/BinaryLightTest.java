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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;

/**
 * @author Christian Bauer
 */
class BinaryLightTest {

    @Test
    void testServer() throws Exception {
        LocalDevice binaryLight = new BinaryLightServer().createDevice();
        assertEquals("SetTarget", binaryLight.getServices()[0].getAction("SetTarget").getName());
    }

    @Test
    void testClient() throws Exception {
        // Well we can't really test the listener easily, but the action invocation should work on a local device

        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        BinaryLightClient client = new BinaryLightClient();
        LocalDevice binaryLight = new BinaryLightServer().createDevice();

        LocalService<SwitchPower> service = binaryLight.getServices()[0];
        client.executeAction(upnpService, binaryLight.getServices()[0]);
        Thread.sleep(100);
        assertTrue(service.getManager().getImplementation().getStatus());
    }
}
