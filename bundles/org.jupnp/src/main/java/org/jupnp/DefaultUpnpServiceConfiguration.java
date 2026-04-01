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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.binding.xml.UDA10DeviceDescriptorBinderImpl;
import org.jupnp.binding.xml.UDA10ServiceDescriptorBinderImpl;
import org.jupnp.model.ModelUtil;
import org.jupnp.model.Namespace;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.ServiceType;
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.TransportConfigurationProvider;
import org.jupnp.transport.impl.DatagramIOConfigurationImpl;
import org.jupnp.transport.impl.DatagramIOImpl;
import org.jupnp.transport.impl.DatagramProcessorImpl;
import org.jupnp.transport.impl.GENAEventProcessorImpl;
import org.jupnp.transport.impl.MulticastReceiverConfigurationImpl;
import org.jupnp.transport.impl.MulticastReceiverImpl;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.SOAPActionProcessor;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default configuration data of a typical UPnP stack.
 * <p>
 * This configuration utilizes the default network transport implementation found in
 * {@link org.jupnp.transport.impl}.
 * </p>
 * <p>
 * This configuration utilizes the DOM default descriptor binders found in
 * {@link org.jupnp.binding.xml}.
 * </p>
 * <p>
 * The thread <code>Executor</code> is an <code>Executors.newCachedThreadPool()</code> with
 * a custom {@link JUPnPThreadFactory} (it only sets a thread name).
 * </p>
 * <p>
 * Note that this pool is effectively unlimited, so the number of threads will
 * grow (and shrink) as needed - or restricted by your JVM.
 * </p>
 * <p>
 * The default {@link org.jupnp.model.Namespace} is configured without any
 * base path or prefix.
 * </p>
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - introduced bounded thread pool
 * @author Jochen Hiller - increased thread pool size to 200
 * @author Victor Toni - consolidated transport abstraction into one interface
 */
public class DefaultUpnpServiceConfiguration implements UpnpServiceConfiguration {

    private final Logger logger = LoggerFactory.getLogger(DefaultUpnpServiceConfiguration.class);

    // set a fairly large core threadpool size, expecting that core timeout policy will
    // allow the pool to reduce in size after inactivity. note that ThreadPoolExecutor
    // only adds threads beyond its core size once the backlog is full, so a low value
    // core size is a poor choice when there are lots of long-running + idle jobs.
    // a brief intro to the issue:
    // http://www.bigsoft.co.uk/blog/2009/11/27/rules-of-a-threadpoolexecutor-pool-size
    private static final int CORE_THREAD_POOL_SIZE = 16;
    private static final int THREAD_POOL_SIZE = 200;
    private static final int THREAD_QUEUE_SIZE = 1000;
    private static final boolean THREAD_POOL_CORE_TIMEOUT = true;

    private final int streamListenPort;
    private final int multicastResponsePort;

    private final ExecutorService defaultExecutorService;

    private final DatagramProcessor datagramProcessor;
    private final SOAPActionProcessor soapActionProcessor;
    private final GENAEventProcessor genaEventProcessor;

    private final DeviceDescriptorBinder deviceDescriptorBinderUDA10;
    private final ServiceDescriptorBinder serviceDescriptorBinderUDA10;

    private final Namespace namespace;
    private StreamClientConfiguration configuration;

    @SuppressWarnings("rawtypes")
    private final TransportConfiguration transportConfiguration;

    /**
     * Defaults to port '0', ephemeral.
     */
    public DefaultUpnpServiceConfiguration() {
        this(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT);
    }

    public DefaultUpnpServiceConfiguration(int streamListenPort) {
        this(streamListenPort, NetworkAddressFactoryImpl.DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT, true);
    }

    public DefaultUpnpServiceConfiguration(int streamListenPort, int multicastResponsePort) {
        this(streamListenPort, multicastResponsePort, true);
    }

    protected DefaultUpnpServiceConfiguration(boolean checkRuntime) {
        this(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT,
                NetworkAddressFactoryImpl.DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT, checkRuntime);
    }

    protected DefaultUpnpServiceConfiguration(int streamListenPort, int multicastResponsePort, boolean checkRuntime) {
        if (checkRuntime && ModelUtil.ANDROID_RUNTIME) {
            throw new Error("Unsupported runtime environment, use org.jupnp.android.AndroidUpnpServiceConfiguration");
        }

        this.streamListenPort = streamListenPort;
        this.multicastResponsePort = multicastResponsePort;

        defaultExecutorService = createDefaultExecutorService();

        datagramProcessor = createDatagramProcessor();
        soapActionProcessor = createSOAPActionProcessor();
        genaEventProcessor = createGENAEventProcessor();

        deviceDescriptorBinderUDA10 = createDeviceDescriptorBinderUDA10();
        serviceDescriptorBinderUDA10 = createServiceDescriptorBinderUDA10();

        namespace = createNamespace();

        configuration = new StreamClientConfigurationImpl(defaultExecutorService);
        transportConfiguration = TransportConfigurationProvider.getDefaultTransportConfiguration();
    }

    @Override
    public DatagramProcessor getDatagramProcessor() {
        return datagramProcessor;
    }

    @Override
    public SOAPActionProcessor getSoapActionProcessor() {
        return soapActionProcessor;
    }

