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
package org.jupnp.xml;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * This should have been part of the JDK.
 * <p>
 * The dumb XPath API needs a map to lookup namespace URIs using prefix keys. Unfortunately,
 * the authors did not know <tt>java.util.Map</tt>.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class NamespaceContextMap extends HashMap<String, String> implements NamespaceContext {

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("No prefix provided!");
        } else if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return getDefaultNamespaceURI();
        } else if (get(prefix) != null) {
            return get(prefix);
        } else {
            return XMLConstants.NULL_NS_URI;
        }
    }

    // Whatever, we don't care
    @Override
    public String getPrefix(String namespaceURI) {
        return null;
    }

    // Whatever, we don't care
    @Override
    public Iterator getPrefixes(String s) {
        return null;
    }

    protected abstract String getDefaultNamespaceURI();
}
