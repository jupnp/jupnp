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

import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.Namespace;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.OutgoingSubscribeResponseMessage;
import org.jupnp.model.message.header.CallbackHeader;
import org.jupnp.model.message.header.NTEventHeader;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.protocol.sync.ReceivingSubscribe;
import org.jupnp.protocol.sync.ReceivingUnsubscribe;
import org.jupnp.util.URIUtil;

/**
 * How long the delivery of the response to a SUBSCRIBE takes is up to the subscriber, the subscription can be gone
 * from the registry by the time the transport reports the response as sent.
 */
class IncomingSubscriptionRemovedEarlyTest {

    private MockUpnpService upnpService;
    private LocalDevice device;
    private LocalService<?> service;
    private ReceivingSubscribe subscribeProt;
    private OutgoingSubscribeResponseMessage subscribeResponseMessage;
    private String subscriptionId;

    @BeforeEach
    void subscribe() throws Exception {
        upnpService = new MockUpnpService();
        upnpService.startup();

        // Register local device and its service
        device = GenaSampleData.createTestDevice(GenaSampleData.LocalTestService.class);
        upnpService.getRegistry().addDevice(device);

        Namespace ns = upnpService.getConfiguration().getNamespace();

        service = SampleData.getFirstService(device);
        URL callbackURL = URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), ns.getEventCallbackPath(service));

        StreamRequestMessage subscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.SUBSCRIBE,
                ns.getEventSubscriptionPath(service));
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.CALLBACK, new CallbackHeader(callbackURL));
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.NT, new NTEventHeader());

        // The request is processed, its response is on the way to the subscriber
        subscribeProt = new ReceivingSubscribe(upnpService, subscribeRequestMessage);
        subscribeProt.run();
        subscribeResponseMessage = subscribeProt.getOutputMessage();
        subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();
        assertNotNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));
    }

    @Test
    void subscriptionExpiredBeforeResponseSent() {
        // This is what the registry maintenance does with an expired subscription
        LocalGENASubscription subscription = upnpService.getRegistry().getLocalSubscription(subscriptionId);
        assertTrue(upnpService.getRegistry().removeLocalSubscription(subscription));
        subscription.end(CancelReason.EXPIRED);

        assertNotEstablishedByResponseSent();
    }

    @Test
    void subscriptionUnsubscribedBeforeResponseSent() {
        StreamRequestMessage unsubscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.UNSUBSCRIBE,
                upnpService.getConfiguration().getNamespace().getEventSubscriptionPath(service));
        unsubscribeRequestMessage.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader(subscriptionId));

        ReceivingUnsubscribe unsubscribeProt = new ReceivingUnsubscribe(upnpService, unsubscribeRequestMessage);
        unsubscribeProt.run();
        assertEquals(UpnpResponse.Status.OK.getStatusCode(),
                unsubscribeProt.getOutputMessage().getOperation().getStatusCode());

        assertNotEstablishedByResponseSent();
    }

    @Test
    void deviceRemovedBeforeResponseSent() {
        upnpService.getRegistry().removeDevice(device);

        assertNotEstablishedByResponseSent();
    }

    private void assertNotEstablishedByResponseSent() {
        assertNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));

        // Now the transport reports that the response has been delivered
        subscribeProt.responseSent(subscribeResponseMessage);

        // The subscription is gone, so neither an initial event nor events for state changes must be sent
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
        assertNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));
        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }
}
