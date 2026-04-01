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
package org.jupnp.model.profile;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.jupnp.model.meta.DeviceDetails;

/**
 * @author Mario Franco
 */
class DeviceDetailsProviderTest {

    @Test
    void headerRegexMatch() {
        RemoteClientInfo clientInfo = new RemoteClientInfo();

        DeviceDetails dd1 = new DeviceDetails("My Testdevice 1");
        DeviceDetails dd2 = new DeviceDetails("My Testdevice 2");

        Map<HeaderDeviceDetailsProvider.Key, DeviceDetails> headerDetails = new HashMap<>();

        headerDetails.put(new HeaderDeviceDetailsProvider.Key("User-Agent", "Xbox.*"), dd1);
        headerDetails.put(new HeaderDeviceDetailsProvider.Key("X-AV-Client-Info", ".*PLAYSTATION 3.*"), dd2);

        HeaderDeviceDetailsProvider provider = new HeaderDeviceDetailsProvider(dd1, headerDetails);

        // No match, test default behavior
        clientInfo.getRequestHeaders().clear();
        clientInfo.getRequestHeaders().add("User-Agent",
                "Microsoft-Windows/6.1 UPnP/1.0 Windows-Media-Player-DMS/12.0.7600.16385 DLNADOC/1.50");
        assertEquals(dd1, provider.provide(clientInfo));

        clientInfo.getRequestHeaders().clear();
        clientInfo.getRequestHeaders().add("User-Agent", "UPnP/1.0");
        clientInfo.getRequestHeaders().add("X-AV-Client-Info",
                "av=5.0; cn=\"Sony Computer Entertainment Inc.\"; mn=\"PLAYSTATION 3\"; mv=\"1.0\";");
        assertEquals(dd2, provider.provide(clientInfo));

        clientInfo.getRequestHeaders().clear();
        clientInfo.getRequestHeaders().add("User-Agent", "Xbox/2.0.4548.0 UPnP/1.0 Xbox/2.0.4548.0");
        assertEquals(dd1, provider.provide(clientInfo));
    }
}
