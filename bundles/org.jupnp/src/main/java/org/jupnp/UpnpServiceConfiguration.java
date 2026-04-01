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
package org.jupnp;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.model.Namespace;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.ServiceType;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.SOAPActionProcessor;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

/**
 * Shared configuration data of the UPnP stack..
 * <p>
 * This interface offers methods for retrieval of configuration data by the
 * {@link org.jupnp.transport.Router} and the {@link org.jupnp.registry.Registry},
 * as well as other parts of the UPnP stack.
 * </p>
 * <p>
 * You can re-use this interface if you implement a subclass of {@link UpnpServiceImpl} or
 * if you create a new implementation of {@link UpnpService}.
 * </p>
 *
 * @author Christian Bauer
 */
public interface UpnpServiceConfiguration {

    /**
     * @return A new instance of the {@link org.jupnp.transport.spi.NetworkAddressFactory} interface.
     */
    NetworkAddressFactory createNetworkAddressFactory();

    /**
     * @return The shared implementation of {@link org.jupnp.transport.spi.DatagramProcessor}.
     */
    DatagramProcessor getDatagramProcessor();

    /**
     * @return The shared implementation of {@link org.jupnp.transport.spi.SOAPActionProcessor}.
     */
    SOAPActionProcessor getSoapActionProcessor();

    /**
     * @return The shared implementation of {@link org.jupnp.transport.spi.GENAEventProcessor}.
     */
    GENAEventProcessor getGenaEventProcessor();

    /**
     * @return A new instance of the {@link org.jupnp.transport.spi.StreamClient} interface.
     */
    StreamClient createStreamClient();

