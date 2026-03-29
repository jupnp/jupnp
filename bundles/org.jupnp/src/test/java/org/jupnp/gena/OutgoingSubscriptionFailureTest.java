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

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpService;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.gena.IncomingEventRequestMessage;
import org.jupnp.model.message.gena.OutgoingEventRequestMessage;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.util.URIUtil;

class OutgoingSubscriptionFailureTest {

    @Test
    void subscriptionLifecycleNetworkOff() {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public List<NetworkAddress> getActiveStreamServers(InetAddress preferredAddress) {
                        // Simulate network switched off
                        return List.of();
                    }

                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] { createSubscribeResponseMessage() };
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
                // Should fail without response and exception (only TRACE log message)
                assertNull(responseStatus);
                assertNull(exception);
                testAssertions.add(true);
            }

            @Override
            public void established(GENASubscription subscription) {
                testAssertions.add(false);
            }

            @Override
            public void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus) {
                testAssertions.add(false);
            }

            @Override
            public void eventReceived(GENASubscription subscription) {
                testAssertions.add(false);
            }

            @Override
            public void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
                testAssertions.add(false);
            }
        };

        upnpService.getControlPoint().execute(callback);
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }
    }

    @Test
    void subscriptionLifecycleMissedEvent() throws Exception {
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
                assertEquals("uuid:1234", subscription.getSubscriptionId());
                assertEquals(180, subscription.getActualDurationSeconds());
                testAssertions.add(true);
            }

            @Override
            public void ended(GENASubscription subscription, CancelReason reason, UpnpResponse responseStatus) {
                assertNull(reason);
                assertEquals(UpnpResponse.Status.OK.getStatusCode(), responseStatus.getStatusCode());
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
                assertEquals(2, numberOfMissedEvents);
                testAssertions.add(true);
            }
        };

        upnpService.getControlPoint().execute(callback);

        ReceivingSync prot = upnpService.getProtocolFactory()
                .createReceivingSync(createEventRequestMessage(upnpService, callback, 0));
        prot.run();

        prot = upnpService.getProtocolFactory().createReceivingSync(createEventRequestMessage(upnpService, callback, 3) // Note
                                                                                                                        // the
                                                                                                                        // missing
                                                                                                                        // event
                                                                                                                        // messages
        );
        prot.run();

        callback.end();

        assertEquals(5, testAssertions.size());
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        List<StreamRequestMessage> sentMessages = upnpService.getRouter().getSentStreamRequestMessages();

        assertEquals(2, sentMessages.size());
        assertEquals(UpnpRequest.Method.SUBSCRIBE, sentMessages.get(0).getOperation().getMethod());
        assertEquals(Integer.valueOf(1800), sentMessages.get(0).getHeaders()
                .getFirstHeader(UpnpHeader.Type.TIMEOUT, TimeoutHeader.class).getValue());

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

    protected IncomingEventRequestMessage createEventRequestMessage(UpnpService upnpService,
            SubscriptionCallback callback, int sequence) {

        List<StateVariableValue> values = new ArrayList<>();
        values.add(new StateVariableValue(callback.getService().getStateVariable("Status"), false));
        values.add(new StateVariableValue(callback.getService().getStateVariable("Target"), true));

        OutgoingEventRequestMessage outgoing = new OutgoingEventRequestMessage(callback.getSubscription(),
                URIUtil.toURL(URI.create("http://10.0.0.123/some/callback")), new UnsignedIntegerFourBytes(sequence),
                values);
        outgoing.getOperation()
                .setUri(upnpService.getConfiguration().getNamespace().getEventCallbackPath(callback.getService()));

        upnpService.getConfiguration().getGenaEventProcessor().writeBody(outgoing);

        return new IncomingEventRequestMessage(outgoing,
                ((RemoteGENASubscription) callback.getSubscription()).getService());
    }
}
