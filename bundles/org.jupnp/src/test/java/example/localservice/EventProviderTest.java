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
package example.localservice;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.jupnp.controlpoint.SubscriptionCallback;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.GENASubscription;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.util.Reflections;

import example.binarylight.BinaryLightSampleData;
import example.controlpoint.EventSubscriptionTest;

class EventProviderTest extends EventSubscriptionTest {

    @Test
    void subscriptionLifecycleChangeSupport() throws Exception {

        MockUpnpService upnpService = createMockUpnpService();

        final List<Boolean> testAssertions = new ArrayList<>();

        // Register local device and its service
        LocalDevice device = BinaryLightSampleData.createDevice(SwitchPowerWithPropertyChangeSupport.class);
        upnpService.getRegistry().addDevice(device);

        LocalService<SwitchPowerWithPropertyChangeSupport> service = SampleData.getFirstService(device);

        SubscriptionCallback callback = new SubscriptionCallback(service, 180) {

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
                assertNotNull(subscription);
                assertNull(reason);
                assertNull(responseStatus);
                testAssertions.add(true);
            }

            @Override
            public void eventReceived(GENASubscription subscription) {
                if (subscription.getCurrentSequence().getValue() == 0) {
                    assertEquals("0", subscription.getCurrentValues().get("Status").toString());
                    testAssertions.add(true);
                } else if (subscription.getCurrentSequence().getValue() == 1) {
                    assertEquals("1", subscription.getCurrentValues().get("Status").toString());
                    testAssertions.add(true);
                } else {
                    testAssertions.add(false);
                }
            }

            @Override
            public void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
                testAssertions.add(false);
            }
        };

        upnpService.getControlPoint().execute(callback);

        // This triggers the internal PropertyChangeSupport of the service impl!
        service.getManager().getImplementation().setTarget(true);

        assertEquals(2L, callback.getSubscription().getCurrentSequence().getValue()); // It's the NEXT sequence!
        assertTrue(callback.getSubscription().getSubscriptionId().startsWith("uuid:"));
        assertEquals(Integer.MAX_VALUE, callback.getSubscription().getActualDurationSeconds());

        callback.end();

        assertEquals(4, testAssertions.size());
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    @Test
    void bundleSeveralVariables() throws Exception {

        MockUpnpService upnpService = createMockUpnpService();

        final List<Boolean> testAssertions = new ArrayList<>();

        // Register local device and its service
        LocalDevice device = BinaryLightSampleData.createDevice(SwitchPowerWithBundledPropertyChange.class);
        upnpService.getRegistry().addDevice(device);

        LocalService<SwitchPowerWithBundledPropertyChange> service = SampleData.getFirstService(device);

        SubscriptionCallback callback = new SubscriptionCallback(service, 180) {

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
                assertNotNull(subscription);
                assertNull(reason);
                assertNull(responseStatus);
                testAssertions.add(true);
            }

            @Override
            public void eventReceived(GENASubscription subscription) {
                if (subscription.getCurrentSequence().getValue() == 0) {
                    assertEquals("0", subscription.getCurrentValues().get("Target").toString());
                    assertEquals("0", subscription.getCurrentValues().get("Status").toString());
                    testAssertions.add(true);
                } else if (subscription.getCurrentSequence().getValue() == 1) {
                    assertEquals("1", subscription.getCurrentValues().get("Target").toString());
                    assertEquals("1", subscription.getCurrentValues().get("Status").toString());
                    testAssertions.add(true);
                } else {
                    testAssertions.add(false);
                }
            }

            @Override
            public void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
                testAssertions.add(false);
            }
        };

        upnpService.getControlPoint().execute(callback);

        // This triggers the internal PropertyChangeSupport of the service impl!
        service.getManager().getImplementation().setTarget(true);

        assertEquals(2L, callback.getSubscription().getCurrentSequence().getValue()); // It's the NEXT sequence!
        assertTrue(callback.getSubscription().getSubscriptionId().startsWith("uuid:"));
        assertEquals(Integer.MAX_VALUE, callback.getSubscription().getActualDurationSeconds());

        callback.end();

        assertEquals(4, testAssertions.size());
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    @Test
    void moderateMaxRate() throws Exception {
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

        // Register local device and its service
        LocalDevice device = BinaryLightSampleData.createDevice(SwitchPowerModerated.class);
        upnpService.getRegistry().addDevice(device);

        LocalService service = SampleData.getFirstService(device);

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
                assertNotNull(subscription);
                assertNull(reason);
                assertNull(responseStatus);
                testAssertions.add(true);
            }

            @Override
            public void eventReceived(GENASubscription subscription) {
                if (subscription.getCurrentSequence().getValue() == 0) {

                    // Initial event contains all evented variables, snapshot of the service state
                    assertEquals("0", subscription.getCurrentValues().get("Status").toString());
                    assertEquals("1", subscription.getCurrentValues().get("ModeratedMinDeltaVar").toString());

                    // Initial state
                    assertEquals("one", subscription.getCurrentValues().get("ModeratedMaxRateVar").toString());

                    testAssertions.add(true);
                } else if (subscription.getCurrentSequence().getValue() == 1) {

                    // Subsequent events do NOT contain unchanged variables
                    assertNull(subscription.getCurrentValues().get("Status"));
                    assertNull(subscription.getCurrentValues().get("ModeratedMinDeltaVar"));

                    // We didn't see the intermediate values "two" and "three" because it's moderated
                    assertEquals("four", subscription.getCurrentValues().get("ModeratedMaxRateVar").toString());

                    testAssertions.add(true);
                } else {
                    testAssertions.add(false);
                }
            }

            @Override
            public void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
                testAssertions.add(false);
            }
        };

        upnpService.getControlPoint().execute(callback);

        Thread.sleep(800);

        Object serviceImpl = service.getManager().getImplementation();

        Reflections.set(Reflections.getField(serviceImpl.getClass(), "moderatedMaxRateVar"), serviceImpl, "two");
        service.getManager().getPropertyChangeSupport().firePropertyChange("ModeratedMaxRateVar", null, null);

        Thread.sleep(800);

        Reflections.set(Reflections.getField(serviceImpl.getClass(), "moderatedMaxRateVar"), serviceImpl, "three");
        service.getManager().getPropertyChangeSupport().firePropertyChange("ModeratedMaxRateVar", null, null);

        Thread.sleep(800);

        Reflections.set(Reflections.getField(serviceImpl.getClass(), "moderatedMaxRateVar"), serviceImpl, "four");
        service.getManager().getPropertyChangeSupport().firePropertyChange("ModeratedMaxRateVar", null, null);

        Thread.sleep(400);

        assertEquals(2L, callback.getSubscription().getCurrentSequence().getValue()); // It's the NEXT sequence!

        callback.end();

        assertEquals(4, testAssertions.size());
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }

    @Test
    void moderateMinDelta() throws Exception {

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

        // Register local device and its service
        LocalDevice device = BinaryLightSampleData.createDevice(SwitchPowerModerated.class);
        upnpService.getRegistry().addDevice(device);

        LocalService service = SampleData.getFirstService(device);

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
                assertNotNull(subscription);
                assertNull(reason);
                assertNull(responseStatus);
                testAssertions.add(true);
            }

            @Override
            public void eventReceived(GENASubscription subscription) {
                if (subscription.getCurrentSequence().getValue() == 0) {

                    // Initial event contains all evented variables, snapshot of the service state
                    assertEquals("0", subscription.getCurrentValues().get("Status").toString());
                    assertEquals("one", subscription.getCurrentValues().get("ModeratedMaxRateVar").toString());

                    // Initial state
                    assertEquals("1", subscription.getCurrentValues().get("ModeratedMinDeltaVar").toString());

                    testAssertions.add(true);
                } else if (subscription.getCurrentSequence().getValue() == 1) {

                    // Subsequent events do NOT contain unchanged variables
                    assertNull(subscription.getCurrentValues().get("Status"));
                    assertNull(subscription.getCurrentValues().get("ModeratedMaxRateVar"));

                    // We didn't get events for values 2 and 3
                    assertEquals("4", subscription.getCurrentValues().get("ModeratedMinDeltaVar").toString());

                    testAssertions.add(true);
                } else {
                    testAssertions.add(false);
                }
            }

            @Override
            public void eventsMissed(GENASubscription subscription, int numberOfMissedEvents) {
                testAssertions.add(false);
            }
        };

        upnpService.getControlPoint().execute(callback);

        Object serviceImpl = service.getManager().getImplementation();

        Reflections.set(Reflections.getField(serviceImpl.getClass(), "moderatedMinDeltaVar"), serviceImpl, 2);
        service.getManager().getPropertyChangeSupport().firePropertyChange("ModeratedMinDeltaVar", 1, 2);

        Reflections.set(Reflections.getField(serviceImpl.getClass(), "moderatedMinDeltaVar"), serviceImpl, 3);
        service.getManager().getPropertyChangeSupport().firePropertyChange("ModeratedMinDeltaVar", 2, 3);

        Reflections.set(Reflections.getField(serviceImpl.getClass(), "moderatedMinDeltaVar"), serviceImpl, 4);
        service.getManager().getPropertyChangeSupport().firePropertyChange("ModeratedMinDeltaVar", 3, 4);

        assertEquals(callback.getSubscription().getCurrentSequence().getValue(), Long.valueOf(2)); // It's the NEXT
                                                                                                   // sequence!

        callback.end();

        assertEquals(4, testAssertions.size());
        for (Boolean testAssertion : testAssertions) {
            assertTrue(testAssertion);
        }

        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
    }
}
