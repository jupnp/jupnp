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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;
import org.jupnp.transport.jakarta.servlet.ServletContainerAdapter;
import org.jupnp.transport.jakarta.servlet.ServletStreamServerImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import jakarta.servlet.Servlet;

/**
 * Tests for {@link JakartaWhiteboardSharedStreamServerProvider}.
 *
 * @author Holger Friedrich - Initial contribution
 */
class JakartaWhiteboardSharedStreamServerProviderTest {

    private static final String CALLBACK_PATH = "/upnpcallback";

    @Test
    void streamServersOfOneProviderShareTheServletContainerAdapter() {
        JakartaWhiteboardSharedStreamServerProvider provider = provider(bundleContext(new CopyOnWriteArrayList<>()));
        try {
            assertSame(adapter(provider), adapter(provider));
        } finally {
            provider.deactivate();
        }
    }

    /**
     * A new provider component instance (e.g. after the bundle was restarted, which invalidates the old
     * BundleContext) must register its servlet through its own BundleContext, not the previous one.
     */
    @Test
    void newProviderInstanceRegistersThroughItsOwnBundleContext() {
        List<String> firstEvents = new CopyOnWriteArrayList<>();
        JakartaWhiteboardSharedStreamServerProvider first = provider(bundleContext(firstEvents));
        adapter(first).registerServlet(CALLBACK_PATH, servlet());
        first.deactivate();

        List<String> secondEvents = new CopyOnWriteArrayList<>();
        JakartaWhiteboardSharedStreamServerProvider second = provider(bundleContext(secondEvents));
        try {
            adapter(second).registerServlet(CALLBACK_PATH, servlet());

            assertEquals(List.of("register context", "register servlet"), secondEvents);
            assertEquals(List.of("register context", "register servlet", "unregister servlet", "unregister context"),
                    firstEvents);
        } finally {
            second.deactivate();
        }
    }

    @Test
    void deactivateUnregistersServletAndContext() {
        List<String> events = new CopyOnWriteArrayList<>();
        JakartaWhiteboardSharedStreamServerProvider provider = provider(bundleContext(events));
        adapter(provider).registerServlet(CALLBACK_PATH, servlet());

        provider.deactivate();

        assertEquals(List.of("register context", "register servlet", "unregister servlet", "unregister context"),
                events);
    }

    private static JakartaWhiteboardSharedStreamServerProvider provider(BundleContext context) {
        JakartaWhiteboardSharedStreamServerProvider provider = new JakartaWhiteboardSharedStreamServerProvider();
        provider.activate(context);
        return provider;
    }

    private static ServletContainerAdapter adapter(JakartaWhiteboardSharedStreamServerProvider provider) {
        return ((ServletStreamServerImpl) provider.createStreamServer(-1)).getConfiguration()
                .getServletContainerAdapter();
    }

    /**
     * A BundleContext without an HttpServiceRuntime, recording service (un)registrations.
     */
    private static BundleContext bundleContext(List<String> events) {
        return proxy(BundleContext.class, (proxy, method, args) -> {
            if ("registerService".equals(method.getName())) {
                String name = args[1] instanceof Servlet ? "servlet" : "context";
                events.add("register " + name);
                return proxy(ServiceRegistration.class, (p, m, a) -> {
                    if ("unregister".equals(m.getName())) {
                        events.add("unregister " + name);
                    }
                    return null;
                });
            }
            return null;
        });
    }

    private static Servlet servlet() {
        return proxy(Servlet.class, (proxy, method, args) -> null);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(JakartaWhiteboardSharedStreamServerProviderTest.class.getClassLoader(),
                new Class<?>[] { type }, (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        if ("hashCode".equals(method.getName())) {
                            return System.identityHashCode(proxy);
                        } else if ("equals".equals(method.getName())) {
                            return proxy == args[0];
                        }
                        return type.getSimpleName() + "Proxy";
                    }
                    return handler.invoke(proxy, method, args);
                }));
    }
}
