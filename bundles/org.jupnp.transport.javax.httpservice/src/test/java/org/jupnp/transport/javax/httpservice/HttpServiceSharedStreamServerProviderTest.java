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
package org.jupnp.transport.javax.httpservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.Servlet;

import org.junit.jupiter.api.Test;
import org.jupnp.transport.javax.servlet.ServletContainerAdapter;
import org.jupnp.transport.javax.servlet.ServletStreamServerImpl;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;

/**
 * Tests for {@link HttpServiceSharedStreamServerProvider}.
 *
 * @author Holger Friedrich - Initial contribution
 */
class HttpServiceSharedStreamServerProviderTest {

    private static final String CALLBACK_PATH = "/upnpcallback";

    @Test
    void streamServersOfOneProviderShareTheServletContainerAdapter() {
        HttpServiceSharedStreamServerProvider provider = provider(httpService(new CopyOnWriteArrayList<>(), false));

        assertSame(adapter(provider), adapter(provider));
    }

    /**
     * A replaced HttpService (e.g. after the http runtime was restarted) results in a new provider component
     * instance. Its callback servlet has to be registered on the new HttpService, not on the old, stopped one.
     */
    @Test
    void replacedHttpServiceGetsItsOwnServletContainerAdapter() {
        List<String> firstEvents = new CopyOnWriteArrayList<>();
        HttpServiceSharedStreamServerProvider first = provider(httpService(firstEvents, false));
        adapter(first).registerServlet(CALLBACK_PATH, servlet());
        first.deactivate();

        List<String> secondEvents = new CopyOnWriteArrayList<>();
        HttpServiceSharedStreamServerProvider second = provider(httpService(secondEvents, false));
        adapter(second).registerServlet(CALLBACK_PATH, servlet());

        assertEquals(List.of("register " + CALLBACK_PATH, "unregister " + CALLBACK_PATH), firstEvents);
        assertEquals(List.of("register " + CALLBACK_PATH), secondEvents);
    }

    @Test
    void deactivateUnregistersServlet() {
        List<String> events = new CopyOnWriteArrayList<>();
        HttpServiceSharedStreamServerProvider provider = provider(httpService(events, false));
        adapter(provider).registerServlet(CALLBACK_PATH, servlet());

        provider.deactivate();

        assertEquals(List.of("register " + CALLBACK_PATH, "unregister " + CALLBACK_PATH), events);
    }

    /**
     * An already stopping http runtime might refuse the unregistration. Deactivation must not fail because of
     * that, and the servlet must not be unregistered a second time.
     */
    @Test
    void failingUnregisterDoesNotFailDeactivationAndIsNotRetried() {
        List<String> events = new CopyOnWriteArrayList<>();
        HttpServiceSharedStreamServerProvider provider = provider(httpService(events, true));
        ServletContainerAdapter adapter = adapter(provider);
        adapter.registerServlet(CALLBACK_PATH, servlet());

        provider.deactivate();
        adapter.stopIfRunning();

        assertEquals(List.of("register " + CALLBACK_PATH, "unregister " + CALLBACK_PATH), events);
    }

    private static HttpServiceSharedStreamServerProvider provider(HttpService httpService) {
        // like DS: the static, mandatory reference is bound before activation
        HttpServiceSharedStreamServerProvider provider = new HttpServiceSharedStreamServerProvider();
        provider.setHttpService(httpService);
        provider.activate(proxy(BundleContext.class, (proxy, method, args) -> null));
        return provider;
    }

    private static ServletContainerAdapter adapter(HttpServiceSharedStreamServerProvider provider) {
        return ((ServletStreamServerImpl) provider.createStreamServer(-1)).getConfiguration()
                .getServletContainerAdapter();
    }

    private static HttpService httpService(List<String> events, boolean failUnregister) {
        return proxy(HttpService.class, (proxy, method, args) -> {
            if ("registerServlet".equals(method.getName())) {
                events.add("register " + args[0]);
            } else if ("unregister".equals(method.getName())) {
                events.add("unregister " + args[0]);
                if (failUnregister) {
                    throw new IllegalStateException("http service is stopping");
                }
            }
            return null;
        });
    }

    private static Servlet servlet() {
        return proxy(Servlet.class, (proxy, method, args) -> null);
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(HttpServiceSharedStreamServerProviderTest.class.getClassLoader(),
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