    @Override
    public GENAEventProcessor getGenaEventProcessor() {
        return genaEventProcessor;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public StreamClient createStreamClient() {
        return transportConfiguration.createStreamClient(getSyncProtocolExecutorService(), configuration);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
        return transportConfiguration.createStreamServer(networkAddressFactory.getStreamListenPort());
    }

    @Override
    public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
        return new MulticastReceiverImpl(new MulticastReceiverConfigurationImpl(
                networkAddressFactory.getMulticastGroup(), networkAddressFactory.getMulticastPort()));
    }

    @Override
    public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
        return new DatagramIOImpl(new DatagramIOConfigurationImpl());
    }

    @Override
    public ExecutorService getMulticastReceiverExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public ExecutorService getDatagramIOExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public ExecutorService getStreamServerExecutorService() {
        return getDefaultExecutorService();
    }

    @Override
    public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
        return deviceDescriptorBinderUDA10;
    }

    @Override
    public ServiceDescriptorBinder getServiceDescriptorBinderUDA10() {
        return serviceDescriptorBinderUDA10;
    }

    @Override
    public ServiceType[] getExclusiveServiceTypes() {
        return new ServiceType[0];
    }

    /**
     * @return Defaults to <code>false</code>.
     */
    @Override
    public boolean isReceivedSubscriptionTimeoutIgnored() {
        return false;
    }

    @Override
    public UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity identity) {
        return null;
    }

    @Override
    public UpnpHeaders getEventSubscriptionHeaders(RemoteService service) {
        return null;
    }

    /**
     * @return Defaults to 1000 milliseconds.
     */
    @Override
    public int getRegistryMaintenanceIntervalMillis() {
        return 1000;
    }

    /**
     * @return Defaults to zero, disabling ALIVE flooding.
     */
    @Override
    public int getAliveIntervalMillis() {
        return 0;
    }

    @Override
    public Integer getRemoteDeviceMaxAgeSeconds() {
        return null;
    }

    @Override
    public ExecutorService getAsyncProtocolExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public ExecutorService getSyncProtocolExecutorService() {
        return getDefaultExecutorService();
    }

    @Override
    public Namespace getNamespace() {
        return namespace;
    }

    @Override
    public Executor getRegistryMaintainerExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public Executor getRegistryListenerExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public Executor getRemoteListenerExecutor() {
        return getDefaultExecutorService();
    }

    @Override
    public NetworkAddressFactory createNetworkAddressFactory() {
        return createNetworkAddressFactory(streamListenPort, multicastResponsePort);
    }

    @Override
    public void shutdown() {
        logger.trace("Shutting down default executor service");
        getDefaultExecutorService().shutdownNow();
    }

    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort, int multicastResponsePort) {
        return new NetworkAddressFactoryImpl(streamListenPort, multicastResponsePort);
    }

    protected DatagramProcessor createDatagramProcessor() {
        return new DatagramProcessorImpl();
    }

    protected SOAPActionProcessor createSOAPActionProcessor() {
        return new SOAPActionProcessorImpl();
    }

    protected GENAEventProcessor createGENAEventProcessor() {
        return new GENAEventProcessorImpl();
    }

    protected DeviceDescriptorBinder createDeviceDescriptorBinderUDA10() {
        return new UDA10DeviceDescriptorBinderImpl();
    }

    protected ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
        return new UDA10ServiceDescriptorBinderImpl();
    }

    protected Namespace createNamespace() {
        return new Namespace();
    }

    protected ExecutorService getDefaultExecutorService() {
        return defaultExecutorService;
    }

    protected ExecutorService createDefaultExecutorService() {
        return new JUPnPExecutor();
    }

    public static class JUPnPExecutor extends ThreadPoolExecutor {

        public JUPnPExecutor() {
            this(new JUPnPThreadFactory(), new ThreadPoolExecutor.DiscardPolicy() {
                // The pool is bounded and rejections will happen during shutdown
                @Override
                public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
                    // Log and discard
                    LoggerFactory.getLogger(DefaultUpnpServiceConfiguration.class)
                            .warn("Thread pool rejected execution of {}", runnable.getClass());
                    super.rejectedExecution(runnable, threadPoolExecutor);
                }
            });
        }

        public JUPnPExecutor(ThreadFactory threadFactory, RejectedExecutionHandler rejectedHandler) {
            // This is the same as Executors.newCachedThreadPool
            super(CORE_THREAD_POOL_SIZE, THREAD_POOL_SIZE, 10L, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(THREAD_QUEUE_SIZE), threadFactory, rejectedHandler);
            allowCoreThreadTimeOut(THREAD_POOL_CORE_TIMEOUT);
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            super.afterExecute(runnable, throwable);
            if (throwable != null) {
                Throwable cause = Exceptions.unwrap(throwable);
                if (cause instanceof InterruptedException) {
                    // Ignore this, might happen when we shutdownNow() the executor. We can't
                    // log at this point as the logging system might be stopped already (e.g.
                    // if it's a CDI component).
                    return;
                }
                // Log only
                LoggerFactory.getLogger(DefaultUpnpServiceConfiguration.class).warn("Thread terminated {} abruptly",
                        runnable, throwable);
            }
        }
    }

    // Executors.DefaultThreadFactory is package visibility (...no touching, you unworthy JDK user!)
    public static class JUPnPThreadFactory implements ThreadFactory {

        protected final ThreadGroup group;
        protected final AtomicInteger threadNumber = new AtomicInteger(1);
        protected final String namePrefix = "jupnp-";

        public JUPnPThreadFactory() {
            group = Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }
    }
}
