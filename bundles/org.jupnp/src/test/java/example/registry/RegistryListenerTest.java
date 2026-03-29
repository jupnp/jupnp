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
package example.registry;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.protocol.RetrieveRemoteDescriptors;
import org.jupnp.registry.DefaultRegistryListener;
import org.jupnp.registry.Registry;

class RegistryListenerTest {

    // Just for documentation inclusion!
    public interface RegistryListener {

        void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device);

        void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception e);

        void remoteDeviceAdded(Registry registry, RemoteDevice device);

        void remoteDeviceUpdated(Registry registry, RemoteDevice device);

        void remoteDeviceRemoved(Registry registry, RemoteDevice device);

        void localDeviceAdded(Registry registry, LocalDevice device);

        void localDeviceRemoved(Registry registry, LocalDevice device);
    }

    @Test
    void quickstartListener() throws Exception {
        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        try {
                            String deviceDescriptorXML = getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice, new RemoteClientInfo(), getConfiguration().getNamespace());
                            String serviceOneXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[0]);
                            String serviceTwoXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[1]);
                            String serviceThreeXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[2]);
                            return new StreamResponseMessage[] {
                                    new StreamResponseMessage(deviceDescriptorXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceOneXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceTwoXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceThreeXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8) };
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };
        upnpService.startup();

        QuickstartRegistryListener listener = new QuickstartRegistryListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        assertTrue(listener.valid);
    }

    @Test
    void failureQuickstartListener() throws Exception {
        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        String deviceDescriptorXML;
                        DeviceDescriptorBinder binder = getConfiguration().getDeviceDescriptorBinderUDA10();
                        try {
                            deviceDescriptorXML = binder.generate(hydratedDevice, new RemoteClientInfo(),
                                    getConfiguration().getNamespace());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return new StreamResponseMessage[] { new StreamResponseMessage(deviceDescriptorXML,
                                ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8), null, null, null // Don't return any
                                                                                               // service descriptors,
                                                                                               // make it fail
                        };
                    }
                };
            }
        };
        upnpService.startup();

        FailureQuickstartRegistryListener listener = new FailureQuickstartRegistryListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        assertTrue(listener.valid);
    }

    public static class QuickstartRegistryListener extends DefaultRegistryListener {
        public boolean valid = false;

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {

            // You can already use the device here and you can see which services it will have
            assertEquals(3, device.findServices().length);

            // But you can't use the services
            for (RemoteService service : device.findServices()) {
                assertEquals(0, service.getActions().length);
                assertEquals(0, service.getStateVariables().length);
            }
            valid = true;
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception e) {
            // You might want to drop the device, its services couldn't be hydrated
        }
    }

    public static class FailureQuickstartRegistryListener extends DefaultRegistryListener {
        public boolean valid = false;

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception e) {
            valid = true;
        }
    }

    @Test
    void regularListener() throws Exception {

        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        try {
                            String deviceDescriptorXML = getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice, new RemoteClientInfo(), getConfiguration().getNamespace());
                            String serviceOneXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[0]);
                            String serviceTwoXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[1]);
                            String serviceThreeXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[2]);
                            return new StreamResponseMessage[] {
                                    new StreamResponseMessage(deviceDescriptorXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceOneXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceTwoXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceThreeXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8) };
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };
        upnpService.startup();

        MyListener listener = new MyListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        upnpService.getRegistry().removeAllRemoteDevices();

        assertTrue(listener.added);
        assertTrue(listener.removed);
    }

    @Test
    void ipAddressChangeOnRegisteredDevice() throws Exception {
        final RemoteDevice discoveredDevice = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        final RemoteDevice hydratedDevice = SampleData.createRemoteDevice();

        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        try {
                            String deviceDescriptorXML = getConfiguration().getDeviceDescriptorBinderUDA10().generate(
                                    hydratedDevice, new RemoteClientInfo(), getConfiguration().getNamespace());
                            String serviceOneXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[0]);
                            String serviceTwoXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[1]);
                            String serviceThreeXML = getConfiguration().getServiceDescriptorBinderUDA10()
                                    .generate(hydratedDevice.findServices()[2]);
                            return new StreamResponseMessage[] {
                                    new StreamResponseMessage(deviceDescriptorXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceOneXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceTwoXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8),
                                    new StreamResponseMessage(serviceThreeXML,
                                            ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8) };
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        };
        upnpService.startup();

        MyListener listener = new MyListener();
        upnpService.getRegistry().addListener(listener);

        RetrieveRemoteDescriptors retrieveDescriptors = new RetrieveRemoteDescriptors(upnpService, discoveredDevice);
        retrieveDescriptors.run();

        assertTrue(listener.added);
        assertFalse(listener.removed);

        listener.reset();

        upnpService.getRegistry().addDevice(new RemoteDevice(SampleData.createSecondRemoteDeviceIdentity(1800)));

        assertTrue(listener.added);
        assertTrue(listener.removed);
        assertEquals("127.0.0.2", listener.deviceAdded.getIdentity().getDescriptorURL().getHost());
        assertEquals("127.0.0.1", listener.deviceRemoved.getIdentity().getDescriptorURL().getHost());

        listener.reset();

        upnpService.getRegistry().removeAllRemoteDevices();
        assertFalse(listener.added);
        assertTrue(listener.removed);
    }

    public static class MyListener extends DefaultRegistryListener {
        public boolean added = false;
        public boolean removed = false;
        public RemoteDevice deviceAdded = null;
        public RemoteDevice deviceRemoved = null;

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            added = true;
            deviceAdded = device;
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            // Stop using the service if this is the same device, it's gone now
            removed = true;
            deviceRemoved = device;
        }

        void reset() {
            added = false;
            removed = false;
            deviceAdded = null;
            deviceRemoved = null;
        }
    }
}
