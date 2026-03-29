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

import org.jupnp.model.Namespace;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.Device;
import org.jupnp.model.profile.RemoteClientInfo;
import org.w3c.dom.Document;

/**
 * Reads and generates device descriptor XML metadata.
 *
 * @author Christian Bauer
 */
public interface DeviceDescriptorBinder {

    <T extends Device> T describe(T undescribedDevice, String descriptorXml)
            throws DescriptorBindingException, ValidationException;

    <T extends Device> T describe(T undescribedDevice, Document dom)
            throws DescriptorBindingException, ValidationException;

    String generate(Device device, RemoteClientInfo info, Namespace namespace) throws DescriptorBindingException;

    Document buildDOM(Device device, RemoteClientInfo info, Namespace namespace) throws DescriptorBindingException;
}
