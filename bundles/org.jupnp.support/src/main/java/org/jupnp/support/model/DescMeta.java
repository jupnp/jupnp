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

import java.net.URI;

import org.jupnp.transport.impl.PooledXmlProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Descriptor metadata about an item/resource.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;any namespace='##other'/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="nameSpace" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
public class DescMeta<M> {

    /**
     * Pooled XML processor for efficient DocumentBuilder reuse.
     */
    private static final DocumentBuilderPool XML_PROCESSOR = new DocumentBuilderPool();

    /**
     * Provides pooled DocumentBuilder instances.
     */
    private static class DocumentBuilderPool extends PooledXmlProcessor {
        public Document createNewDocument() {
            try {
                return newDocument();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create document", e);
            }
        }
    }

    protected String id;
    protected String type;
    protected URI nameSpace;
    protected M metadata;

    public DescMeta() {
    }

    public DescMeta(String id, String type, URI nameSpace, M metadata) {
        this.id = id;
        this.type = type;
        this.nameSpace = nameSpace;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public URI getNameSpace() {
        return nameSpace;
    }

    public void setNameSpace(URI nameSpace) {
        this.nameSpace = nameSpace;
    }

    public M getMetadata() {
        return metadata;
    }

    public void setMetadata(M metadata) {
        this.metadata = metadata;
    }

    /**
     * Creates a new metadata document with a desc-wrapper root element.
     *
     * @return A new DOM Document with the desc-wrapper root element
     */
    public Document createMetadataDocument() {
        Document d = XML_PROCESSOR.createNewDocument();
        Element rootElement = d.createElementNS(DIDLContent.DESC_WRAPPER_NAMESPACE_URI, "desc-wrapper");
        d.appendChild(rootElement);
        return d;
    }
}
