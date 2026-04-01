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
package org.jupnp.support.avtransport.lastchange;

import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.jupnp.model.ModelUtil;
import org.jupnp.support.lastchange.EventedValue;
import org.jupnp.support.lastchange.LastChangeParser;

/**
 * @author Christian Bauer
 */
public class AVTransportLastChangeParser extends LastChangeParser {

    public static final String NAMESPACE_URI = "urn:schemas-upnp-org:metadata-1-0/AVT/";
    public static final String SCHEMA_RESOURCE = "org/jupnp/support/avtransport/metadata-1.01-avt.xsd";

    @Override
    protected String getNamespace() {
        return NAMESPACE_URI;
    }

    @Override
    protected Source[] getSchemaSources() {
        // TODO: Android 2.2 has a broken SchemaFactory, we can't validate
        // http://code.google.com/p/android/issues/detail?id=9491&q=schemafactory&colspec=ID%20Type%20Status%20Owner%20Summary%20Stars
        if (!ModelUtil.ANDROID_RUNTIME) {
            return new Source[] { new StreamSource(
                    Thread.currentThread().getContextClassLoader().getResourceAsStream(SCHEMA_RESOURCE)) };
        }
        return null;
    }

    @Override
    protected Set<Class<? extends EventedValue<?>>> getEventedVariables() {
        return AVTransportVariable.ALL;
    }
}
