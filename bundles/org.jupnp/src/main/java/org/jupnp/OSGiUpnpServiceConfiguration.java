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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.jupnp.transport.impl.DatagramIOConfigurationImpl;
import org.jupnp.transport.impl.DatagramIOImpl;
import org.jupnp.transport.impl.DatagramProcessorImpl;
import org.jupnp.transport.impl.GENAEventProcessorImpl;
import org.jupnp.transport.impl.MulticastReceiverConfigurationImpl;
import org.jupnp.transport.impl.MulticastReceiverImpl;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;
import org.jupnp.transport.impl.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.DatagramIO;
import org.jupnp.transport.spi.DatagramProcessor;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.MulticastReceiver;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.SOAPActionProcessor;
import org.jupnp.transport.spi.SharedStreamServerProvider;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration data of a typical UPnP stack on OSGi.
 * <p>
 * The network transport implementation is provided by a separate bundle (e.g.
 * <code>org.jupnp.transport.jetty9</code> or <code>org.jupnp.transport.jetty12</code>) and injected via
 * {@link #addTransportConfiguration(TransportConfiguration)}; this class no longer bundles one itself.
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

    @SuppressWarnings("rawtypes")
    protected final List<TransportConfiguration> transportConfigurations = new CopyOnWriteArrayList<>();

    protected final List<SharedStreamServerProvider> sharedStreamServerProviders = new CopyOnWriteArrayList<>();

    protected Integer timeoutSeconds = 10;
    protected Integer retryIterations = 5;
    protected Integer retryAfterSeconds = (int) TimeUnit.MINUTES.toSeconds(10);

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
    }

    /**
     * @return the {@link TransportConfiguration} injected via
     *         {@link #addTransportConfiguration(TransportConfiguration)}
     * @throws InitializationException if more than one transport bundle's {@link TransportConfiguration} service
     *             is currently bound, or (should not happen while this component is active, given the AT_LEAST_ONE
     *             cardinality below, but a bind/deactivation race is not ruled out) if none is
     */
    @SuppressWarnings("rawtypes")
    protected TransportConfiguration getTransportConfiguration() {
        // Snapshot once: transportConfigurations is a CopyOnWriteArrayList, so size()/isEmpty()/get(0) as
        // separate calls could each observe a different underlying array if a provider is bound/unbound
        // concurrently in between, risking a stale null or IndexOutOfBoundsException. The copy constructor
        // iterates a single fixed snapshot instead.
        List<TransportConfiguration> current = new ArrayList<>(transportConfigurations);
        if (current.size() > 1) {
            throw new InitializationException("Multiple transport implementations found: " + current + ". "
                    + "Make sure only one jUPnP transport bundle (e.g. org.jupnp.transport.jetty9 or "
                    + "org.jupnp.transport.jetty12) is installed.");
        }
        if (current.isEmpty()) {
            // Callers (createStreamClient(), the createStreamServer() fallback) dereference the result
            // directly with no null check, matching every other consumer of this method -- fail loudly here
            // instead of letting them NPE.
            throw new InitializationException(
                    "No transport implementation bound. This should not happen while this component is "
                            + "active, since the reference requires at least one; check for a concurrent unbind.");
        }
        return current.get(0);
    }

    /**
     * A mandatory reference to the transport bundle's {@link TransportConfiguration} service (e.g. provided by
     * <code>org.jupnp.transport.jetty9</code> or <code>org.jupnp.transport.jetty12</code>). Unlike the
     * ServiceLoader-based discovery used by {@link DefaultUpnpServiceConfiguration} (which can race a
     * transport bundle that hasn't finished starting yet), Declarative Services defers activation of this
     * component until a matching service is actually registered, so the outcome does not depend on bundle
     * start order.
     * <p>
     * Cardinality is deliberately {@code AT_LEAST_ONE} rather than the default {@code MANDATORY} (1..1): with a
     * plain 1..1 reference, DS would silently bind whichever candidate ranks highest if two transport bundles
     * (e.g. jetty9 and jetty12) were both installed, picking one non-deterministically with no error. Collecting
     * all bound services here lets {@link #getTransportConfiguration()} fail loudly on ambiguity instead, the
     * same way {@link org.jupnp.transport.TransportConfigurationProvider} does for the ServiceLoader path.
     * <p>
     * The target filter excludes services registered by an OSGi ServiceLoader Mediator (e.g. Apache Aries SPI
     * Fly), which tags them with the {@code serviceloader.mediator} service property (see
     * {@code org.apache.aries.spifly.SpiFlyConstants.SERVICELOADER_MEDIATOR_PROPERTY}; there is no
     * "osgi." prefix, despite the rest of the ServiceLoader Mediator capability namespace using one). Transport
     * bundles also declare a ServiceLoader capability so a mediator can bridge them to non-DS consumers such as
     * {@link org.jupnp.transport.TransportConfigurationProvider} (see e.g. org.jupnp.transport.jetty12's
     * bnd.bnd); without this filter, a mediator present in the same runtime would register a second, separate
     * service instance for the very same transport bundle, and this reference would see it as a spurious
     * ambiguity between "two" transport implementations that are actually just one, registered twice.
     * <p>
     * The greedy policy option matters here specifically because of the ambiguity check: with the default
     * reluctant option, DS would keep this component bound to whichever single transport bundle registered
     * first and never even look at a second one that registers later, silently defeating the "fail loudly if
     * more than one is installed" guarantee {@link #getTransportConfiguration()} is supposed to provide.
     */
    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policyOption = ReferencePolicyOption.GREEDY,
            target = "(!(serviceloader.mediator=*))")
    @SuppressWarnings("rawtypes")
    public void addTransportConfiguration(TransportConfiguration transportConfiguration) {
        transportConfigurations.add(transportConfiguration);
    }

    public void removeTransportConfiguration(TransportConfiguration transportConfiguration) {
        transportConfigurations.remove(transportConfiguration);
    }

    @Activate
    protected void activate(Map<String, Object> configProps) {
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

    /**
     * A {@link SharedStreamServerProvider} is entirely optional: on runtimes that don't have one registered
     * (e.g. because no optional bundle such as <code>org.jupnp.transport.httpservice</code> is installed, or it
     * is installed but its own dependency -- the classic OSGi HttpService -- isn't available, as on pax-web 10
     * and newer, which dropped it in favor of the Jakarta Servlet Whiteboard), UPnP requests are served by the
     * standalone stream server of the discovered transport instead of a shared HTTP server. The greedy policy
     * option reactivates this component when a provider appears after activation, so the outcome does not depend
     * on bundle start order.
     * <p>
     * Core has no compile-time dependency on javax.servlet or the OSGi HttpService API: {@link
     * SharedStreamServerProvider} is a generic seam, so whatever a provider bundle needs to talk to its shared
     * server (HttpService, some other whiteboard, etc.) stays entirely inside that bundle.
     * <p>
     * Cardinality and the target filter mirror {@link #addTransportConfiguration(TransportConfiguration)}: at
     * most one provider is expected, and {@link #createStreamServer(NetworkAddressFactory)} fails loudly rather
     * than picking one if, unexpectedly, more than one is bound.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policyOption = ReferencePolicyOption.GREEDY, target = "(!(serviceloader.mediator=*))")
    public void addSharedStreamServerProvider(SharedStreamServerProvider provider) {
        sharedStreamServerProviders.add(provider);
    }

    public void removeSharedStreamServerProvider(SharedStreamServerProvider provider) {
        sharedStreamServerProviders.remove(provider);
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
        return getTransportConfiguration().createStreamClient(getSyncProtocolExecutorService(),
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
        // Snapshot once, same reasoning as getTransportConfiguration().
        List<SharedStreamServerProvider> providers = new ArrayList<>(sharedStreamServerProviders);
        if (providers.size() > 1) {
            throw new InitializationException("Multiple shared stream server providers found: " + providers + ". "
                    + "Make sure at most one shared-server bundle (e.g. org.jupnp.transport.httpservice) is "
                    + "installed.");
        }
        if (!providers.isEmpty()) {
            logger.debug("createStreamServer using a shared stream server provider");
            return providers.get(0)
                    .createStreamServer(httpProxyPort != -1 ? httpProxyPort : callbackURI.getBasePath().getPort());
        }

        logger.debug("createStreamServer without a shared stream server provider");
        return getTransportConfiguration().createStreamServer(networkAddressFactory.getStreamListenPort());
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
