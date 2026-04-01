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

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.RecoveringUDA10DeviceDescriptorBinderImpl;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.binding.xml.UDA10ServiceDescriptorBinderSAXImpl;
import org.jupnp.model.Namespace;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.transport.impl.GENAEventProcessorImpl;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;
import org.jupnp.transport.impl.ServletStreamServerConfigurationImpl;
import org.jupnp.transport.impl.ServletStreamServerImpl;
import org.jupnp.transport.impl.jetty.JettyServletContainer;
import org.jupnp.transport.impl.jetty.JettyStreamClientImpl;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.SOAPActionProcessor;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

import android.os.Build;

/**
 * Configuration settings for deployment on Android.
 * <p>
 * This configuration utilizes the Jetty transport implementation found in <code>org.jupnp.transport.impl.jetty</code>
 * for TCP/HTTP networking, as client and server. The servlet context path for UPnP is set to <code>/upnp</code>.
 * </p>
 * <p>
 * This configuration utilizes {@link UDA10ServiceDescriptorBinderSAXImpl}, the system property
 * <code>org.xml.sax.driver</code> is set to <code>org.xmlpull.v1.sax2.Driver</code>.
 * </p>
 * <p>
 * To preserve battery, the {@link org.jupnp.registry.Registry} will only be maintained every 3 seconds.
 * </p>
 *
 * @author Christian Bauer
 */
public class AndroidUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

    public AndroidUpnpServiceConfiguration() {
        this(0, 0); // Ephemeral port
    }

    public AndroidUpnpServiceConfiguration(int streamListenPort, int multicastResponsePort) {
        super(streamListenPort, multicastResponsePort, false);

        // This should be the default on Android 2.1 but it's not set by default
        System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver");
    }

    @Override
    protected NetworkAddressFactory createNetworkAddressFactory(int streamListenPort, int multicastResponsePort) {
        return new AndroidNetworkAddressFactory(streamListenPort, multicastResponsePort);
    }

    @Override
    protected Namespace createNamespace() {
        // For the Jetty server, this is the servlet context path
        return new Namespace("/upnp");
    }

    @Override
    public StreamClient createStreamClient() {
        // Use Jetty
        return new JettyStreamClientImpl(new StreamClientConfigurationImpl(getSyncProtocolExecutorService()) {
            @Override
            public String getUserAgentValue(int majorVersion, int minorVersion) {
                // TODO: UPNP VIOLATION: Synology NAS requires User-Agent to contain
                // "Android" to return DLNA protocolInfo required to stream to Samsung TV
                // see: http://two-play.com/forums/viewtopic.php?f=6&t=81
                ServerClientTokens tokens = new ServerClientTokens(majorVersion, minorVersion);
                tokens.setOsName("Android");
                tokens.setOsVersion(Build.VERSION.RELEASE);
                return tokens.toString();
            }
        });
    }

    @Override
    public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
        // Use Jetty, start/stop a new shared instance of JettyServletContainer
        return new ServletStreamServerImpl(new ServletStreamServerConfigurationImpl(JettyServletContainer.INSTANCE,
                networkAddressFactory.getStreamListenPort()));
    }

    @Override
    protected DeviceDescriptorBinder createDeviceDescriptorBinderUDA10() {
        return new RecoveringUDA10DeviceDescriptorBinderImpl();
    }

    @Override
    protected ServiceDescriptorBinder createServiceDescriptorBinderUDA10() {
        return new UDA10ServiceDescriptorBinderSAXImpl();
    }

    @Override
    protected SOAPActionProcessor createSOAPActionProcessor() {
        return new SOAPActionProcessorImpl();
    }

    @Override
    protected GENAEventProcessor createGENAEventProcessor() {
        return new GENAEventProcessorImpl();
    }

    @Override
    public int getRegistryMaintenanceIntervalMillis() {
        return 3000; // Preserve battery on Android, only run every 3 seconds
    }
}
