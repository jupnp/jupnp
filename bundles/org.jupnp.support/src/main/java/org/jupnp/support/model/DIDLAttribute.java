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
package org.jupnp.support.model;

/**
 *
 * @author Christian Bauer
 * @author Mario Franco
 */
public class DIDLAttribute {

    private String namespaceURI;
    private String prefix;
    private String value;

    public DIDLAttribute(String namespaceURI, String prefix, String value) {
        this.namespaceURI = namespaceURI;
        this.prefix = prefix;
        this.value = value;
    }

    /**
     * @return the namespaceURI
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * @return the prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
}
