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
package org.jupnp.data;

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;

import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.DeviceIdentity;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.profile.DeviceDetailsProvider;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;

public class SampleData {

    /* ###################################################################################### */

    public static InetAddress getLocalBaseAddress() {
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static InetAddress getSecondLocalBaseAddress() {
        try {
            return InetAddress.getByName("127.0.0.2");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static URL getLocalBaseURL() {
        try {
            return new URL("http://127.0.0.1:" + NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static URL getSecondLocalBaseURL() {
        try {
            return new URL("http://127.0.0.2:" + NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT + "/");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /* ###################################################################################### */

    public static DeviceIdentity createLocalDeviceIdentity() {
        return createLocalDeviceIdentity(1800);
    }

    public static DeviceIdentity createLocalDeviceIdentity(int maxAgeSeconds) {
        return new DeviceIdentity(SampleDeviceRoot.getRootUDN(), maxAgeSeconds);
    }

    public static LocalDevice createLocalDevice() {
        return createLocalDevice(false);
    }

    public static LocalDevice createLocalDevice(boolean useProvider) {
        return createLocalDevice(createLocalDeviceIdentity(), useProvider);
    }

    public static Constructor<LocalDevice> getLocalDeviceConstructor() {
        try {
            return LocalDevice.class.getConstructor(DeviceIdentity.class, DeviceType.class, DeviceDetails.class,
                    Icon[].class, LocalService.class, LocalDevice.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Constructor<LocalDevice> getLocalDeviceWithProviderConstructor() {
        try {
            return LocalDevice.class.getConstructor(DeviceIdentity.class, DeviceType.class, DeviceDetailsProvider.class,
                    Icon[].class, LocalService.class, LocalDevice.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Constructor<LocalService> getLocalServiceConstructor() {
        try {
            return LocalService.class.getConstructor(ServiceType.class, ServiceId.class, Action[].class,
                    StateVariable[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalDevice createLocalDevice(DeviceIdentity identity) {
        return createLocalDevice(identity, false);
    }

    public static LocalDevice createLocalDevice(DeviceIdentity identity, boolean useProvider) {
        try {

            Constructor<LocalDevice> ctor = useProvider ? getLocalDeviceWithProviderConstructor()
                    : getLocalDeviceConstructor();

            Constructor<LocalService> serviceConstructor = getLocalServiceConstructor();

            return new SampleDeviceRootLocal(identity, new SampleServiceOne().newInstanceLocal(serviceConstructor),
                    new SampleDeviceEmbeddedOne(
                            new DeviceIdentity(SampleDeviceEmbeddedOne.getEmbeddedOneUDN(), identity),
                            new SampleServiceTwo().newInstanceLocal(serviceConstructor),
                            new SampleDeviceEmbeddedTwo(
                                    new DeviceIdentity(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN(), identity),
                                    new SampleServiceThree().newInstanceLocal(serviceConstructor), null)
                                    .newInstance(ctor, useProvider))
                            .newInstance(ctor, useProvider))
                    .newInstance(ctor, useProvider);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalService getFirstService(LocalDevice device) {
        return device.getServices()[0];
    }

    /* ###################################################################################### */

    public static RemoteDeviceIdentity createRemoteDeviceIdentity() {
        return createRemoteDeviceIdentity(1800);
    }

    public static RemoteDeviceIdentity createRemoteDeviceIdentity(int maxAgeSeconds) {
        return new RemoteDeviceIdentity(SampleDeviceRoot.getRootUDN(), maxAgeSeconds,
                SampleDeviceRoot.getDeviceDescriptorURL(), null, getLocalBaseAddress());
    }

    public static RemoteDeviceIdentity createSecondRemoteDeviceIdentity(int maxAgeSeconds) {
        return new RemoteDeviceIdentity(SampleDeviceRoot.getRootUDN(), maxAgeSeconds,
                SampleDeviceRoot.getSecondDeviceDescriptorURL(), null, getSecondLocalBaseAddress());
    }

    public static RemoteDevice createRemoteDevice() {
        return createRemoteDevice(createRemoteDeviceIdentity());
    }

    public static Constructor<RemoteDevice> getRemoteDeviceConstructor() {
        try {
            return RemoteDevice.class.getConstructor(RemoteDeviceIdentity.class, DeviceType.class, DeviceDetails.class,
                    Icon[].class, RemoteService.class, RemoteDevice.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Constructor<RemoteService> getRemoteServiceConstructor() {
        try {
            return RemoteService.class.getConstructor(ServiceType.class, ServiceId.class, URI.class, URI.class,
                    URI.class, Action[].class, StateVariable[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RemoteDevice createRemoteDevice(RemoteDeviceIdentity identity) {
        try {

            Constructor<RemoteDevice> ctor = getRemoteDeviceConstructor();
            Constructor<RemoteService> serviceConstructor = getRemoteServiceConstructor();

            return new SampleDeviceRoot(identity, new SampleServiceOne().newInstanceRemote(serviceConstructor),
                    new SampleDeviceEmbeddedOne(
                            new RemoteDeviceIdentity(SampleDeviceEmbeddedOne.getEmbeddedOneUDN(), identity),
                            new SampleServiceTwo().newInstanceRemote(serviceConstructor),
                            new SampleDeviceEmbeddedTwo(
                                    new RemoteDeviceIdentity(SampleDeviceEmbeddedTwo.getEmbeddedTwoUDN(), identity),
                                    new SampleServiceThree().newInstanceRemote(serviceConstructor), null)
                                    .newInstance(ctor))
                            .newInstance(ctor))
                    .newInstance(ctor);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static RemoteService getFirstService(RemoteDevice device) {
        return device.getServices()[0];
    }

    public static RemoteService createUndescribedRemoteService() {
        RemoteService service = new SampleServiceOneUndescribed()
                .newInstanceRemote(SampleData.getRemoteServiceConstructor());
        new SampleDeviceRoot(SampleData.createRemoteDeviceIdentity(), service, null)
                .newInstance(SampleData.getRemoteDeviceConstructor());
        return service;
    }

    /* ###################################################################################### */

    public static <T> LocalService<T> readService(Class<T> clazz) {
        return readService(new AnnotationLocalServiceBinder().read(clazz), clazz);
    }

    public static <T> LocalService<T> readService(LocalServiceBinder binder, Class<T> clazz) {
        return readService(binder.read(clazz), clazz);
    }

    public static <T> LocalService<T> readService(LocalService<T> service, Class<T> clazz) {
        service.setManager(new DefaultServiceManager<>(service, clazz));
        return service;
    }
}