    /**
     * @param networkAddressFactory The configured {@link org.jupnp.transport.spi.NetworkAddressFactory}.
     * @return A new instance of the {@link org.jupnp.transport.spi.MulticastReceiver} interface.
     */
    MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory);

    /**
     * @param networkAddressFactory The configured {@link org.jupnp.transport.spi.NetworkAddressFactory}.
     * @return A new instance of the {@link org.jupnp.transport.spi.DatagramIO} interface.
     */
    DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory);

    /**
     * @param networkAddressFactory The configured {@link org.jupnp.transport.spi.NetworkAddressFactory}.
     * @return A new instance of the {@link org.jupnp.transport.spi.StreamServer} interface.
     */
    StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory);

    /**
     * @return The executor which runs the listening background threads for multicast datagrams.
     */
    Executor getMulticastReceiverExecutor();

    /**
     * @return The executor which runs the listening background threads for unicast datagrams.
     */
    Executor getDatagramIOExecutor();

    /**
     * @return The executor which runs the listening background threads for HTTP requests.
     */
    ExecutorService getStreamServerExecutorService();

    /**
     * @return The shared implementation of {@link org.jupnp.binding.xml.DeviceDescriptorBinder} for the UPnP 1.0 Device
     *         Architecture..
     */
    DeviceDescriptorBinder getDeviceDescriptorBinderUDA10();

    /**
     * @return The shared implementation of {@link org.jupnp.binding.xml.ServiceDescriptorBinder} for the UPnP 1.0
     *         Device Architecture..
     */
    ServiceDescriptorBinder getServiceDescriptorBinderUDA10();

    /**
     * Returns service types that can be handled by this UPnP stack, all others will be ignored.
     * <p>
     * Return <code>null</code> to completely disable remote device and service discovery.
     * All incoming notifications and search responses will then be dropped immediately.
     * This is mostly useful in applications that only provide services with no (remote)
     * control point functionality.
     * </p>
     * <p>
     * Note that a discovered service type with version 2 or 3 will match an exclusive
     * service type with version 1. UPnP services are required to be backwards
     * compatible, version 2 is a superset of version 1, and version 3 is a superset
     * of version 2, etc.
     * </p>
     *
     * @return An array of service types that are exclusively discovered, no other service will
     *         be discovered. A <code>null</code> return value will disable discovery!
     *         An empty array means all services will be discovered.
     */
    ServiceType[] getExclusiveServiceTypes();

    /**
     * @return The time in milliseconds to wait between each registry maintenance operation.
     */
    int getRegistryMaintenanceIntervalMillis();

    /**
     * Optional setting for flooding alive NOTIFY messages for local devices.
     * <p>
     * Use this to advertise local devices at the specified interval, independent of its
     * {@link org.jupnp.model.meta.DeviceIdentity#maxAgeSeconds} value. Note
     * that this will increase network traffic.
     * </p>
     * <p>
     * Some control points (XBMC and other Platinum UPnP SDK based devices, OPPO-93) seem
     * to not properly receive SSDP M-SEARCH replies sent by jUPnP, but will handle NOTIFY
     * alive messages just fine.
     * </p>
     *
     * @return The time in milliseconds for ALIVE message intervals, set to <code>0</code> to disable
     */
    int getAliveIntervalMillis();

    /**
     * Ignore the received event subscription timeout from remote control points.
     * <p>
     * Some control points have trouble renewing subscriptions properly; enabling this option
     * in conjunction with a high value for
     * {@link org.jupnp.model.UserConstants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS}
     * ensures that your devices will not disappear on such control points.
     * </p>
     *
     * @return <code>true</code> if the timeout in incoming event subscriptions should be ignored
     *         and the default value ({@link org.jupnp.model.UserConstants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS})
     *         should be used instead.
     *
     */
    boolean isReceivedSubscriptionTimeoutIgnored();

    /**
     * Returns the time in seconds a remote device will be registered until it is expired.
     * <p>
     * This setting is useful on systems which do not support multicast networking
     * (Android on HTC phones, for example). On such a system you will not receive messages when a
     * remote device disappears from the network and you will not receive its periodic heartbeat
     * alive messages. Only an initial search response (UDP unicast) has been received from the
     * remote device, with its proposed maximum age. To avoid (early) expiration of the remote
     * device, you can override its maxium age with this configuration setting, ignoring the
     * initial maximum age sent by the device. You most likely want to return
     * <code>0</code> in this case, so that the remote device is never expired unless you
     * manually remove it from the {@link org.jupnp.registry.Registry}. You typically remove
     * the device when an action or GENA subscription request to the remote device failed.
     * </p>
     *
     * @return <code>null</code> (the default) to accept the remote device's proposed maximum age, or
     *         <code>0</code> for unlimited age, or a value in seconds.
     */
    Integer getRemoteDeviceMaxAgeSeconds();

    /**
     * Optional extra headers for device descriptor retrieval HTTP requests.
     * <p>
     * Some devices might require extra headers to recognize your control point, use this
     * method to set these headers. They will be used for every descriptor (XML) retrieval
     * HTTP request by jUPnP. See {@link org.jupnp.model.profile.ClientInfo} for
     * action request messages.
     * </p>
     *
     * @param identity The (so far) discovered identity of the remote device.
     * @return <code>null</code> or extra HTTP headers.
     */
    UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity identity);

    /**
     * Optional extra headers for event subscription (almost HTTP) messages.
     * <p>
     * Some devices might require extra headers to recognize your control point, use this
     * method to set these headers for GENA subscriptions. Note that the headers will
     * not be applied to actual event messages, only subscribe, unsubscribe, and renewal.
     * </p>
     *
     * @return <code>null</code> or extra HTTP headers.
     */
    UpnpHeaders getEventSubscriptionHeaders(RemoteService service);

    /**
     * @return The executor which runs the processing of asynchronous aspects of the UPnP stack (discovery).
     */
    Executor getAsyncProtocolExecutor();

    /**
     * @return The executor service which runs the processing of synchronous aspects of the UPnP stack (description,
     *         control, GENA).
     */
    ExecutorService getSyncProtocolExecutorService();

    /**
     * @return An instance of {@link org.jupnp.model.Namespace} for this UPnP stack.
     */
    Namespace getNamespace();

    /**
     * @return The executor which runs the background thread for maintaining the registry.
     */
    Executor getRegistryMaintainerExecutor();

    /**
     * @return The executor which runs the notification threads of registry listeners.
     */
    Executor getRegistryListenerExecutor();

    /**
     * @return The executor which runs the remote listener threads.
     */
    Executor getRemoteListenerExecutor();

    /**
     * Called by the {@link org.jupnp.UpnpService} on shutdown, useful to e.g. shutdown thread pools.
     */
    void shutdown();
}
