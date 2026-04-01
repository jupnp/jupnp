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
package org.jupnp.resources;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.binding.xml.UDA10ServiceDescriptorBinderImpl;
import org.jupnp.binding.xml.UDA10ServiceDescriptorBinderSAXImpl;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleServiceOne;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.util.io.IO;

class UDA10ServiceDescriptorParsingTest {

    @Test
    void readUDA10DescriptorDOM() throws Exception {
        ServiceDescriptorBinder binder = new UDA10ServiceDescriptorBinderImpl();
        RemoteService service = SampleData.createUndescribedRemoteService();
        service = binder.describe(service,
                IO.readLines(getClass().getResourceAsStream("/descriptors/service/uda10.xml")));
        SampleServiceOne.assertMatch(service, SampleData.getFirstService(SampleData.createRemoteDevice()));
    }

    @Test
    void readUDA10DescriptorSAX() throws Exception {
        ServiceDescriptorBinder binder = new UDA10ServiceDescriptorBinderSAXImpl();
        RemoteService service = SampleData.createUndescribedRemoteService();
        service = binder.describe(service,
                IO.readLines(getClass().getResourceAsStream("/descriptors/service/uda10.xml")));
        SampleServiceOne.assertMatch(service, SampleData.getFirstService(SampleData.createRemoteDevice()));
    }

    @Test
    void writeUDA10Descriptor() throws Exception {
        ServiceDescriptorBinder binder = new UDA10ServiceDescriptorBinderImpl();
        RemoteDevice rd = SampleData.createRemoteDevice();
        String descriptorXml = binder.generate(SampleData.getFirstService(rd));

        RemoteService service = SampleData.createUndescribedRemoteService();
        service = binder.describe(service, descriptorXml);
        SampleServiceOne.assertMatch(service, SampleData.getFirstService(rd));
    }
}
