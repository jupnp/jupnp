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
package org.jupnp.local;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.mock.MockUpnpServiceConfiguration;
import org.jupnp.model.DiscoveryOptions;
import org.jupnp.model.Namespace;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.message.OutgoingDatagramMessage;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.util.URIUtil;

/**
 * TODO: These timing-sensitive tests fail sometimes... should use latches instead to coordinate threads
 */
class LocalDeviceBindingAdvertisementTest {

    @Test
    void registerLocalDevice() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(true, true);
        upnpService.startup();

        LocalDevice binaryLight = DemoBinaryLight.createTestDevice();

        upnpService.getRegistry().addDevice(binaryLight);

        Thread.sleep(5000);

        assertEquals(12, upnpService.getRouter().getOutgoingDatagramMessages().size());
        for (UpnpMessage msg : upnpService.getRouter().getOutgoingDatagramMessages()) {
            assertAliveMsgBasics(upnpService.getConfiguration().getNamespace(), msg, binaryLight, 1800);
        }

        upnpService.shutdown();

        DeviceDescriptorBinder dvcBinder = upnpService.getConfiguration().getDeviceDescriptorBinderUDA10();
        String descriptorXml = dvcBinder.generate(binaryLight, new RemoteClientInfo(),
                upnpService.getConfiguration().getNamespace());

