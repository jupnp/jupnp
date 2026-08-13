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
package org.jupnp.transport.jakarta.whiteboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.servlet.runtime.HttpServiceRuntime;
import org.osgi.service.servlet.runtime.HttpServiceRuntimeConstants;

/**
 * No mocking framework is declared for this module (see the {@code org.jupnp.bom.test} BOM), so
 * {@link BundleContext} and {@link ServiceReference} are faked with plain {@link Proxy} instances instead --
 * both interfaces are large, but {@link JakartaWhiteboardServletContainerAdapter#addConnector(String, int)}
 * only ever calls {@code BundleContext.getServiceReference(HttpServiceRuntime.class)} and
 * {@code ServiceReference.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT)} on its way to
 * discovering a port, so only those two calls need a real answer.
 *
 * @author Holger Friedrich - initial contribution
 */
class JakartaWhiteboardServletContainerAdapterTest {

    @Test
    void addConnectorWaitsForEndpointToBecomeAvailable() throws Exception {
        long testStart = System.currentTimeMillis();
        long availableAfterMillis = 150;
        int expectedPort = 45123;

        // Simulates HttpServiceRuntime registering before Pax Web has actually finished binding a listener and
        // updated its HTTP_SERVICE_ENDPOINT property -- the exact race PORT_DISCOVERY_TIMEOUT_MILLIS/
        // PORT_DISCOVERY_POLL_MILLIS exist to ride out.
        BundleContext context = fakeBundleContext(() -> System.currentTimeMillis() - testStart >= availableAfterMillis
                ? new String[] { "http://0.0.0.0:" + expectedPort }
                : null);
        JakartaWhiteboardServletContainerAdapter adapter = new JakartaWhiteboardServletContainerAdapter(context);

        int port = adapter.addConnector("0.0.0.0", -1, 5000, 20);

        assertEquals(expectedPort, port);
        assertTrue(System.currentTimeMillis() - testStart >= availableAfterMillis,
                "addConnector() returned before the endpoint actually became available -- it should have polled, "
                        + "not raced ahead of HttpServiceRuntime");
    }

    @Test
    void addConnectorThrowsIOExceptionWhenNoEndpointBecomesAvailableWithinTimeout() {
        BundleContext context = fakeBundleContext(() -> null);
        JakartaWhiteboardServletContainerAdapter adapter = new JakartaWhiteboardServletContainerAdapter(context);

        IOException exception = assertThrows(IOException.class, () -> adapter.addConnector("0.0.0.0", -1, 200, 20));
        assertTrue(exception.getMessage().contains("HttpServiceRuntime"),
                "Exception message should explain what couldn't be discovered: " + exception.getMessage());
    }

    @Test
    void addConnectorReturnsExplicitPortWithoutDiscovery() throws Exception {
        // A non-negative port means the caller (ServletStreamServerImpl) already knows what to bind to --
        // discovery must not run at all in that case, let alone be able to time out.
        BundleContext context = fakeBundleContext(() -> {
            throw new AssertionError("Discovery should not run when an explicit port was given");
        });
        JakartaWhiteboardServletContainerAdapter adapter = new JakartaWhiteboardServletContainerAdapter(context);

        assertEquals(8080, adapter.addConnector("0.0.0.0", 8080, 5000, 20));
    }

    private static BundleContext fakeBundleContext(Supplier<String[]> endpoints) {
        ServiceReference<HttpServiceRuntime> reference = fakeServiceReference(endpoints);
        return proxy(BundleContext.class, (proxyInstance, method, args) -> {
            if ("getServiceReference".equals(method.getName()) && args != null && args.length == 1
                    && args[0] == HttpServiceRuntime.class) {
                return reference;
            }
            return null;
        });
    }

    private static ServiceReference<HttpServiceRuntime> fakeServiceReference(Supplier<String[]> endpoints) {
        return proxy(ServiceReference.class, (proxyInstance, method, args) -> {
            if ("getProperty".equals(method.getName()) && args != null && args.length == 1
                    && HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT.equals(args[0])) {
                return endpoints.get();
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler delegate) {
        InvocationHandler handler = (proxyInstance, method, args) -> {
            switch (method.getName()) {
                case "equals":
                    return proxyInstance == args[0];
                case "hashCode":
                    return System.identityHashCode(proxyInstance);
                case "toString":
                    return "Fake" + type.getSimpleName();
                default:
                    return delegate.invoke(proxyInstance, method, args);
            }
        };
        return (T) Proxy.newProxyInstance(JakartaWhiteboardServletContainerAdapterTest.class.getClassLoader(),
                new Class<?>[] { type }, handler);
    }
}
