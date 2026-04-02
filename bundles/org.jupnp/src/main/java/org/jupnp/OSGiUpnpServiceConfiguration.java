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

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.RecoveringUDA10DeviceDescriptorBinderImpl;
import org.jupnp.binding.xml.RecoveringUDA10ServiceDescriptorBinderSAXImpl;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
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
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.impl.osgi.HttpServiceServletContainerAdapter;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.SOAPActionProcessor;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration data of a typical UPnP stack on OSGi.
 * <p>
 * This configuration utilizes the default network transport implementation found in {@link org.jupnp.transport.impl}.
 * </p>
 * <p>
 * This configuration utilizes the SAX default descriptor binders found in {@link org.jupnp.binding.xml}.
 * </p>
 * <p>
 * The thread <code>Executor</code> is an <code>Executors.newCachedThreadPool()</code> with a custom
 * QueueingThreadFactory.
 * </p>
 * <p>
 * The default {@link org.jupnp.model.Namespace} is configured without any base path or prefix.
 * </p>
 * This component is enabled by the {@link OSGiUpnpServiceConfigurationEnabler} based on the <code>autoEnable</code>
 * configuration value. Set <code>autoEnable</code> to <code>false</code> when using a custom
 * {@link UpnpServiceConfiguration} component.
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - introduced bounded thread pool and http service streaming server
 * @author Victor Toni - consolidated transport abstraction into one interface
 * @author Wouter Born - conditionally enable component based on autoEnable configuration value
 * @author Laurent Garnier - added OSGi dependency to HttpService and removed its release
 * @author Laurent Garnier - added parameter "interfaces" to set a list of network interfaces to consider
 */
@Component(configurationPid = "org.jupnp", configurationPolicy = ConfigurationPolicy.REQUIRE, enabled = false)
public class OSGiUpnpServiceConfiguration implements UpnpServiceConfiguration {

    protected static final String OSGI_SERVICE_HTTP_PORT = "org.osgi.service.http.port";

    private final Logger logger = LoggerFactory.getLogger(OSGiUpnpServiceConfiguration.class);

    // configurable properties
    protected int threadPoolSize = 20;
    protected int asyncThreadPoolSize = 20;
    protected int remoteThreadPoolSize = 40;
    protected String interfaces;
    protected int multicastResponsePort;
    protected int httpProxyPort = -1;
    protected int streamListenPort = 8080;
    protected boolean asyncThreadPool = true;
    protected boolean mainThreadPool = true;
    protected boolean remoteThreadPool = true;
    protected Namespace callbackURI = new Namespace("http://localhost/upnpcallback");

    protected ExecutorService mainExecutorService;
    protected ExecutorService asyncExecutorService;
    protected ExecutorService remoteExecutorService;

    protected DatagramProcessor datagramProcessor;
    protected SOAPActionProcessor soapActionProcessor;
    protected GENAEventProcessor genaEventProcessor;

    protected DeviceDescriptorBinder deviceDescriptorBinderUDA10;
    protected ServiceDescriptorBinder serviceDescriptorBinderUDA10;

    protected Namespace namespace;

    protected BundleContext context;

    @SuppressWarnings("rawtypes")
    protected TransportConfiguration transportConfiguration;

    protected Integer timeoutSeconds = 10;
    protected Integer retryIterations = 5;
    protected Integer retryAfterSeconds = (int) TimeUnit.MINUTES.toSeconds(10);

    protected HttpService httpService;

    /**
     * Defaults to port '0', ephemeral.
     */
    public OSGiUpnpServiceConfiguration() {
        this(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT);
    }

    public OSGiUpnpServiceConfiguration(int streamListenPort) {
        this(streamListenPort, NetworkAddressFactoryImpl.DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT, true);
    }

    public OSGiUpnpServiceConfiguration(int streamListenPort, int multicastResponsePort) {
        this(streamListenPort, multicastResponsePort, true);
    }

    protected OSGiUpnpServiceConfiguration(boolean checkRuntime) {
        this(NetworkAddressFactoryImpl.DEFAULT_TCP_HTTP_LISTEN_PORT,
                NetworkAddressFactoryImpl.DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT, checkRuntime);
    }

    protected OSGiUpnpServiceConfiguration(int streamListenPort, int multicastResponsePort, boolean checkRuntime) {
        if (checkRuntime && ModelUtil.ANDROID_RUNTIME) {
            throw new Error("Unsupported runtime environment, use org.jupnp.android.AndroidUpnpServiceConfiguration");
        }

        this.streamListenPort = streamListenPort;
        this.multicastResponsePort = multicastResponsePort;

        this.transportConfiguration = TransportConfigurationProvider.getDefaultTransportConfiguration();
    }

