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
package org.jupnp.transport.spi;

import org.jupnp.transport.TransportConfiguration;

/**
 * Optional OSGi service: implement this to let {@link org.jupnp.OSGiUpnpServiceConfiguration} serve UPnP
 * requests through an existing shared HTTP server (e.g. the classic OSGi HttpService, via
 * {@code org.jupnp.transport.javax.httpservice}) instead of a transport's own standalone {@link StreamServer}.
 * <p>
 * Core has no built-in implementation of this interface and no compile-time dependency on any particular
 * shared-server technology (javax.servlet, HttpService, or otherwise); it is provided entirely by optional
 * bundles. When no {@link SharedStreamServerProvider} is registered, {@code createStreamServer()} falls
 * through to the discovered {@link TransportConfiguration}'s own native {@link StreamServer} instead.
 * </p>
 *
 * @author Holger Friedrich - extracted from OSGiUpnpServiceConfiguration's former hard-coded HttpService path
 */
public interface SharedStreamServerProvider {

    /**
     * @param proxyPort the port UPnP callback/descriptor URLs should advertise, since the actual requests are
     *            served through the shared server's own (usually different) port
     * @return a {@link StreamServer} backed by the shared server
     */
    StreamServer<?> createStreamServer(int proxyPort);
}
