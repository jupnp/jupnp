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
package org.jupnp.util;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a managed service to enable or disable the {@link SpecificationViolationReporter}.
 * 
 * @author Andre Fuechsel
 */
@Component(configurationPid = "org.jupnp.util", property = "specificationViolationReporterEnabled:Boolean=true")
@Designate(ocd = SpecificationViolationReporterConfig.Config.class)
public class SpecificationViolationReporterConfig {

    @ObjectClassDefinition(id = "org.jupnp.util", name = "jUPnP specification violation reporting configuration", description = "Configuration for jUPnP specification violation reporting")
    public @interface Config {
        @AttributeDefinition(name = "specificationViolationReporterEnabled", description = "Enable specification violation reporting.")
        boolean specificationViolationReporterEnabled() default true;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Activate
    public void activate(final Config config) {
        reconfigure(config);
    }

    @Modified
    public void modified(final Config config) {
        reconfigure(config);
    }

    private void reconfigure(Config config) {
        if (config.specificationViolationReporterEnabled()) {
            logger.info("Enabling jUPnP specification violation reporter");
            SpecificationViolationReporter.enableReporting();
        } else {
            logger.info("Disabling jUPnP specification violation reporter");
            SpecificationViolationReporter.disableReporting();
        }
    }
}
