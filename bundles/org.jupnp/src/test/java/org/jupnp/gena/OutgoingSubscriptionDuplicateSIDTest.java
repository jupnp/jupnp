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
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;

/**
 * The identifier of an outgoing subscription is chosen by the remote host: what happens to one subscription must not
 * change the registry entry of another subscription that was given the same identifier.
 */
class OutgoingSubscriptionDuplicateSIDTest {

    static final String SID = "uuid:1234";

    @Test
    void renewalOfRemovedSubscriptionDoesNotReplaceSubscriptionWithSameSID() {
        StreamResponseMessage renewed = new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
        renewed.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader(SID));
        renewed.getHeaders().add(UpnpHeader.Type.TIMEOUT, new TimeoutHeader(1800));

        MockUpnpService upnpService = createUpnpService(renewed);
        RemoteDevice device = SampleData.createRemoteDevice();
        TestSubscription registered = registerSubscription(upnpService, device);

        upnpService.getProtocolFactory().createSendingRenewal(new TestSubscription(registered.getService())).run();

        assertSame(registered, upnpService.getRegistry().getRemoteSubscription(SID));

        // The registered subscription is still the one that ends with its device
        upnpService.getRegistry().removeDevice(device);

        assertNull(upnpService.getRegistry().getRemoteSubscription(SID));
        assertEquals(List.of(CancelReason.DEVICE_WAS_REMOVED), registered.ended);
    }

    @Test
    void failedRenewalOfRemovedSubscriptionDoesNotRemoveSubscriptionWithSameSID() {
        MockUpnpService upnpService = createUpnpService(
                new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED)));
        TestSubscription registered = registerSubscription(upnpService, SampleData.createRemoteDevice());

        upnpService.getProtocolFactory().createSendingRenewal(new TestSubscription(registered.getService())).run();

        assertSame(registered, upnpService.getRegistry().getRemoteSubscription(SID));
        assertTrue(registered.ended.isEmpty());
    }

    @Test
    void unsubscribeOfRemovedSubscriptionDoesNotRemoveSubscriptionWithSameSID() {
        MockUpnpService upnpService = createUpnpService(
                new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK)));
        TestSubscription registered = registerSubscription(upnpService, SampleData.createRemoteDevice());

        upnpService.getProtocolFactory().createSendingUnsubscribe(new TestSubscription(registered.getService())).run();

        assertSame(registered, upnpService.getRegistry().getRemoteSubscription(SID));
        assertTrue(registered.ended.isEmpty());
    }

    MockUpnpService createUpnpService(StreamResponseMessage response) {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] { response };
                    }
                };
            }
        };
        upnpService.startup();
        return upnpService;
    }

    TestSubscription registerSubscription(MockUpnpService upnpService, RemoteDevice device) {
        upnpService.getRegistry().addDevice(device);

        TestSubscription subscription = new TestSubscription(SampleData.getFirstService(device));
        upnpService.getRegistry().addRemoteSubscription(subscription);
        return subscription;
    }

    /**
     * Every instance has the same identifier, as if the remote host had handed it out more than once.
     */
    static class TestSubscription extends RemoteGENASubscription {

        final List<CancelReason> ended = new ArrayList<>();

        TestSubscription(RemoteService service) {
            super(service, 1800);
            setSubscriptionId(SID);
            setActualSubscriptionDurationSeconds(1800);
        }

        @Override
        public void invalidMessage(UnsupportedDataException e) {
        }

        @Override
        public void failed(UpnpResponse responseStatus) {
        }

        @Override
        public void ended(CancelReason reason, UpnpResponse responseStatus) {
            ended.add(reason);
        }

        @Override
        public void eventsMissed(int numberOfMissedEvents) {
        }

        @Override
        public void established() {
        }

        @Override
        public void eventReceived() {
        }
    }
}
