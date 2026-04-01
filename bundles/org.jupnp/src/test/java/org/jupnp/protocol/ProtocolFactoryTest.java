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
package org.jupnp.protocol;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.Namespace;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.protocol.sync.ReceivingEvent;

/**
 * @author Christian Bauer
 */
class ProtocolFactoryTest {

    @Test
    void noSyncProtocol() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        assertThrows(ProtocolCreationException.class,
                () -> upnpService.getProtocolFactory().createReceivingSync(new StreamRequestMessage(
                        UpnpRequest.Method.NOTIFY, URI.create("/dev/1234/upnp-org/SwitchPower/invalid"), "")));
    }

    @Test
    void receivingEvent() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        StreamRequestMessage message = new StreamRequestMessage(UpnpRequest.Method.NOTIFY,
                URI.create("/dev/1234/upnp-org/SwitchPower" + Namespace.EVENTS + Namespace.CALLBACK_FILE), "");
        ReceivingSync protocol = upnpService.getProtocolFactory().createReceivingSync(message);
        assertInstanceOf(ReceivingEvent.class, protocol);

        // TODO: UPNP VIOLATION: Onkyo devices send event messages with trailing garbage characters
        // dev/1234/svc/upnp-org/MyService/event/callback192%2e168%2e10%2e38
        message = new StreamRequestMessage(UpnpRequest.Method.NOTIFY, URI.create(
                "/dev/1234/upnp-org/SwitchPower" + Namespace.EVENTS + Namespace.CALLBACK_FILE + "192%2e168%2e10%2e38"),
                "");
        protocol = upnpService.getProtocolFactory().createReceivingSync(message);
        assertInstanceOf(ReceivingEvent.class, protocol);
    }
}
