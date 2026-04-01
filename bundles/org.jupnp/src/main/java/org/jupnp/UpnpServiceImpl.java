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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.controlpoint.ControlPointImpl;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.protocol.ProtocolFactoryImpl;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryImpl;
import org.jupnp.transport.Router;
import org.jupnp.transport.RouterException;
import org.jupnp.transport.RouterImpl;
import org.jupnp.util.Exceptions;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link UpnpService}, starts immediately on construction.
 * <p>
 * If no {@link UpnpServiceConfiguration} is provided it will automatically instantiate
 * {@link DefaultUpnpServiceConfiguration}. This configuration <strong>does not work</strong> on Android! Use the
 * AndroidUpnpService application component instead
 * </p>
 * <p>
 * Override the various <tt>create...()</tt> methods to customize instantiation of protocol factory, router, etc.
 * </p>
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - OSGiified the service
 * @author Laurent Garnier - moved OSGi dependency to HttpService into OSGiUpnpServiceConfiguration
 */
@Component(configurationPid = "org.jupnp.upnpservice")
@Designate(ocd = UpnpServiceImpl.Config.class)
public class UpnpServiceImpl implements UpnpService {

    @ObjectClassDefinition(id = "org.jupnp.upnpservice", name = "jUPnP service configuration", description = "Configuration for jUPnP OSGi service")
    public @interface Config {
        @AttributeDefinition(name = "initialSearchEnabled", description = "Enable initial search when starting jUPnP service.")
        boolean initialSearchEnabled() default true;
    }

    private final Logger logger = LoggerFactory.getLogger(UpnpServiceImpl.class);

    protected boolean isConfigured = false;
    protected boolean isRunning = false;
    private volatile boolean isInitialSearchEnabled = true;

    private final Object lock = new Object();

    protected UpnpServiceConfiguration configuration;
    protected ProtocolFactory protocolFactory;
    protected Registry registry;

    protected ControlPoint controlPoint;
    protected Router router;

    protected ScheduledExecutorService scheduledExecutorService;

    protected volatile ScheduledFuture<?> scheduledFuture;

    public UpnpServiceImpl() {
    }

    public UpnpServiceImpl(UpnpServiceConfiguration configuration) {
        this.configuration = configuration;
    }

    @Activate
    public void activate(Config config) {
        scheduledFuture = null;
        scheduledExecutorService = createExecutor();
        isInitialSearchEnabled = config.initialSearchEnabled();
        startup();
    }

    @Deactivate
    public void deactivate() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        scheduledExecutorService.shutdownNow();
        shutdown();
    }

    private static ScheduledExecutorService createExecutor() {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Upnp Service Delayed Startup Thread");
            thread.setUncaughtExceptionHandler((thread1, exception) -> {
                throw new IllegalStateException(exception);
            });
            return thread;
        });
    }

    @Reference
    public void setUpnpServiceConfiguration(UpnpServiceConfiguration configuration) {
        this.configuration = configuration;
        if (isRunning) {
            logger.debug("New OSGi UpnpServiceConfiguration is bound while UPnP service is running; restart needed");
            restart(true);
        }
    }

    public void unsetUpnpServiceConfiguration(UpnpServiceConfiguration configuration) {
        this.configuration = null;
    }

    protected ProtocolFactory createProtocolFactory() {
        return new ProtocolFactoryImpl(this);
    }

    protected Registry createRegistry(ProtocolFactory protocolFactory) {
        return new RegistryImpl(this);
    }

    protected Router createRouter(ProtocolFactory protocolFactory, Registry registry) {
        return new RouterImpl(getConfiguration(), protocolFactory);
    }

    protected ControlPoint createControlPoint(ProtocolFactory protocolFactory, Registry registry) {
        return new ControlPointImpl(getConfiguration(), protocolFactory, registry);
    }

    @Override
    public UpnpServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public ControlPoint getControlPoint() {
        return controlPoint;
    }

    @Override
    public ProtocolFactory getProtocolFactory() {
        return protocolFactory;
    }

    @Override
    public Registry getRegistry() {
        return registry;
    }

    @Override
    public Router getRouter() {
        return router;
    }

    @Override
    public synchronized void shutdown() {
        shutdown(false);
    }

    protected void shutdown(boolean separateThread) {
        Runnable shutdown = () -> {
            synchronized (lock) {
                if (isRunning) {
                    logger.info("Shutting down UPnP service...");
                    shutdownRegistry();
                    shutdownConfiguration();
                    shutdownRouter();
                    logger.info("UPnP service shutdown completed");
                    isRunning = false;
                }
            }
        };
        if (separateThread) {
            // This is not a daemon thread, it has to complete!
            new Thread(shutdown).start();
        } else {
            shutdown.run();
        }
    }

    private void restart(boolean separateThread) {
        Runnable restart = () -> {
            shutdown();
            startup();
        };
        if (separateThread) {
            // This is not a daemon thread, it has to complete!
            new Thread(restart).start();
        } else {
            restart.run();
        }
    }

    protected void shutdownRegistry() {
        getRegistry().shutdown();
    }

    protected void shutdownRouter() {
        try {
            getRouter().shutdown();
        } catch (RouterException e) {
            Throwable cause = Exceptions.unwrap(e);
            if (cause instanceof InterruptedException) {
                logger.debug("Router shutdown was interrupted", e);
            } else {
                throw new RuntimeException("Router error on shutdown", e);
            }
        }
    }

    protected void shutdownConfiguration() {
        getConfiguration().shutdown();
    }

    private void delayedStartup(int msDelay) {

        Runnable startup = this::startup;

        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        scheduledFuture = scheduledExecutorService.schedule(startup, msDelay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void startup() {
        synchronized (lock) {
            if (!isRunning) {
                logger.info("Starting UPnP service...");

                // Instantiation order is important: Router needs to start its network services after registry is ready

                logger.debug("Using configuration: {}", getConfiguration().getClass().getName());

                this.protocolFactory = createProtocolFactory();
                this.registry = createRegistry(protocolFactory);
                this.router = createRouter(protocolFactory, registry);

                try {
                    this.router.enable();
                } catch (RouterException e) {
                    throw new RuntimeException("Enabling network router failed", e);
                }

                this.controlPoint = createControlPoint(protocolFactory, registry);

                logger.debug("UPnP service started successfully");

                isRunning = true;

                if (isInitialSearchEnabled) {
                    controlPoint.search(new STAllHeader());
                }
            }
        }
    }
}
