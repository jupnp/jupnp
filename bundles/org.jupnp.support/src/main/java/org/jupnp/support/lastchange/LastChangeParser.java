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
package org.jupnp.support.lastchange;

import static org.jupnp.model.XMLUtil.appendNewElement;

import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.jupnp.model.XMLUtil;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.shared.AbstractMap;
import org.jupnp.util.io.IO;
import org.jupnp.xml.DOMParser;
import org.jupnp.xml.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Reads and writes the "LastChange" XML content.
 * <p>
 * Validates against a schema if the {@link #getSchemaSources()} method
 * doesn't return <code>null</code>.
 * </p>
 * <p>
 * Note: This is broken on most devices and with most services out in the wild. In fact,
 * you might want to use polling the service with actions, to get its status, instead of
 * GENA. Polling can be expensive on low-power control points, however.
 * </p>
 *
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class LastChangeParser extends SAXParser {

    private final Logger logger = LoggerFactory.getLogger(LastChangeParser.class);

    public enum CONSTANTS {
        Event,
        InstanceID,
        val;

        public boolean equals(String s) {
            return this.name().equals(s);
        }
    }

    protected abstract String getNamespace();

    protected Set<Class<? extends EventedValue<?>>> getEventedVariables() {
        return Collections.emptySet();
    }

    protected EventedValue<?> createValue(String name, Map.Entry<String, String>[] attributes) throws Exception {
        for (Class<? extends EventedValue<?>> evType : getEventedVariables()) {
            if (evType.getSimpleName().equals(name)) {
                Constructor<? extends EventedValue<?>> ctor = evType.getConstructor(Map.Entry[].class);
                return ctor.newInstance(new Object[] { attributes });
            }
        }
        return null;
    }

    /**
     * Uses the current thread's context classloader to read and unmarshall the given resource.
     *
     * @param resource The resource on the classpath.
     * @return The unmarshalled Event model.
     * @throws Exception
     */
    public Event parseResource(String resource) throws Exception {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            return parse(IO.readLines(is));
        }
    }

    public Event parse(String xml) throws Exception {

        if (xml == null || xml.isEmpty()) {
            throw new RuntimeException("Null or empty XML");
        }

        Event event = new Event();
        new RootHandler(event, this);

        if (logger.isDebugEnabled()) {
            logger.debug("Parsing 'LastChange' event XML content");
            logger.debug(
                    "===================================== 'LastChange' BEGIN ============================================");
            logger.debug(xml);
            logger.debug(
                    "====================================== 'LastChange' END  ============================================");
        }
        parse(new InputSource(new StringReader(xml)));

        logger.debug("Parsed event with instances IDs: {}", event.getInstanceIDs().size());
        if (logger.isTraceEnabled()) {
            for (InstanceID instanceID : event.getInstanceIDs()) {
                logger.trace("InstanceID '{}' has values: {}", instanceID.getId(), instanceID.getValues().size());
                for (EventedValue<?> eventedValue : instanceID.getValues()) {
                    logger.trace("{} => {}", eventedValue.getName(), eventedValue.getValue());
                }
            }
        }

        return event;
    }

    class RootHandler extends SAXParser.Handler<Event> {

        RootHandler(Event instance, SAXParser parser) {
            super(instance, parser);
        }

        RootHandler(Event instance) {
            super(instance);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            if (CONSTANTS.InstanceID.equals(localName)) {
                String valAttr = attributes.getValue(CONSTANTS.val.name());
                if (valAttr != null) {
                    InstanceID instanceID = new InstanceID(new UnsignedIntegerFourBytes(valAttr));
                    getInstance().getInstanceIDs().add(instanceID);
                    new InstanceIDHandler(instanceID, this);
                }
            }
        }
    }

    class InstanceIDHandler extends SAXParser.Handler<InstanceID> {

        InstanceIDHandler(InstanceID instance, SAXParser.Handler<?> parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(String uri, String localName, String qName, final Attributes attributes)
                throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            @SuppressWarnings("unchecked")
            Map.Entry<String, String>[] attributeMap = new Map.Entry[attributes.getLength()];
            for (int i = 0; i < attributeMap.length; i++) {
                attributeMap[i] = new AbstractMap.SimpleEntry<>(attributes.getLocalName(i), attributes.getValue(i));
            }
            try {
                EventedValue<?> esv = createValue(localName, attributeMap);
                if (esv != null) {
                    getInstance().getValues().add(esv);
                }
            } catch (Exception e) {
                // Don't exit, just log a warning
                logger.warn("Error reading event XML, ignoring value", e);
            }
        }

        @Override
        protected boolean isLastElement(String uri, String localName, String qName) {
            return CONSTANTS.InstanceID.equals(localName);
        }
    }

    public String generate(Event event) throws Exception {
        return XMLUtil.documentToFragmentString(buildDOM(event));
    }

    protected Document buildDOM(Event event) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        Document d = factory.newDocumentBuilder().newDocument();
        generateRoot(event, d);

        return d;
    }

    protected void generateRoot(Event event, Document descriptor) {
        Element eventElement = descriptor.createElementNS(getNamespace(), CONSTANTS.Event.name());
        descriptor.appendChild(eventElement);
        generateInstanceIDs(event, descriptor, eventElement);
    }

    protected void generateInstanceIDs(Event event, Document descriptor, Element rootElement) {
        for (InstanceID instanceID : event.getInstanceIDs()) {
            if (instanceID.getId() == null) {
                continue;
            }
            Element instanceIDElement = appendNewElement(descriptor, rootElement, CONSTANTS.InstanceID.name());
            instanceIDElement.setAttribute(CONSTANTS.val.name(), instanceID.getId().toString());

            for (EventedValue<?> eventedValue : instanceID.getValues()) {
                generateEventedValue(eventedValue, descriptor, instanceIDElement);
            }
        }
    }

    protected void generateEventedValue(EventedValue<?> eventedValue, Document descriptor, Element parentElement) {
        String name = eventedValue.getName();
        Map.Entry<String, String>[] attributes = eventedValue.getAttributes();
        if (attributes != null && attributes.length > 0) {
            Element evElement = appendNewElement(descriptor, parentElement, name);
            for (Map.Entry<String, String> attr : attributes) {
                evElement.setAttribute(attr.getKey(), DOMParser.escape(attr.getValue()));
            }
        }
    }
}