    @Activate
    protected void activate(BundleContext context, Map<String, Object> configProps) {
        this.context = context;

        setConfigValues(configProps);

        createExecutorServices();

        datagramProcessor = createDatagramProcessor();
        soapActionProcessor = createSOAPActionProcessor();
        genaEventProcessor = createGENAEventProcessor();

        deviceDescriptorBinderUDA10 = createDeviceDescriptorBinderUDA10();
        serviceDescriptorBinderUDA10 = createServiceDescriptorBinderUDA10();

        namespace = createNamespace();

        logger.debug("{} activated", this);
    }

    @Deactivate
    protected void deactivate() {
        shutdown();
        logger.debug("{} deactivated", this);
    }

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    public void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    public void unsetHttpService(HttpService httpService) {
        this.httpService = null;
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
        return transportConfiguration.createStreamClient(getSyncProtocolExecutorService(),
                createStreamClientConfiguration());
    }

    private StreamClientConfiguration createStreamClientConfiguration() {
        return new StreamClientConfigurationImpl(asyncExecutorService, timeoutSeconds, 5, retryAfterSeconds,
                retryIterations);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public MulticastReceiver createMulticastReceiver(NetworkAddressFactory networkAddressFactory) {
        return new MulticastReceiverImpl(new MulticastReceiverConfigurationImpl(
                networkAddressFactory.getMulticastGroup(), networkAddressFactory.getMulticastPort()));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public DatagramIO createDatagramIO(NetworkAddressFactory networkAddressFactory) {
        return new DatagramIOImpl(new DatagramIOConfigurationImpl());
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
        if (httpService != null) {
            logger.debug("createStreamServer using OSGi HttpService");
            return new ServletStreamServerImpl(new ServletStreamServerConfigurationImpl(
                    HttpServiceServletContainerAdapter.getInstance(httpService, context),
                    httpProxyPort != -1 ? httpProxyPort : callbackURI.getBasePath().getPort()));
        }

        logger.debug("createStreamServer without OSGi HttpService");
        return transportConfiguration.createStreamServer(networkAddressFactory.getStreamListenPort());
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
        if (asyncThreadPool) {
            return asyncExecutorService;
        } else {
            return Executors.newCachedThreadPool();
        }
    }

    @Override
    public ExecutorService getSyncProtocolExecutorService() {
        return getMainExecutorService();
    }

    @Override
    public Namespace getNamespace() {
        return namespace;
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
    public Executor getRemoteListenerExecutor() {
        return getRemoteExecutorService();
    }

    @Override
    public NetworkAddressFactory createNetworkAddressFactory() {
        return createNetworkAddressFactory(streamListenPort, multicastResponsePort);
    }

    @Override
    public void shutdown() {
        logger.debug("Shutting down executor services");
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
        if (remoteExecutorService != null) {
            remoteExecutorService.shutdownNow();
        }
    }

    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort, int multicastResponsePort) {
        return new NetworkAddressFactoryImpl(streamListenPort, multicastResponsePort, interfaces);
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
        return new RecoveringUDA10DeviceDescriptorBinderImpl();
    }

    protected ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
        return new RecoveringUDA10ServiceDescriptorBinderSAXImpl();
    }

    protected Namespace createNamespace() {
        return callbackURI;
    }

    protected ExecutorService getRemoteExecutorService() {
        if (remoteThreadPool) {
            return remoteExecutorService;
        } else {
            return Executors.newCachedThreadPool();
        }
    }

    protected ExecutorService getMainExecutorService() {
        if (mainThreadPool) {
            return mainExecutorService;
        } else {
            return Executors.newCachedThreadPool();
        }
    }

    protected void createExecutorServices() {
        if (mainThreadPool) {
            logger.debug("Creating mainThreadPool");
            mainExecutorService = createMainExecutorService();
        } else {
            logger.debug("Skipping mainThreadPool creation.");
        }

        if (asyncThreadPool) {
            logger.debug("Creating asyncThreadPool");
            asyncExecutorService = createAsyncProtocolExecutorService();
        } else {
            logger.debug("Skipping asyncThreadPool creation.");
        }

        if (remoteThreadPool) {
            logger.debug("Creating remoteThreadPool");
            remoteExecutorService = createRemoteProtocolExecutorService();
        } else {
            logger.debug("Skipping remoteThreadPool creation.");
        }
    }

    protected ExecutorService createMainExecutorService() {
        return QueueingThreadPoolExecutor.createInstance("upnp-main", threadPoolSize);
    }

    protected ExecutorService createAsyncProtocolExecutorService() {
        return QueueingThreadPoolExecutor.createInstance("upnp-async", asyncThreadPoolSize);
    }

    protected ExecutorService createRemoteProtocolExecutorService() {
        return QueueingThreadPoolExecutor.createInstance("upnp-remote", remoteThreadPoolSize);
    }

    protected void setConfigValues(Map<String, Object> properties) {
        if (properties == null) {
            return;
        }

        Object prop = properties.get("threadPoolSize");
        if (prop instanceof String) {
            try {
                threadPoolSize = Integer.parseInt((String) prop);
                mainThreadPool = threadPoolSize != -1;
            } catch (NumberFormatException e) {
                logger.error("Invalid value '{}' for threadPoolSize - using default value '{}'", prop, threadPoolSize);
            }
        }
        logger.info("OSGiUpnpServiceConfiguration createConfiguration threadPoolSize = {} {}", threadPoolSize,
                mainThreadPool);

        prop = properties.get("asyncThreadPoolSize");
        if (prop instanceof String) {
            try {
                asyncThreadPoolSize = Integer.parseInt((String) prop);
                asyncThreadPool = asyncThreadPoolSize != -1;
            } catch (NumberFormatException e) {
                logger.error("Invalid value '{}' for asyncThreadPoolSize - using default value '{}'", prop,
                        asyncThreadPoolSize);
            }
        }
        logger.info("OSGiUpnpServiceConfiguration createConfiguration asyncThreadPoolSize = {} {}", asyncThreadPoolSize,
                asyncThreadPool);

        prop = properties.get("multicastResponsePort");
        if (prop instanceof String) {
            try {
                multicastResponsePort = Integer.parseInt((String) prop);
            } catch (NumberFormatException e) {
                logger.error("Invalid value '{}' for multicastResponsePort - using default value '{}'", prop,
                        multicastResponsePort);
            }
        } else if (prop instanceof Integer) {
            multicastResponsePort = (Integer) prop;
        }

        prop = properties.get("streamListenPort");
        if (prop instanceof String) {
            try {
                streamListenPort = Integer.parseInt((String) prop);
            } catch (NumberFormatException e) {
                logger.error("Invalid value '{}' for streamListenPort - using default value '{}'", prop,
                        streamListenPort);
            }
        } else if (prop instanceof Integer) {
            streamListenPort = (Integer) prop;
        } else if (System.getProperty(OSGI_SERVICE_HTTP_PORT) != null) {
            try {
                streamListenPort = Integer.parseInt(System.getProperty(OSGI_SERVICE_HTTP_PORT));
            } catch (NumberFormatException e) {
                logger.debug("Invalid value '{}' for osgi.http.port - using default value '{}'", prop,
                        streamListenPort);
            }
        }

        prop = properties.get("interfaces");
        if (prop instanceof String) {
            interfaces = (String) prop;
        }
        logger.info("OSGiUpnpServiceConfiguration interfaces = {}", interfaces);

        prop = properties.get("callbackURI");
        if (prop instanceof String) {
            try {
                callbackURI = new Namespace((String) prop);
            } catch (Exception e) {
                logger.error("Invalid value '{}' for callbackURI - using default value '{}'", prop, callbackURI);
            }
        }

        prop = properties.get("httpProxyPort");
        if (prop instanceof String) {
            try {
                httpProxyPort = Integer.parseInt((String) prop);
            } catch (NumberFormatException e) {
                logger.error("Invalid value '{}' for httpProxyPort - using default value '{}'", prop, httpProxyPort);
            }
        } else if (prop instanceof Integer) {
            httpProxyPort = (Integer) prop;
        }

        prop = properties.get("retryAfterSeconds");
        if (prop instanceof String) {
            try {
                retryAfterSeconds = Integer.valueOf((String) prop);
            } catch (NumberFormatException e) {
                logger.error("Invalid value '{}' for retryAfterSeconds - using default value", prop);
            }
        } else if (prop instanceof Integer) {
            retryAfterSeconds = (Integer) prop;
        }
        logger.info("OSGiUpnpServiceConfiguration retryAfterSeconds = {}", retryAfterSeconds);

        prop = properties.get("retryIterations");
        if (prop instanceof String) {
            try {
                retryIterations = Integer.valueOf((String) prop);
            } catch (NumberFormatException e) {
                logger.error("Invalid value '{}' for retryIterations - using default value", prop);
            }
        } else if (prop instanceof Integer) {
            retryIterations = (Integer) prop;
        }
        logger.info("OSGiUpnpServiceConfiguration retryIterations = {}", retryIterations);

        prop = properties.get("timeoutSeconds");
        if (prop instanceof String) {
            try {
                timeoutSeconds = Integer.valueOf((String) prop);
            } catch (NumberFormatException e) {
                logger.error("Invalid value '{}' for timeoutSeconds - using default value", prop);
            }
        } else if (prop instanceof Integer) {
            timeoutSeconds = (Integer) prop;
        }
        logger.info("OSGiUpnpServiceConfiguration timeoutSeconds = {}", timeoutSeconds);

        // let's automatically determine the size for the remoteThreadPool
        if (!mainThreadPool || !asyncThreadPool) {
            remoteThreadPool = false;
            remoteThreadPoolSize = -1;
        } else {
            remoteThreadPool = true;
            remoteThreadPoolSize = threadPoolSize + asyncThreadPoolSize;
        }
    }
}
