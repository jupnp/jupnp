/*
 * Copyright (C) 2011-2024 4th Line GmbH, Switzerland and others
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
import org.w3c.dom.Document;

/**
 * Reads and generates service descriptor XML metadata.
 *
 * @author Christian Bauer
 */
public interface ServiceDescriptorBinder {

    <T extends Service> T describe(T undescribedService, String descriptorXml)
            throws DescriptorBindingException, ValidationException;

    <T extends Service> T describe(T undescribedService, Document dom)
            throws DescriptorBindingException, ValidationException;

    String generate(Service service) throws DescriptorBindingException;

    Document buildDOM(Service service) throws DescriptorBindingException;
}
