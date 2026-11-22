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
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpService;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.Namespace;
import org.jupnp.model.gena.LocalGENASubscription;
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
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.protocol.sync.ReceivingSubscribe;
import org.jupnp.protocol.sync.ReceivingUnsubscribe;
import org.jupnp.protocol.sync.SendingEvent;
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

    @Test
    void renewalBeforeInitialEventDoesNotEstablishAgain() throws Exception {
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
        String subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();

        // The subscriber has the identifier and renews before the initial event was sent, that is, while the
        // sequence of the subscription is still 0
        ReceivingSubscribe renewalProt = new ReceivingSubscribe(upnpService,
                createRenewalRequestMessage(ns, service, subscriptionId));
        renewalProt.run();
        assertEquals(UpnpResponse.Status.OK.getStatusCode(),
                renewalProt.getOutputMessage().getOperation().getStatusCode());
        renewalProt.responseSent(renewalProt.getOutputMessage());

        // The renewal must not register the subscription on the service nor send an initial event
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());

        // Only the response of the subscription request does
        subscribeProt.responseSent(subscribeResponseMessage);
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());

        StreamRequestMessage unsubscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.UNSUBSCRIBE,
                ns.getEventSubscriptionPath(service));
        unsubscribeRequestMessage.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader(subscriptionId));

        ReceivingUnsubscribe unsubscribeProt = new ReceivingUnsubscribe(upnpService, unsubscribeRequestMessage);
        unsubscribeProt.run();
        assertEquals(UpnpResponse.Status.OK.getStatusCode(),
                unsubscribeProt.getOutputMessage().getOperation().getStatusCode());

        // No more events after the subscriber has unsubscribed
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", true, false);
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    @Test
    void renewalFailedResponseKeepsSubscription() throws Exception {
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
        String subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();
        subscribeProt.responseSent(subscribeResponseMessage);
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());

        // Now the response of a renewal can't be delivered to the subscriber
        ReceivingSubscribe renewalProt = new ReceivingSubscribe(upnpService,
                createRenewalRequestMessage(ns, service, subscriptionId));
        renewalProt.run();
        renewalProt.responseException(new Exception("Response not delivered"));

        // The subscription is still registered on the service, so it has to stay in the registry as well
        assertNotNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));

        StreamRequestMessage unsubscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.UNSUBSCRIBE,
                ns.getEventSubscriptionPath(service));
        unsubscribeRequestMessage.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader(subscriptionId));

        ReceivingUnsubscribe unsubscribeProt = new ReceivingUnsubscribe(upnpService, unsubscribeRequestMessage);
        unsubscribeProt.run();
        assertEquals(UpnpResponse.Status.OK.getStatusCode(),
                unsubscribeProt.getOutputMessage().getOperation().getStatusCode());

        // No more events after the subscriber has unsubscribed
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    private StreamRequestMessage createRenewalRequestMessage(Namespace ns, LocalService<?> service,
            String subscriptionId) {
        StreamRequestMessage renewalRequestMessage = new StreamRequestMessage(UpnpRequest.Method.SUBSCRIBE,
                ns.getEventSubscriptionPath(service));
        renewalRequestMessage.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader(subscriptionId));
        return renewalRequestMessage;
    }

    @Test
    void subscriptionReleasesEventValues() throws Exception {
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
        String subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();

        // The state of the service has been read for the initial event
        LocalGENASubscription subscription = upnpService.getRegistry().getLocalSubscription(subscriptionId);
        assertFalse(subscription.getCurrentValues().isEmpty());

        // Now send the initial event, the subscription should no longer hold the values
        subscribeProt.responseSent(subscribeResponseMessage);
        assertTrue(subscription.getCurrentValues().isEmpty());

        // Same for the next event
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
        assertTrue(subscription.getCurrentValues().isEmpty());

        // Both have been sent with their values
        List<StreamRequestMessage> sentMessages = upnpService.getRouter().getSentStreamRequestMessages();
        assertEquals(2, sentMessages.size());
        for (int i = 0; i < 2; i++) {
            assertEquals((long) i, sentMessages.get(i).getHeaders()
                    .getFirstHeader(UpnpHeader.Type.SEQ, EventSequenceHeader.class).getValue().getValue());
            assertTrue(sentMessages.get(i).getBodyString().contains("<Status>0</Status>"));
        }
    }

    @Test
    void subscriptionInitialEventWithConcurrentStateChange() throws Exception {
        AtomicBoolean stateChanged = new AtomicBoolean();
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected ProtocolFactory createProtocolFactory(UpnpService service, boolean sendsAlive) {
                return new MockProtocolFactory(service, sendsAlive) {
                    @Override
                    public SendingEvent createSendingEvent(LocalGENASubscription subscription) {
                        // "Modify" the state of the service just before the initial event message is prepared
                        if (stateChanged.compareAndSet(false, true)) {
                            subscription.getService().getManager().getPropertyChangeSupport()
                                    .firePropertyChange("Status", false, true);
                        }
                        return super.createSendingEvent(subscription);
                    }
                };
            }
        };
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
        subscribeProt.responseSent(subscribeProt.getOutputMessage());
        assertTrue(stateChanged.get());

        // The subscription wasn't listening yet, so this is the initial event and not a second one without values
        List<StreamRequestMessage> sentMessages = upnpService.getRouter().getSentStreamRequestMessages();
        assertEquals(1, sentMessages.size());
        assertEquals(0L, sentMessages.get(0).getHeaders().getFirstHeader(UpnpHeader.Type.SEQ, EventSequenceHeader.class)
                .getValue().getValue());
        assertTrue(sentMessages.get(0).getBodyString().contains("<Status>0</Status>"));
    }

    @Test
    void eventRejectedBySubscriberEndsSubscription() throws Exception {
        // The callback answers the initial event with 412, and anything after that with 500
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage getStreamResponseMessage(StreamRequestMessage request) {
                        return new StreamResponseMessage(
                                getSentStreamRequestMessages().size() == 1 ? UpnpResponse.Status.PRECONDITION_FAILED
                                        : UpnpResponse.Status.INTERNAL_SERVER_ERROR);
                    }
                };
            }
        };
        upnpService.startup();

        LocalDevice device = GenaSampleData.createTestDevice(GenaSampleData.LocalTestService.class);
        upnpService.getRegistry().addDevice(device);

        Namespace ns = upnpService.getConfiguration().getNamespace();

        LocalService<?> service = SampleData.getFirstService(device);
        URL callbackURL = URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), ns.getEventCallbackPath(service));

        // Two callback URLs, the second one is not tried after the first one has rejected the subscription
        StreamRequestMessage subscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.SUBSCRIBE,
                ns.getEventSubscriptionPath(service));
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.CALLBACK,
                new CallbackHeader(List.of(callbackURL, new URL("http://127.0.0.1:8081/callback"))));
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.NT, new NTEventHeader());

        ReceivingSubscribe subscribeProt = new ReceivingSubscribe(upnpService, subscribeRequestMessage);
        subscribeProt.run();
        OutgoingSubscribeResponseMessage subscribeResponseMessage = subscribeProt.getOutputMessage();
        String subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();
        assertNotNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));

        // The initial event is answered with 412, the subscriber doesn't know this subscription
        subscribeProt.responseSent(subscribeResponseMessage);
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));

        // No more events, the subscription no longer listens to state changes of the service
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());

        // And it can't be renewed
        StreamRequestMessage renewalRequestMessage = new StreamRequestMessage(UpnpRequest.Method.SUBSCRIBE,
                ns.getEventSubscriptionPath(service));
        renewalRequestMessage.getHeaders().add(UpnpHeader.Type.SID, new SubscriptionIdHeader(subscriptionId));

        ReceivingSubscribe renewalProt = new ReceivingSubscribe(upnpService, renewalRequestMessage);
        renewalProt.run();
        assertEquals(UpnpResponse.Status.PRECONDITION_FAILED.getStatusCode(),
                renewalProt.getOutputMessage().getOperation().getStatusCode());
    }

    @Test
    void eventFailedForOtherReasonKeepsSubscription() throws Exception {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage getStreamResponseMessage(StreamRequestMessage request) {
                        // No response at all for the initial event, then an error of the callback server
                        return getSentStreamRequestMessages().size() == 1 ? null
                                : new StreamResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
                    }
                };
            }
        };
        upnpService.startup();

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
        String subscriptionId = subscribeResponseMessage.getHeaders()
                .getFirstHeader(UpnpHeader.Type.SID, SubscriptionIdHeader.class).getValue();

        subscribeProt.responseSent(subscribeResponseMessage);
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", true, false);

        // Only 412 ends the subscription, it is still there and events are still sent
        assertEquals(3, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertNotNull(upnpService.getRegistry().getLocalSubscription(subscriptionId));
    }
}
