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
package example.controlpoint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.SubscriptionIdHeader;
import org.jupnp.model.message.header.TimeoutHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.BooleanDatatype;
import org.jupnp.model.types.Datatype;
import org.jupnp.util.Reflections;

import example.binarylight.BinaryLightSampleData;
import example.binarylight.SwitchPower;

public class EventSubscriptionTest {

    @Test
    void subscriptionLifecycle() throws Exception {

        MockUpnpService upnpService = createMockUpnpService();

        final List<Boolean> testAssertions = new ArrayList<>();

        // Register local device and its service
        LocalDevice device = BinaryLightSampleData.createDevice(SwitchPower.class);
        upnpService.getRegistry().addDevice(device);

        LocalService service = device.getServices()[0];

        SubscriptionCallback callback = new SubscriptionCallback(service, 600) {

            @Override
            public void established(GENASubscription sub) {
                System.out.println("Established: " + sub.getSubscriptionId());
                testAssertions.add(true);
            }

            @Override
            protected void failed(GENASubscription subscription, UpnpResponse responseStatus, Exception exception,
                    String defaultMsg) {
                System.err.println(defaultMsg);
                testAssertions.add(false);
            }

            @Override
            public void ended(GENASubscription sub, CancelReason reason, UpnpResponse response) {
                assertNull(reason);
                assertNotNull(sub);
                assertNull(response);
                testAssertions.add(true);
            }

            @Override
            public void eventReceived(GENASubscription sub) {

                System.out.println("Event: " + sub.getCurrentSequence().getValue());

                Map<String, StateVariableValue> values = sub.getCurrentValues();
                StateVariableValue status = values.get("Status");

                assertEquals(BooleanDatatype.class, status.getDatatype().getClass());
                assertEquals(Datatype.Builtin.BOOLEAN, status.getDatatype().getBuiltin());

                System.out.println("Status is: " + status);

                if (sub.getCurrentSequence().getValue() == 0) {
                    assertEquals("0", sub.getCurrentValues().get("Status").toString());
                    testAssertions.add(true);
                } else if (sub.getCurrentSequence().getValue() == 1) {
                    assertEquals("1", sub.getCurrentValues().get("Status").toString());
                    testAssertions.add(true);
                } else {
                    testAssertions.add(false);
                }
            }

            @Override
            public void eventsMissed(GENASubscription sub, int numberOfMissedEvents) {
                System.out.println("Missed events: " + numberOfMissedEvents);
                testAssertions.add(false);
            }

            @Override
            protected void invalidMessage(RemoteGENASubscription sub, UnsupportedDataException e) {
                // Log/send an error report?
            }
        };

        upnpService.getControlPoint().execute(callback);

        // Modify the state of the service and trigger event
        Object serviceImpl = service.getManager().getImplementation();
        Reflections.set(Reflections.getField(serviceImpl.getClass(), "status"), serviceImpl, true);
        service.getManager().getPropertyChangeSupport().firePropertyChange("Status", false, true);

        assertEquals(2L, callback.getSubscription().getCurrentSequence().getValue()); // It's the NEXT sequence!
        assertTrue(callback.getSubscription().getSubscriptionId().startsWith("uuid:"));

        // Actually, the local subscription we are testing here has an "unlimited" duration
        assertEquals(Integer.MAX_VALUE, callback.getSubscription().getActualDurationSeconds());

        callback.end();

        assertEquals(4, testAssertions.size());
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    protected MockUpnpService createMockUpnpService() {
        MockUpnpService mock = new MockUpnpService() {
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
        mock.startup();
        return mock;
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
}
