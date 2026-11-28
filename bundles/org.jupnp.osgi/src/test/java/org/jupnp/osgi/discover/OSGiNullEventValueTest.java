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
package org.jupnp.osgi.discover;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.CustomDatatype;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.osgi.impl.UPnPDeviceImpl;
import org.jupnp.osgi.impl.UPnPServiceImpl;
import org.osgi.service.upnp.UPnPEventListener;

class OSGiNullEventValueTest {

    private RemoteService service;
    private UPnPEventListenerSubscriptionCallback callback;
    private RemoteGENASubscription subscription;
    private final List<Dictionary> events = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        service = new RemoteService(new UDAServiceType("SwitchPower"), new UDAServiceId("SwitchPower"),
                URI.create("/scpd.xml"), URI.create("/control"), URI.create("/event"), new Action[0],
                new StateVariable[] {
                        new StateVariable("Status",
                                new StateVariableTypeDetails(Datatype.Builtin.BOOLEAN.getDatatype())),
                        new StateVariable("Target",
                                new StateVariableTypeDetails(Datatype.Builtin.BOOLEAN.getDatatype())),
                        new StateVariable("Vendor", new StateVariableTypeDetails(new CustomDatatype("vendor"))) });
        RemoteDevice device = new RemoteDevice(
                new RemoteDeviceIdentity(new UDN("lamp"), 1800, new URL("http://127.0.0.1/description.xml"), null,
                        null),
                new UDADeviceType("BinaryLight"),
                new DeviceDetails("Lamp", new ManufacturerDetails("jUPnP"), new ModelDetails("Lamp")), service);

        UPnPDeviceImpl osgiDevice = new UPnPDeviceImpl(device);
        UPnPEventListener listener = (deviceId, serviceId, dictionary) -> events.add(dictionary);
        callback = new UPnPEventListenerSubscriptionCallback(osgiDevice,
                (UPnPServiceImpl) osgiDevice.getService(service.getServiceId().toString()), listener);

        subscription = new RemoteGENASubscription(service, 1800) {
            @Override
            public void established() {
            }

            @Override
            public void eventReceived() {
                callback.eventReceived(this);
            }

            @Override
            public void invalidMessage(UnsupportedDataException e) {
            }

            @Override
            public void failed(UpnpResponse responseStatus) {
            }

            @Override
            public void ended(CancelReason reason, UpnpResponse responseStatus) {
            }

            @Override
            public void eventsMissed(int numberOfMissedEvents) {
            }
        };
    }

    private void receive(long sequence, String variable, String value) {
        subscription.receive(new UnsignedIntegerFourBytes(sequence),
                List.of(new StateVariableValue(service.getStateVariable(variable), value)));
    }

    @Test
    void emptyValueIsLeftOutOfEvents() {
        receive(0, "Status", "0");
        receive(1, "Target", "1");
        assertEquals(2, events.size());
        assertEquals(Boolean.FALSE, events.get(1).get("Status"));
        assertEquals(Boolean.TRUE, events.get(1).get("Target"));

        // An empty boolean has no OSGi value, it must not prevent the delivery of this and all later events
        receive(2, "Target", "");
        receive(3, "Status", "1");
        assertEquals(4, events.size());
        assertEquals(Boolean.FALSE, events.get(2).get("Status"));
        assertNull(events.get(2).get("Target"));
        assertEquals(Boolean.TRUE, events.get(3).get("Status"));
        assertNull(events.get(3).get("Target"));

        receive(4, "Target", "0");
        assertEquals(5, events.size());
        assertEquals(Boolean.FALSE, events.get(4).get("Target"));
    }

    @Test
    void vendorDatatypeValueIsLeftOutOfEvents() {
        receive(0, "Vendor", "something");
        assertTrue(events.isEmpty());

        receive(1, "Status", "1");
        assertEquals(1, events.size());
        assertEquals(1, events.get(0).size());
        assertEquals(Boolean.TRUE, events.get(0).get("Status"));
    }
}
