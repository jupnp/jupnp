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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.servlet.runtime.HttpServiceRuntime;
import org.osgi.service.servlet.runtime.HttpServiceRuntimeConstants;

import jakarta.servlet.Servlet;

/**
 * No mocking framework is declared for this module (see the {@code org.jupnp.bom.test} BOM), so
 * {@link BundleContext} and {@link ServiceReference} are faked with plain {@link Proxy} instances instead --
 * both interfaces are large, but {@link JakartaWhiteboardServletContainerAdapter#addConnector(String, int)}
 * only ever calls {@code BundleContext.getServiceReferences(HttpServiceRuntime.class, filter)} and
 * {@code ServiceReference.getProperty(HttpServiceRuntimeConstants.HTTP_SERVICE_ENDPOINT)} on its way to
 * discovering a port, so only those two calls need a real answer.
 *
 * @author Holger Friedrich - initial contribution
 */
class JakartaWhiteboardServletContainerAdapterTest {

    @Test
    void addConnectorReadsPortFromRuntimeEndpoint() throws Exception {
        BundleContext context = fakeBundleContext(
                () -> new String[] { "/relative-only", "https://0.0.0.0:8443/", "http://0.0.0.0:45123/" });
        JakartaWhiteboardServletContainerAdapter adapter = new JakartaWhiteboardServletContainerAdapter(context);

        assertEquals(45123, adapter.addConnector("0.0.0.0", -1));
    }

    @Test
    void addConnectorThrowsIOExceptionImmediatelyWhenNoEndpointIsAvailable() {
        // Must not wait for an endpoint to appear: addConnector() runs on the thread activating jUPnP, which
        // during a bundle refresh may be the one that still has to restart Pax Web.
        BundleContext context = fakeBundleContext(() -> null);
        JakartaWhiteboardServletContainerAdapter adapter = new JakartaWhiteboardServletContainerAdapter(context);

        long start = System.currentTimeMillis();
        IOException exception = assertThrows(IOException.class, () -> adapter.addConnector("0.0.0.0", -1));
        assertTrue(System.currentTimeMillis() - start < 1000, "addConnector() should not wait for an endpoint");
        assertTrue(exception.getMessage().contains("HttpServiceRuntime"),
                "Exception message should explain what couldn't be discovered: " + exception.getMessage());
    }

    @Test
    void registerServletDoesNotWaitForWhiteboardToPickItUp() throws Exception {
        // No HttpServiceRuntime at all, so the Whiteboard never picks the registration up -- registerServlet()
        // must still return right away, leaving the confirmation to a background thread.
        List<String> unregistered = new CopyOnWriteArrayList<>();
        BundleContext context = proxy(BundleContext.class, (proxyInstance, method, args) -> {
            if ("registerService".equals(method.getName())) {
                String name = args[1] instanceof Servlet ? "servlet" : "context";
                return proxy(ServiceRegistration.class, (p, m, a) -> {
                    if ("unregister".equals(m.getName())) {
                        unregistered.add(name);
                    }
                    return null;
                });
            }
            return null;
        });
        JakartaWhiteboardServletContainerAdapter adapter = new JakartaWhiteboardServletContainerAdapter(context);

        long start = System.currentTimeMillis();
        adapter.registerServlet("/upnpcallback", proxy(Servlet.class, (p, m, a) -> null));
        assertTrue(System.currentTimeMillis() - start < 1000, "registerServlet() should not wait for the Whiteboard");

        Thread check = adapter.getRegistrationCheck();
        assertNotNull(check, "Expected a background thread confirming the Whiteboard registration");

        adapter.stopIfRunning();
        check.join(5000);
        assertFalse(check.isAlive(), "stopIfRunning() should cancel the background registration check");
        assertEquals(List.of("servlet", "context"), unregistered);
    }

    @Test
    void addConnectorReturnsExplicitPortWithoutDiscovery() throws Exception {
        // A non-negative port means the caller (ServletStreamServerImpl) already knows what to bind to --
        // discovery must not run at all in that case, let alone be able to time out.
        BundleContext context = fakeBundleContext(() -> {
            throw new AssertionError("Discovery should not run when an explicit port was given");
        });
        JakartaWhiteboardServletContainerAdapter adapter = new JakartaWhiteboardServletContainerAdapter(context);

        assertEquals(8080, adapter.addConnector("0.0.0.0", 8080));
    }

    private static BundleContext fakeBundleContext(Supplier<String[]> endpoints) {
        ServiceReference<HttpServiceRuntime> reference = fakeServiceReference(endpoints);
        return proxy(BundleContext.class, (proxyInstance, method, args) -> {
            // Only answers the lookup with the provider's own activation filter, so addConnector() can't read a
            // different runtime than the one that let the provider activate
            if ("getServiceReferences".equals(method.getName()) && args != null && args.length == 2
                    && args[0] == HttpServiceRuntime.class
                    && JakartaWhiteboardSharedStreamServerProvider.HTTP_ENDPOINT_FILTER.equals(args[1])) {
                return List.of(reference);
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
