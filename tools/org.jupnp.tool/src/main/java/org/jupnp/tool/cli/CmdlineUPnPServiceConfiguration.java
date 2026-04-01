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
package org.jupnp.tool.cli;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.impl.jetty.JettyTransportConfiguration;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class configures the behavior of jUPnP used in junpnptool.
 *
 * It will allow to observe the processing by getting statistical information
 * about processing. It will also log rejected execution in a smarter way to not
 * flood logging with too much log messages.
 *
 * Note: It allows to configure the behavior by static attributes, which can NOT
 * be changed after constructing the services.
 */
public class CmdlineUPnPServiceConfiguration extends DefaultUpnpServiceConfiguration {

    static final Logger LOGGER = LoggerFactory.getLogger(CmdlineUPnPServiceConfiguration.class);

    static int MAIN_POOL_SIZE = 20;
    static int ASYNC_POOL_SIZE = 20;

    static int MULTICAST_RESPONSE_LISTEN_PORT = NetworkAddressFactoryImpl.DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT;

    // class methods to configure behavior

    public static void setDebugStatistics(boolean onOrOff) {
        MonitoredQueueingThreadPoolExecutor.DEBUG_STATISTICS = onOrOff;
    }

    public static void setPoolConfiguration(int mainPoolSize, int asyncPoolSize) {
        MAIN_POOL_SIZE = mainPoolSize;
        ASYNC_POOL_SIZE = asyncPoolSize;
    }

    public static void setMulticastResponsePort(Integer port) {
        MULTICAST_RESPONSE_LISTEN_PORT = port.intValue();
    }

    private ExecutorService mainExecutorService;

    private ExecutorService asyncExecutorService;

    private final StreamClientConfiguration configuration;
    private final TransportConfiguration transportConfiguration;

    // instance methods

    public CmdlineUPnPServiceConfiguration() {
        super();
        createExecutorServices();

        configuration = new StreamClientConfigurationImpl(mainExecutorService);
        transportConfiguration = new JettyTransportConfiguration();
    }

    /**
     * We create a network address factory based on configured multicast port.
     */
    @Override
    public NetworkAddressFactory createNetworkAddressFactory() {
        return super.createNetworkAddressFactory(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT,
                MULTICAST_RESPONSE_LISTEN_PORT);
    }

    private void createExecutorServices() {
        mainExecutorService = new MonitoredQueueingThreadPoolExecutor("jupnptool-main", MAIN_POOL_SIZE);
        asyncExecutorService = new MonitoredQueueingThreadPoolExecutor("jupnptool-async", ASYNC_POOL_SIZE);
    }

    protected ExecutorService getMainExecutorService() {
        return mainExecutorService;
    }

    @Override
    public Executor getRegistryMaintainerExecutor() {
        return getMainExecutorService();
    }

    @Override
    public Executor getRegistryListenerExecutor() {
        return getMainExecutorService();
    }

    @Override
    public ExecutorService getMulticastReceiverExecutor() {
        return getMainExecutorService();
    }

    @Override
    public ExecutorService getDatagramIOExecutor() {
        return getMainExecutorService();
    }

    @Override
    public ExecutorService getStreamServerExecutorService() {
        return getMainExecutorService();
    }

    @Override
    public ExecutorService getAsyncProtocolExecutor() {
        return asyncExecutorService;
    }

    @Override
    public void shutdown() {
        LOGGER.debug("Shutting down executor services");
        shutdownExecutorServices();

        // create the executor again ready for reuse in case the runtime is started up again.
        createExecutorServices();
    }

    protected void shutdownExecutorServices() {
        if (mainExecutorService != null) {
            mainExecutorService.shutdownNow();
        }
        if (asyncExecutorService != null) {
            asyncExecutorService.shutdownNow();
        }
    }

    @Override
    public StreamClient createStreamClient() {
        return transportConfiguration.createStreamClient(getSyncProtocolExecutorService(), configuration);
    }

    @Override
    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
        return transportConfiguration.createStreamServer(networkAddressFactory.getStreamListenPort());
    }

    // inner classes

    /**
     * This class implements a rejection handler which logs rejects smart. Logs
     * once happens one error message, and further messages report only number
     * of rejections.
     *
     * This class locks against multiple usage to have clea log messages. This
     * may have influence during runtime, as logging will be done during holder
     * a lock.
     */
    public static class SmartLoggingDiscardPolicy extends ThreadPoolExecutor.DiscardPolicy {

        private static final Lock rejectLock = new ReentrantLock();

        private static boolean displayedErrorOnce = false;
        private static String lastRejectedClass = null;
        private static final int MAX_NO_OF_REJECTS_TO_LOG = 20;
        private static int noOfRejects = 0;

        // The pool is bounded and rejections will happen during shutdown
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            if (threadPoolExecutor.isTerminating()) {
                // do log rejects during shutdown in debug level only
                LOGGER.debug("Thread pool rejected during termination execution of {}", runnable);
            } else {
                try {
                    rejectLock.lock();
                    if (!displayedErrorOnce) {
                        LOGGER.error("Thread pool rejected executions, consider to resize pool sizing");
                        displayedErrorOnce = true;
                    }
                    // check for changed runnable class names
                    if (lastRejectedClass == null || !lastRejectedClass.equals(runnable.getClass().getName())) {
                        LOGGER.warn("Thread pool rejected execution of {}", runnable);
                        noOfRejects = 0;
                        lastRejectedClass = runnable.getClass().getName();
                    } else {
                        // same runnable class name, increment number of calls
                        noOfRejects = noOfRejects + 1;
                        if (noOfRejects >= MAX_NO_OF_REJECTS_TO_LOG) {
                            LOGGER.warn("Thread pool rejected execution of ({} times) {}", noOfRejects, runnable);
                            noOfRejects = 0;
                        }
                    }
                } finally {
                    rejectLock.unlock();
                }
            }
            super.rejectedExecution(runnable, threadPoolExecutor);
        }
    }
}
