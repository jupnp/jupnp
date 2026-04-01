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
package org.jupnp.mock;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.controlpoint.ControlPointImpl;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.protocol.ProtocolFactoryImpl;
import org.jupnp.protocol.async.SendingNotificationAlive;
import org.jupnp.protocol.async.SendingSearch;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryImpl;
import org.jupnp.registry.RegistryMaintainer;
import org.jupnp.transport.RouterException;
import org.jupnp.transport.spi.NetworkAddressFactory;

/**
 * Simplifies testing of core and non-core modules.
 * <p>
 * It uses the {@link org.jupnp.mock.MockUpnpService.MockProtocolFactory}.
 * </p>
 *
 * @author Christian Bauer
 */
public class MockUpnpService implements UpnpService {

    protected UpnpServiceConfiguration configuration;
    protected ControlPoint controlPoint;
    protected ProtocolFactory protocolFactory;
    protected Registry registry;
    protected MockRouter router;

    protected NetworkAddressFactory networkAddressFactory;
    private boolean sendsAlive;

    /**
     * Single-thread of execution for the whole UPnP stack, no ALIVE messages or registry maintenance.
     */
    public MockUpnpService() {
        this(false, new MockUpnpServiceConfiguration(false, false));
    }

    /**
     * No ALIVE messages.
     */
    public MockUpnpService(MockUpnpServiceConfiguration configuration) {
        this(false, configuration);
    }

    /**
     * Single-thread of execution for the whole UPnP stack, except one background registry maintenance thread.
     */
    public MockUpnpService(final boolean sendsAlive, final boolean maintainsRegistry) {
        this(sendsAlive, new MockUpnpServiceConfiguration(maintainsRegistry, false));
    }

    public MockUpnpService(final boolean sendsAlive, final boolean maintainsRegistry, final boolean multiThreaded) {
        this(sendsAlive, new MockUpnpServiceConfiguration(maintainsRegistry, multiThreaded));
    }

    public MockUpnpService(final boolean sendsAlive, final MockUpnpServiceConfiguration configuration) {

        this.sendsAlive = sendsAlive;
        this.configuration = configuration;
    }

    protected ProtocolFactory createProtocolFactory(UpnpService service, boolean sendsAlive) {
        return new MockProtocolFactory(service, sendsAlive);
    }

    protected MockRouter createRouter() {
        return new MockRouter(getConfiguration(), getProtocolFactory());
    }

    /**
     * This factory customizes several protocols.
     * <p>
     * The {@link org.jupnp.protocol.async.SendingNotificationAlive} protocol
     * only sends messages if this feature is enabled when instantiating the factory.
     * </p>
     * <p>
     * The {@link org.jupnp.protocol.async.SendingSearch} protocol doesn't wait between
     * sending search message bulks, this speeds up testing.
     * </p>
     */
    public static class MockProtocolFactory extends ProtocolFactoryImpl {

        private boolean sendsAlive;

        public MockProtocolFactory(UpnpService upnpService, boolean sendsAlive) {
            super(upnpService);
            this.sendsAlive = sendsAlive;
        }

        @Override
        public SendingNotificationAlive createSendingNotificationAlive(LocalDevice localDevice) {
            return new SendingNotificationAlive(getUpnpService(), localDevice) {
                @Override
                protected void execute() throws RouterException {
                    if (sendsAlive) {
                        super.execute();
                    }
                }
            };
        }

        @Override
        public SendingSearch createSendingSearch(UpnpHeader searchTarget, int mxSeconds) {
            return new SendingSearch(getUpnpService(), searchTarget, mxSeconds) {
                @Override
                public int getBulkIntervalMilliseconds() {
                    return 0; // Don't wait
                }
            };
        }
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
    public MockRouter getRouter() {
        return router;
    }

    @Override
    public void shutdown() {
        getRegistry().shutdown();
        getConfiguration().shutdown();
    }

    @Override
    public void startup() {
        this.protocolFactory = createProtocolFactory(this, sendsAlive);

        this.registry = new RegistryImpl(this) {
            @Override
            protected RegistryMaintainer createRegistryMaintainer() {
                return ((MockUpnpServiceConfiguration) configuration).isMaintainsRegistry()
                        ? super.createRegistryMaintainer()
                        : null;
            }
        };

        this.networkAddressFactory = this.configuration.createNetworkAddressFactory();

        this.router = createRouter();

        this.controlPoint = new ControlPointImpl(configuration, protocolFactory, registry);
    }
}
