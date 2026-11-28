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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.CallbackHeader;
import org.jupnp.model.message.header.NTEventHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.protocol.sync.ReceivingSubscribe;

/**
 * UDA 1.0 section 4.1.1: "If there is more than 1 URL, when the service sends events, it will try these URLs in
 * order until one succeeds."
 */
class EventCallbackURLsTest {

    @Test
    void eventIsSentToFirstCallbackURLOnly() throws Exception {
        List<String> notified = subscribeAndChangeState(UpnpResponse.Status.OK, UpnpResponse.Status.OK,
                UpnpResponse.Status.OK);

        // The initial event and the one for the state change
        assertEquals(List.of("http://192.168.1.20:8081/0", "http://192.168.1.20:8081/0"), notified);
    }

    @Test
    void eventIsSentToNextCallbackURLAfterFailure() throws Exception {
        List<String> notified = subscribeAndChangeState(UpnpResponse.Status.INTERNAL_SERVER_ERROR, null,
                UpnpResponse.Status.OK, UpnpResponse.Status.OK);

        assertEquals(
                List.of("http://192.168.1.20:8081/0", "http://192.168.1.20:8081/1", "http://192.168.1.20:8081/2",
                        "http://192.168.1.20:8081/0", "http://192.168.1.20:8081/1", "http://192.168.1.20:8081/2"),
                notified);
    }

    /**
     * @param callbackStatus The answer of each callback URL of the subscription, <code>null</code> for none.
     * @return The URLs the event messages have been sent to.
     */
    private List<String> subscribeAndChangeState(UpnpResponse.Status... callbackStatus) throws Exception {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage getStreamResponseMessage(StreamRequestMessage request) {
                        String path = request.getOperation().getURI().getPath();
                        UpnpResponse.Status status = callbackStatus[Integer.parseInt(path.substring(1))];
                        return status != null ? new StreamResponseMessage(status) : null;
                    }
                };
            }
        };

        ReceivingSubscribe subscribeProt = subscribe(upnpService, callbackStatus.length);
        assertEquals(UpnpResponse.Status.OK.getStatusCode(),
                subscribeProt.getOutputMessage().getOperation().getStatusCode());

        // Send the initial event, then "modify" the state of the service
        subscribeProt.responseSent(subscribeProt.getOutputMessage());
        LocalService<?> service = SampleData
                .getFirstService(upnpService.getRegistry().getLocalDevices().iterator().next());
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);

        List<String> notified = new ArrayList<>();
        for (StreamRequestMessage sentMessage : upnpService.getRouter().getSentStreamRequestMessages()) {
            assertEquals(UpnpRequest.Method.NOTIFY, sentMessage.getOperation().getMethod());
            notified.add(sentMessage.getOperation().getURI().toString());
        }
        return notified;
    }

    private ReceivingSubscribe subscribe(MockUpnpService upnpService, int callbackURLs) throws Exception {
        upnpService.startup();

        LocalDevice device = GenaSampleData.createTestDevice(GenaSampleData.LocalTestService.class);
        upnpService.getRegistry().addDevice(device);

        LocalService<?> service = SampleData.getFirstService(device);

        StreamRequestMessage subscribeRequestMessage = new StreamRequestMessage(UpnpRequest.Method.SUBSCRIBE,
                upnpService.getConfiguration().getNamespace().getEventSubscriptionPath(service));

        List<URL> urls = new ArrayList<>();
        for (int i = 0; i < callbackURLs; i++) {
            urls.add(new URL("http://192.168.1.20:8081/" + i));
        }
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.CALLBACK, new CallbackHeader(urls));
        subscribeRequestMessage.getHeaders().add(UpnpHeader.Type.NT, new NTEventHeader());

        ReceivingSubscribe subscribeProt = new ReceivingSubscribe(upnpService, subscribeRequestMessage);
        subscribeProt.run();
        return subscribeProt;
    }
}
