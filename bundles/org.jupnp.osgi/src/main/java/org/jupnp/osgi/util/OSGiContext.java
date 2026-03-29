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
package org.jupnp.osgi.util;

import org.jupnp.UpnpService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Wouter Born - Initial contribution
 */
@Component
public class OSGiContext {

    private static BundleContext bundleContext;
    private static UpnpService upnpService;

    @Activate
    public OSGiContext(BundleContext bundleContext, @Reference UpnpService upnpService) {
        OSGiContext.bundleContext = bundleContext;
        OSGiContext.upnpService = upnpService;
    }

    @Deactivate
    public void deactivate() {
        OSGiContext.bundleContext = null;
        OSGiContext.upnpService = null;
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public static UpnpService getUpnpService() {
        return upnpService;
    }
}
