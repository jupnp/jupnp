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
package org.jupnp.android;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.UpnpServiceImpl;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.registry.Registry;
import org.jupnp.transport.Router;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

/**
 * Provides a UPnP stack with Android configuration as an application service component.
 * <p>
 * Sends a search for all UPnP devices on instantiation. See the
 * {@link org.jupnp.android.AndroidUpnpService} interface for a usage example.
 * </p>
 * <p/>
 * Override the
 * {@link #createRouter(org.jupnp.UpnpServiceConfiguration, org.jupnp.protocol.ProtocolFactory, android.content.Context)}
 * and {@link #createConfiguration()} methods to customize the service.
 *
 * @author Christian Bauer
 */
public class AndroidUpnpServiceImpl extends Service {

    protected UpnpService upnpService;
    protected Binder binder = new Binder();

    /**
     * Starts the UPnP service.
     */
    @Override
    public void onCreate() {
        super.onCreate();

        upnpService = new UpnpServiceImpl(createConfiguration()) {

            @Override
            protected Router createRouter(ProtocolFactory protocolFactory, Registry registry) {
                return AndroidUpnpServiceImpl.this.createRouter(getConfiguration(), protocolFactory,
                        AndroidUpnpServiceImpl.this);
            }

            @Override
            public synchronized void shutdown() {
                // First have to remove the receiver, so Android won't complain about it leaking
                // when the main UI thread exits.
                ((AndroidRouter) getRouter()).unregisterBroadcastReceiver();

                // Now we can concurrently run the Cling shutdown code, without occupying the
                // Android main UI thread. This will complete probably after the main UI thread
                // is done.
                super.shutdown(true);
            }
        };
    }

    protected UpnpServiceConfiguration createConfiguration() {
        return new AndroidUpnpServiceConfiguration();
    }

    protected AndroidRouter createRouter(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory,
            Context context) {
        return new AndroidRouter(configuration, protocolFactory, context);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Stops the UPnP service, when the last Activity unbinds from this Service.
     */
    @Override
    public void onDestroy() {
        upnpService.shutdown();
        super.onDestroy();
    }

    protected class Binder extends android.os.Binder implements AndroidUpnpService {

        @Override
        public UpnpService get() {
            return upnpService;
        }

        @Override
        public UpnpServiceConfiguration getConfiguration() {
            return upnpService.getConfiguration();
        }

        @Override
        public Registry getRegistry() {
            return upnpService.getRegistry();
        }

        @Override
        public ControlPoint getControlPoint() {
            return upnpService.getControlPoint();
        }
    }
}
