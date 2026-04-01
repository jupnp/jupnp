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

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpService;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.IncomingEventRequestMessage;
import org.jupnp.model.message.gena.OutgoingEventRequestMessage;
import org.jupnp.model.message.header.CallbackHeader;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.util.URIUtil;

class OutgoingSubscriptionLifecycleTest {

    @Test
    void subscriptionLifecycle() throws Exception {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] { createSubscribeResponseMessage(),
                                createUnsubscribeResponseMessage() };
                    }
                };
            }
        };
        upnpService.startup();

        final List<Boolean> testAssertions = new ArrayList<>();

        // Register remote device and its service
        RemoteDevice device = SampleData.createRemoteDevice();
        upnpService.getRegistry().addDevice(device);

        RemoteService service = SampleData.getFirstService(device);

        SubscriptionCallback callback = new SubscriptionCallback(service) {
            @Override
            protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
                    String defaultMsg) {
                testAssertions.add(false);
            }

            @Override
            public void established(GENASubscription subscription) {
                testAssertions.add(true);
            }

            @Override
            public void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus) {
                assertNull(reason);
                assertEquals(responseStatus.getStatusCode(), UpnpResponse.Status.OK.getStatusCode());
                testAssertions.add(true);
            }

            @Override
            public void eventReceived(GENASubscription subscription) {
                assertEquals("0", subscription.getCurrentValues().get("Status").toString());
                assertEquals("1", subscription.getCurrentValues().get("Target").toString());
                testAssertions.add(true);
            }

            @Override
            public void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
                testAssertions.add(false);
            }
        };

        upnpService.getControlPoint().execute(callback);

        // Subscription process OK?
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        // Simulate received event
        upnpService.getProtocolFactory().createReceivingSync(createEventRequestMessage(upnpService, callback)).run();

        assertEquals(0L, callback.getSubscription().getCurrentSequence().getValue());
        assertEquals("uuid:1234", callback.getSubscription().getSubscriptionId());
        assertEquals(180, callback.getSubscription().getActualDurationSeconds());

        List<URL> callbackURLs = ((RemoteGENASubscription) callback.getSubscription()).getEventCallbackURLs(
                upnpService.getRouter().getActiveStreamServers(null), upnpService.getConfiguration().getNamespace());

        callback.end();

        assertNull(callback.getSubscription());

        assertEquals(3, testAssertions.size());
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        List<StreamRequestMessage> sentMessages = upnpService.getRouter().getSentStreamRequestMessages();
        assertEquals(2, sentMessages.size());
        assertEquals(UpnpRequest.Method.SUBSCRIBE, sentMessages.get(0).getOperation().getMethod());
        assertEquals(1800, sentMessages.get(0).getHeaders().getFirstHeader(UpnpHeader.Type.TIMEOUT, TimeoutHeader.class)
                .getValue());

        assertEquals(1, callbackURLs.size());
        assertEquals(callbackURLs.get(0), sentMessages.get(0).getHeaders()
                .getFirstHeader(UpnpHeader.Type.CALLBACK, CallbackHeader.class).getValue().get(0));

        assertEquals(UpnpRequest.Method.UNSUBSCRIBE, sentMessages.get(1).getOperation().getMethod());
        assertEquals("uuid:1234", sentMessages.get(1).getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue());
    }

    protected StreamResponseMessage createSubscribeResponseMessage() {
        StreamResponseMessage msg = new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
        msg.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader("uuid:1234"));
        msg.getHeaders().add(UpnpHeader.Type.TIMEOUT, new TimeoutHeader(180));
        return msg;
    }

    protected StreamResponseMessage createUnsubscribeResponseMessage() {
        return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
    }

    protected IncomingEventRequestMessage createEventRequestMessage(final UpnpService upnpService,
            final SubscriptionCallback callback) {

        List<StateVariableValue> values = new ArrayList<>();
        values.add(new StateVariableValue(callback.getService().getStateVariable("Status"), false));
        values.add(new StateVariableValue(callback.getService().getStateVariable("Target"), true));

        OutgoingEventRequestMessage outgoing = new OutgoingEventRequestMessage(callback.getSubscription(),
                URIUtil.toURL(URI.create("http://10.0.0.123/this/is/ignored/anyway")), new UnsignedIntegerFourBytes(0),
                values);
        outgoing.getOperation()
                .setUri(upnpService.getConfiguration().getNamespace().getEventCallbackPath(callback.getService()));

        upnpService.getConfiguration().getGenaEventProcessor().writeBody(outgoing);

        return new IncomingEventRequestMessage(outgoing,
                ((RemoteGENASubscription) callback.getSubscription()).getService());
    }
}
