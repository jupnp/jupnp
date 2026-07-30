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
package org.jupnp.osgi.tests.jetty12.jakarta;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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
import org.osgi.service.servlet.runtime.HttpServiceRuntime;
import org.osgi.service.servlet.runtime.dto.RuntimeDTO;
import org.osgi.service.servlet.runtime.dto.ServletContextDTO;
import org.osgi.service.servlet.runtime.dto.ServletDTO;

/**
 * Activates the real {@link OSGiUpnpServiceConfiguration} Declarative Services component with a genuine Pax
 * Web 11.x Jakarta HTTP Whiteboard implementation present (see itest.bndrun's {@code -runbundles}), the
 * opposite scenario from {@code org.jupnp.osgi.tests.jetty12}'s {@code RealConfigurationActivationTest},
 * which deliberately has no shared-server implementation available at all.
 * <p>
 * This proves the full path end to end: {@code org.jupnp.transport.jakarta.httpservice}'s
 * {@code JakartaHttpServiceSharedStreamServerProvider} binds to the real {@link HttpServiceRuntime} Pax Web
 * registers, {@code OSGiUpnpServiceConfiguration.createStreamServer()} picks it over jetty12's standalone
 * path, and the callback servlet it registers via the OSGi HTTP Whiteboard is actually wired up by Pax
 * Web's own runtime -- not just published as an unconsumed OSGi service.
 * </p>
 */
public class RealSharedServerActivationTest {

    private static final BundleContext BUNDLE_CONTEXT = FrameworkUtil.getBundle(RealSharedServerActivationTest.class)
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
    void activatesRealOSGiUpnpServiceConfigurationWithJakartaWhiteboard() throws Exception {
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
        assertFalse(activeStreamServers.isEmpty(), "The shared stream server should be bound to at least one address");
        for (NetworkAddress streamServer : activeStreamServers) {
            assertTrue(streamServer.getPort() > 0,
                    "Stream server on " + streamServer.getAddress() + " should have a real assigned port");
        }

        // The proof this is genuinely going through Pax Web's Whiteboard, not just that
        // JakartaHttpServiceServletContainerAdapter published a Servlet service nobody consumed: query Pax
        // Web's own HttpServiceRuntime DTO and confirm it lists our servlet's registered pattern.
        waitForAssert(this::assertUpnpCallbackServletRegisteredWithWhiteboard);
    }

    private void assertUpnpCallbackServletRegisteredWithWhiteboard() {
        HttpServiceRuntime runtime = waitForService(HttpServiceRuntime.class);
        RuntimeDTO runtimeDTO = runtime.getRuntimeDTO();
        boolean found = false;
        StringBuilder seen = new StringBuilder();
        for (ServletContextDTO contextDTO : runtimeDTO.servletContextDTOs) {
            for (ServletDTO servletDTO : contextDTO.servletDTOs) {
                seen.append(String.join(",", servletDTO.patterns)).append(' ');
                if (Arrays.stream(servletDTO.patterns).anyMatch(pattern -> pattern.startsWith("/upnpcallback"))) {
                    found = true;
                }
            }
        }
        StringBuilder failed = new StringBuilder();
        for (var failedServletDTO : runtimeDTO.failedServletDTOs) {
            failed.append(String.join(",", failedServletDTO.patterns)).append(":reason=")
                    .append(failedServletDTO.failureReason).append(' ');
        }
        assertTrue(found,
                "Pax Web's HttpServiceRuntime should list a servlet registered for /upnpcallback -- check "
                        + "JakartaHttpServiceServletContainerAdapter.registerServlet() and the "
                        + "osgi.http.whiteboard.servlet.pattern property it sets. Registered patterns seen: [" + seen
                        + "]. Failed registrations: [" + failed + "]");
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
        int timeout = 25000;

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
