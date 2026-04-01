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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.Namespace;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.OutgoingSubscribeResponseMessage;
import org.jupnp.model.message.header.CallbackHeader;
import org.jupnp.model.message.header.EventSequenceHeader;
import org.jupnp.model.message.header.NTEventHeader;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.protocol.sync.ReceivingSubscribe;
import org.jupnp.protocol.sync.ReceivingUnsubscribe;
import org.jupnp.util.URIUtil;

class IncomingSubscriptionLifecycleTest {

    @Test
    void subscriptionLifecycle() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Register local device and its service
        LocalDevice device = GenaSampleData.createTestDevice(GenaSampleData.LocalTestService.class);
        upnpService.getRegistry().addDevice(device);

        Namespace ns = upnpService.getConfiguration().getNamespace();

        LocalService<?> service = SampleData.getFirstService(device);
        URL callbackURL = URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), ns.getEventCallbackPath(service));

        StreamRequestMessage subscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.SUBSCRIBE,
                ns.getEventSubscriptionPath(service));

        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.CALLBACK, new CallbackHeader(callbackURL));
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.NT, new NTEventHeader());

        ReceivingSubscribe subscribeProt = new ReceivingSubscribe(upnpService, subscribeRequestMessage);
        subscribeProt.run();
        OutgoingSubscribeResponseMessage subscribeResponseMessage = subscribeProt.getOutputMessage();

        assertEquals(UpnpResponse.Status.OK.getStatusCode(), subscribeResponseMessage.getOperation().getStatusCode());
        String subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();
        assertTrue(subscriptionId.startsWith("uuid:"));
        assertEquals(1800, subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.TIMEOUT, TimeoutHeader.class).getValue());
        assertEquals(1800, upnpService.getRegistry().getLocalSubscription(subscriptionId).getActualDurationSeconds());

        // Now send the initial event
        subscribeProt.responseSent(subscribeResponseMessage);

        // And immediately "modify" the state of the service, this should result in "concurrent" event messages
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);

        StreamRequestMessage unsubscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.UNSUBSCRIBE,
                ns.getEventSubscriptionPath(service));
        unsubscribeRequestMessage.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader(subscriptionId));

        ReceivingUnsubscribe unsubscribeProt = new ReceivingUnsubscribe(upnpService, unsubscribeRequestMessage);
        unsubscribeProt.run();
        StreamResponseMessage unsubscribeResponseMessage = unsubscribeProt.getOutputMessage();
        assertEquals(UpnpResponse.Status.OK.getStatusCode(), unsubscribeResponseMessage.getOperation().getStatusCode());
        assertNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));

        List<StreamRequestMessage> sentMessages = upnpService.getRouter().getSentStreamRequestMessages();
        assertEquals(2, sentMessages.size());
        for (int i = 0; i < 2; i++) {
            assertEquals(UpnpRequest.Method.NOTIFY, sentMessages.get(i).getOperation().getMethod());
        }
        assertEquals(subscriptionId, sentMessages.get(0).getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue());
        assertEquals(subscriptionId, sentMessages.get(1).getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue());
        assertEquals(callbackURL.toString(), sentMessages.get(0).getOperation().getURI().toString());
        assertEquals(0L, sentMessages.get(0).getHeaders().getFirstHeader(UpnpHeader.Type.SEQ, EventSequenceHeader.class)
                .getValue().getValue());
        assertEquals(1L, sentMessages.get(1).getHeaders().getFirstHeader(UpnpHeader.Type.SEQ, EventSequenceHeader.class)
                .getValue().getValue());
    }

    @Test
    void subscriptionLifecycleFailedResponse() throws Exception {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Register local device and its service
        LocalDevice device = GenaSampleData.createTestDevice(GenaSampleData.LocalTestService.class);
        upnpService.getRegistry().addDevice(device);

        Namespace ns = upnpService.getConfiguration().getNamespace();

        LocalService<?> service = SampleData.getFirstService(device);
        URL callbackURL = URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), ns.getEventCallbackPath(service));

        StreamRequestMessage subscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.SUBSCRIBE,
                ns.getEventSubscriptionPath(service));

        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.CALLBACK, new CallbackHeader(callbackURL));
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.NT, new NTEventHeader());

        ReceivingSubscribe subscribeProt = new ReceivingSubscribe(upnpService, subscribeRequestMessage);
        subscribeProt.run();

        // From the response the subscriber _should_ receive, keep the identifier for later
        OutgoingSubscribeResponseMessage subscribeResponseMessage = subscribeProt.getOutputMessage();
        String subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();

        // Now, instead of passing the successful response to the protocol, we make it think something went wrong
        subscribeProt.responseSent(null);

        // The subscription should be removed from the registry!
        assertNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));
    }
}