        RemoteDevice testDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());

        testDevice = dvcBinder.describe(testDevice, descriptorXml);
        assertEquals("Example Binary Light", testDevice.getDetails().getFriendlyName());

        // TODO: more tests

        ServiceDescriptorBinder svcBinder = upnpService.getConfiguration().getServiceDescriptorBinderUDA10();
        String serviceXml = svcBinder.generate(binaryLight.getServices()[0]);

        // TODO: more tests
    }

    @Test
    void waitForRefresh() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(true, true);
        upnpService.startup();

        LocalDevice ld = SampleData.createLocalDevice(SampleData.createLocalDeviceIdentity(1));

        upnpService.getRegistry().addDevice(ld);
        assertEquals(1, upnpService.getRegistry().getLocalDevices().size());

        Thread.sleep(5000);

        assertEquals(1, upnpService.getRegistry().getLocalDevices().size());

        List<OutgoingDatagramMessage> outgoingDatagramMessages = new ArrayList<>(
                upnpService.getRouter().getOutgoingDatagramMessages());

        // 30 from addDevice()
        // 30 from regular refresh
        assertTrue(outgoingDatagramMessages.size() >= 60);
        for (UpnpMessage msg : outgoingDatagramMessages) {
            assertAliveMsgBasics(upnpService.getConfiguration().getNamespace(), msg, ld, 1);
        }

        upnpService.getRouter().getOutgoingDatagramMessages().clear();

        upnpService.shutdown();

        // Ignore ALIVE messages send during shutdown
        outgoingDatagramMessages = upnpService.getRouter().getOutgoingDatagramMessages().stream().filter(
                msg -> msg.getHeaders().getFirstHeader(UpnpHeader.Type.NTS).getValue() == NotificationSubtype.BYEBYE)
                .collect(Collectors.toList());

        // Check correct byebye
        assertTrue(outgoingDatagramMessages.size() >= 30);
        for (UpnpMessage msg : outgoingDatagramMessages) {
            assertByeByeMsgBasics(upnpService.getConfiguration().getNamespace(), msg, ld, 1);
        }
    }

    @Test
    void waitForAliveFlood() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(true, new MockUpnpServiceConfiguration(true) {
            @Override
            public int getAliveIntervalMillis() {
                return 2000;
            }
        });
        upnpService.startup();

        LocalDevice ld = SampleData.createLocalDevice(SampleData.createLocalDeviceIdentity(1000) // Max age ignored
        );

        upnpService.getRegistry().addDevice(ld);
        assertEquals(1, upnpService.getRegistry().getLocalDevices().size());

        Thread.sleep(5000);

        assertEquals(1, upnpService.getRegistry().getLocalDevices().size());

        // 30 from addDevice()
        // 30 from first flood
        // 30 from second flood
        assertTrue(upnpService.getRouter().getOutgoingDatagramMessages().size() >= 90);
        for (UpnpMessage msg : upnpService.getRouter().getOutgoingDatagramMessages()) {
            assertAliveMsgBasics(upnpService.getConfiguration().getNamespace(), msg, ld, 1000);
        }

        upnpService.shutdown();
    }

    @Test
    void byeByeBeforeAlive() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(true, true);
        upnpService.startup();

        LocalDevice ld = SampleData.createLocalDevice(SampleData.createLocalDeviceIdentity(60));

        upnpService.getRegistry().addDevice(ld, new DiscoveryOptions(true, true));

        Thread.sleep(5000);

        assertTrue(upnpService.getRouter().getOutgoingDatagramMessages().size() >= 60);
        // 30 BYEBYE
        // 30 ALIVE
        int i = 0;
        for (; i < 30; i++) {
            UpnpMessage msg = upnpService.getRouter().getOutgoingDatagramMessages().get(i);
            assertByeByeMsgBasics(upnpService.getConfiguration().getNamespace(), msg, ld, 60);
        }
        for (; i < 60; i++) {
            UpnpMessage msg = upnpService.getRouter().getOutgoingDatagramMessages().get(i);
            assertAliveMsgBasics(upnpService.getConfiguration().getNamespace(), msg, ld, 60);
        }

        upnpService.shutdown();
    }

    @Test
    void registerNonAdvertisedLocalDevice() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(true, true);
        upnpService.startup();

        LocalDevice binaryLight = DemoBinaryLight.createTestDevice();

        upnpService.getRegistry().addDevice(binaryLight, new DiscoveryOptions(false)); // Not advertised

        Thread.sleep(2000);

        assertEquals(0, upnpService.getRouter().getOutgoingDatagramMessages().size());

        upnpService.shutdown();
    }

    protected void assertAliveMsgBasics(Namespace namespace, UpnpMessage msg, LocalDevice device, Integer maxAge) {
        assertEquals(NotificationSubtype.ALIVE, msg.getHeaders().getFirstHeader(UpnpHeader.Type.NTS).getValue());
        assertEquals(
                URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), namespace.getDescriptorPath(device)).toString(),
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION).getValue().toString());
        assertEquals(maxAge, msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE).getValue());
        assertEquals(new ServerClientTokens(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER).getValue());
    }

    protected void assertByeByeMsgBasics(Namespace namespace, UpnpMessage msg, LocalDevice device, Integer maxAge) {
        assertEquals(NotificationSubtype.BYEBYE, msg.getHeaders().getFirstHeader(UpnpHeader.Type.NTS).getValue());
        assertEquals(
                URIUtil.createAbsoluteURL(SampleData.getLocalBaseURL(), namespace.getDescriptorPath(device)).toString(),
                msg.getHeaders().getFirstHeader(UpnpHeader.Type.LOCATION).getValue().toString());
        assertEquals(maxAge, msg.getHeaders().getFirstHeader(UpnpHeader.Type.MAX_AGE).getValue());
        assertEquals(new ServerClientTokens(), msg.getHeaders().getFirstHeader(UpnpHeader.Type.SERVER).getValue());
    }

    @UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
    public static class DemoBinaryLight {

        private static LocalDevice createTestDevice() throws Exception {
            LocalServiceBinder binder = new AnnotationLocalServiceBinder();
            return new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("BinaryLight", 1),
                    new DeviceDetails("Example Binary Light"), binder.read(DemoBinaryLight.class));
        }

        @UpnpStateVariable(defaultValue = "0", sendEvents = false)
        private boolean target = false;

        @UpnpStateVariable(defaultValue = "0")
        private boolean status = false;

        @UpnpAction
        void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {
            target = newTargetValue;
            status = newTargetValue;
        }

        @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
        public boolean getTarget() {
            return target;
        }

        @UpnpAction(out = { @UpnpOutputArgument(name = "ResultStatus") })
        public boolean getStatus() {
            return status;
        }
    }
}
