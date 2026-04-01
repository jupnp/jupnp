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
package org.jupnp;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables the {@link OSGiUpnpServiceConfiguration} when the <code>autoEnable</code> configuration parameter is
 * <code>true</code> or not configured.
 *
 * @author Wouter Born - Initial contribution
 */
@Component(configurationPid = "org.jupnp", configurationPolicy = ConfigurationPolicy.REQUIRE)
public class OSGiUpnpServiceConfigurationEnabler {

    private final Logger logger = LoggerFactory.getLogger(OSGiUpnpServiceConfigurationEnabler.class);

    private static final String AUTO_ENABLE = "autoEnable";

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> config) {
        Object value = config.get(AUTO_ENABLE);
        boolean autoEnable = value == null || Boolean.parseBoolean(value.toString());
        if (autoEnable) {
            context.enableComponent(OSGiUpnpServiceConfiguration.class.getName());
            logger.info("{} enabled by {}", OSGiUpnpServiceConfiguration.class.getSimpleName(),
                    OSGiUpnpServiceConfigurationEnabler.class);
        } else {
            logger.info("{} not enabled by {}", OSGiUpnpServiceConfiguration.class.getSimpleName(),
                    OSGiUpnpServiceConfigurationEnabler.class);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        context.disableComponent(OSGiUpnpServiceConfiguration.class.getName());
        logger.info("{} disabled by {}", OSGiUpnpServiceConfiguration.class.getSimpleName(),
                OSGiUpnpServiceConfigurationEnabler.class);
    }
}
