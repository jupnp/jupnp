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
package org.jupnp.osgi.tests.jetty12;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Hashtable;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jupnp.OSGiUpnpServiceConfiguration;
import org.jupnp.UpnpService;
import org.jupnp.model.NetworkAddress;
import org.jupnp.transport.RouterException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Activates the actual shipped {@link OSGiUpnpServiceConfiguration} Declarative Services component -- not a
 * synthetic stand-in like {@link TransportDiscoveryComponent} -- with no <code>javax.servlet</code>,
 * <code>javax.servlet.http</code>, or classic <code>org.osgi.service.http.HttpService</code> implementation
 * present on this run's class path (see itest.bndrun's <code>-runbundles</code>): core has no compile-time
 * dependency on any of them at all, and the optional {@code org.jupnp.transport.javax.httpservice} bridge bundle
 * that would otherwise provide a {@link org.jupnp.transport.spi.SharedStreamServerProvider} for them isn't
 * installed in this run.
 * <p>
 * {@link OSGiUpnpServiceConfiguration} only activates once Configuration Admin has a configuration object for
 * pid <code>org.jupnp</code> (its {@code configurationPolicy = REQUIRE}) and
 * {@link org.jupnp.OSGiUpnpServiceConfigurationEnabler} has enabled it (it starts out
 * {@code enabled = false}); this test supplies that configuration itself, since none of the itest's other
 * bundles do so on their own. Once active, the core bundle's own {@code UpnpServiceImpl} Declarative Services
 * component (a mandatory reference to any {@code UpnpServiceConfiguration}) binds to it automatically and
 * starts the actual UPnP stack, which proves:
 * </p>
 * <ul>
 * <li>SCR can load and activate {@link OSGiUpnpServiceConfiguration} with no shared-server provider bound;</li>
 * <li>the Jetty 12 {@code TransportConfiguration} is injected into it;</li>
 * <li>the standalone Jetty 12 stream server actually starts and binds a real port (with no
 * {@link org.jupnp.transport.spi.SharedStreamServerProvider} bound, {@code createStreamServer()} falls
 * through to the transport's own native server);</li>
 * <li>the resulting {@link UpnpService} is registered and usable.</li>
 * </ul>
 */
public class RealConfigurationActivationTest {

    private static final BundleContext BUNDLE_CONTEXT = FrameworkUtil.getBundle(RealConfigurationActivationTest.class)
            .getBundleContext();

    private static Configuration jupnpConfiguration;

    @BeforeAll
    static void enableOSGiUpnpServiceConfiguration() throws Exception {
        ServiceReference<ConfigurationAdmin> reference = BUNDLE_CONTEXT.getServiceReference(ConfigurationAdmin.class);
        assertNotNull(reference, "ConfigurationAdmin service not available -- check itest.bndrun -runbundles");
        ConfigurationAdmin configurationAdmin = BUNDLE_CONTEXT.getService(reference);

        // An empty configuration for pid "org.jupnp" is enough: it satisfies both OSGiUpnpServiceConfiguration's
        // and OSGiUpnpServiceConfigurationEnabler's configurationPolicy = REQUIRE, and leaves "autoEnable"
        // unset, which the enabler treats as true.
        jupnpConfiguration = configurationAdmin.getConfiguration("org.jupnp", null);
        jupnpConfiguration.update(new Hashtable<>());
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (jupnpConfiguration != null) {
            jupnpConfiguration.delete();
        }
    }

    @Test
    void activatesRealOSGiUpnpServiceConfigurationWithoutServletPackages() throws Exception {
        UpnpService upnpService = waitForService(UpnpService.class);

        assertInstanceOf(OSGiUpnpServiceConfiguration.class, upnpService.getConfiguration(),
                "UpnpServiceImpl should have bound to the real OSGiUpnpServiceConfiguration, not a stand-in");

        waitForAssert(() -> {
            try {
                assertTrue(upnpService.getRouter().isEnabled(), "Router should be enabled");
            } catch (RouterException e) {
                throw new AssertionError(e);
            }
        });

        List<NetworkAddress> activeStreamServers = upnpService.getRouter().getActiveStreamServers(null);
        assertFalse(activeStreamServers.isEmpty(), "Jetty 12's standalone StreamServer should be bound to "
                + "at least one address -- there is no HttpService here to share a port with");
        for (NetworkAddress streamServer : activeStreamServers) {
            assertTrue(streamServer.getPort() > 0,
                    "Jetty 12 StreamServer on " + streamServer.getAddress() + " should have a real assigned port");
        }
    }

    private static <T> T waitForService(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[1];
        waitForAssert(() -> {
            ServiceReference<T> reference = BUNDLE_CONTEXT.getServiceReference(clazz);
            assertNotNull(reference, clazz.getSimpleName() + " service not registered yet");
            result[0] = BUNDLE_CONTEXT.getService(reference);
        });
        return result[0];
    }

    private static void waitForAssert(Runnable assertion) {
        int sleepTime = 200;
        int timeout = 10000;

        long waitingTime = 0;
        while (waitingTime < timeout) {
            try {
                assertion.run();
                return;
            } catch (AssertionError error) {
                waitingTime += sleepTime;
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Unexpected interruption while waiting", e);
                }
            }
        }
        assertion.run();
    }
}
