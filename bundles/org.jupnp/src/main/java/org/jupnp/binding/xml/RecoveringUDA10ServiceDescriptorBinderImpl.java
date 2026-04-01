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
package org.jupnp.binding.xml;

import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.Service;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This binder does not enforce strict UPnP spec conformance - it rather ignores services that are not correctly
 * declared.
 *
 * @author Kai Kreuzer
 * @author Jochen Hiller - use SpecificationViolationReporter, change logger to be final
 */
public class RecoveringUDA10ServiceDescriptorBinderImpl extends UDA10ServiceDescriptorBinderImpl {

    private final Logger logger = LoggerFactory.getLogger(ServiceDescriptorBinder.class);

    @Override
    public <S extends Service> S describe(S undescribedService, String descriptorXml)
            throws DescriptorBindingException, ValidationException {
        try {
            String fixedXml = fixWrongNamespaces(descriptorXml);
            return super.describe(undescribedService, fixedXml);
        } catch (DescriptorBindingException e) {
            logger.warn(e.getMessage());
        }
        return null;
    }

    protected String fixWrongNamespaces(String descriptorXml) {
        if (descriptorXml.contains("<scpd xmlns=\"urn:Belkin:service-1-0\">")) {
            SpecificationViolationReporter
                    .report("Detected invalid scpd namespace 'urn:Belkin', replacing it with 'urn:schemas-upnp-org'");
            return descriptorXml.replaceAll("<scpd xmlns=\"urn:Belkin:service-1-0\">",
                    "<scpd xmlns=\"urn:schemas-upnp-org:service-1-0\">");
        }
        return descriptorXml;
    }
}
