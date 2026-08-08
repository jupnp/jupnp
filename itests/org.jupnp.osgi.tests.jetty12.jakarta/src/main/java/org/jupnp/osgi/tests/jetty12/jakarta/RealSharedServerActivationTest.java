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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
        // JakartaHttpServiceServletContainerAdapter published a Servlet service nobody consumed: send a real
        // HTTP request through the whole live pipeline (HTTP listener -> Pax Web context selection -> "/*"
        // servlet mapping -> AsyncServlet -> jUPnP's Router/ProtocolFactory) and check for a response only
        // jUPnP's own code can produce. Querying HttpServiceRuntime's DTOs instead would not be enough: a real
        // openHAB/Pax Web deployment has reported a servlet as successfully registered while the endpoint was
        // nevertheless unreachable over actual HTTP -- see JakartaHttpServiceServletContainerAdapter's class
        // Javadoc for that failure mode, which is exactly what this test needs to catch.
        waitForAssert(() -> assertUpnpCallbackServletReachableOverHttp(activeStreamServers.get(0), upnpService));
    }

    private void assertUpnpCallbackServletReachableOverHttp(NetworkAddress streamServer, UpnpService upnpService) {
        String basePath = upnpService.getConfiguration().getNamespace().getBasePath().getPath();
        String host = streamServer.getAddress().getHostAddress();
        if (host.contains(":")) {
            host = "[" + host + "]";
        }
        URI probeUri = URI.create("http://" + host + ":" + streamServer.getPort() + basePath + "/reachability-probe");

        HttpResponse<Void> response;
        try {
            // NOTIFY to a path that doesn't end in Namespace.CALLBACK_FILE ("/cb") can never match a GENA
            // event subscription in ProtocolFactoryImpl.createReceivingSync() -- it falls through to that
            // method's final `throw new ProtocolCreationException(...)`, which UpnpStream.process() turns
            // into HTTP 501. That 501 is a signal only jUPnP's own router produces: Pax Web/Jetty return a
            // plain 404 for any path nothing is mapped to, never 501, so getting 501 back here -- rather than
            // a connection failure or an ordinary 404 -- is what actually proves the request reached
            // AsyncServlet and jUPnP's Router, not just that Pax Web's Whiteboard modeled the registration.
            HttpRequest request = HttpRequest.newBuilder(probeUri)
                    .method("NOTIFY",
                            HttpRequest.BodyPublishers
                                    .ofString("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\"/>"))
                    .header("Content-Type", "text/xml").header("NT", "upnp:event").header("NTS", "upnp:propchange")
                    .header("SID", "uuid:jupnp-itest-reachability-probe").header("SEQ", "0").build();
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new AssertionError("Could not reach " + probeUri + " -- check that "
                    + "JakartaHttpServiceServletContainerAdapter actually registered a reachable context/servlet", e);
        }
        assertEquals(501, response.statusCode(),
                "Expected jUPnP's own \"protocol not found\" response (501 Not Implemented) for " + probeUri
                        + ", proving the request reached AsyncServlet and jUPnP's Router/ProtocolFactory -- got "
                        + response.statusCode()
                        + " instead, which points at Pax Web never routing the request to jUPnP's servlet at all "
                        + "(check JakartaHttpServiceServletContainerAdapter.registerServlet() and the "
                        + "osgi.http.whiteboard.context.path property it sets on its dedicated context)");
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
