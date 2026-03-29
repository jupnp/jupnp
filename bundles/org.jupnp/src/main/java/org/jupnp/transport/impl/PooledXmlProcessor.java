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
package org.jupnp.transport.impl;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Abstract class responsible for creating new {@link Document}s either for writing or already parsed from a given input
 * stream. The class manages a pool of {@link DocumentBuilder}s internally and reuses them in different threads saving
 * CPU time from factory and builder instantiation if possible.
 * <p>
 * On Android this class will not reuse {@link DocumentBuilder}s because the {@link DocumentBuilder#reset()}
 * implementation sets all internal properties to <code>false</code>
 * <p>
 * Default pool size is 20.
 *
 * @author Ivan Iliev - Initial contribution and API
 * @author Wouter Born - Detect if pooling is possible to fix issues on Android
 */
public abstract class PooledXmlProcessor {

    private final DocumentBuilderFactory documentBuilderFactory;

    private final ConcurrentLinkedQueue<DocumentBuilder> builderPool;

    private final transient Logger logger = LoggerFactory.getLogger(PooledXmlProcessor.class);

    private boolean reuseDocumentBuilders;

    protected PooledXmlProcessor() {
        this(20);
    }

    protected PooledXmlProcessor(int basePoolSize) {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        builderPool = new ConcurrentLinkedQueue<>();

        try {
            reuseDocumentBuilders = isDocumentBuilderReusable();
            if (reuseDocumentBuilders) {
                logger.debug("Adding {} instances to the pool because DocumentBuilders can be reused", basePoolSize);
                for (int i = 0; i < basePoolSize; i++) {
                    builderPool.add(documentBuilderFactory.newDocumentBuilder());
                }
            } else {
                logger.debug("Not adding instances to the pool because DocumentBuilders cannot be reused");
            }
        } catch (ParserConfigurationException e) {
            logger.error("Error when invoking newDocumentBuilder()", e);
        }
    }

    /**
     * Determines if {@link DocumentBuilder} instances can be reused after resetting them.
     * On Android it is not possible to reuse builders because {@link DocumentBuilder#reset()} sets all internal
     * properties to <code>false</code>.
     *
     * @return <code>true</code> if {@link DocumentBuilder} instances can be reused after resetting them,
     *         <code>false</code> otherwise
     * @throws ParserConfigurationException when a {@link DocumentBuilder} cannot be created
     */
    private boolean isDocumentBuilderReusable() throws ParserConfigurationException {
        DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
        builder.reset();
        return builder.isNamespaceAware();
    }

    /**
     * @return a new unused instance of {@link Document}.
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    protected Document newDocument()
            throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
        return getDocument(null, null);
    }

    /**
     * 
     * @param inputSource to parse from
     * @param errorHandler custom error handler for the parsing operation
     * @return The parsed {@link Document} instance.
     * 
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    protected Document readDocument(InputSource inputSource, ErrorHandler errorHandler)
            throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
        return getDocument(inputSource, errorHandler);
    }

    /**
     * @param inputSource to parse from
     * @return The parsed {@link Document} instance.
     * 
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    protected Document readDocument(InputSource inputSource)
            throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
        return getDocument(inputSource, null);
    }

    private Document getDocument(InputSource inputSource, ErrorHandler errorHandler)
            throws FactoryConfigurationError, ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = reuseDocumentBuilders ? builderPool.poll() : null;
        if (builder == null) {
            builder = documentBuilderFactory.newDocumentBuilder();
        }

        try {
            if (errorHandler != null) {
                builder.setErrorHandler(errorHandler);
            }

            if (inputSource != null) {
                return builder.parse(inputSource);
            }

            return builder.newDocument();
        } finally {
            returnBuilder(builder);
        }
    }

    private void returnBuilder(DocumentBuilder builder) throws FactoryConfigurationError {
        if (reuseDocumentBuilders) {
            builder.reset();
            builderPool.add(builder);
        }
    }
}
