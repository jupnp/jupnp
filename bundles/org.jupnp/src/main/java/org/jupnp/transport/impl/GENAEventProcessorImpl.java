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

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;

import org.jupnp.model.Constants;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.XMLUtil;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.gena.IncomingEventRequestMessage;
import org.jupnp.model.message.gena.OutgoingEventRequestMessage;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Default implementation based on the <em>W3C DOM</em> XML processing API.
 *
 * @author Christian Bauer
 */
public class GENAEventProcessorImpl extends PooledXmlProcessor implements GENAEventProcessor, ErrorHandler {

    private final Logger logger = LoggerFactory.getLogger(GENAEventProcessor.class);

    protected DocumentBuilderFactory createDocumentBuilderFactory() throws FactoryConfigurationError {
        return DocumentBuilderFactory.newInstance();
    }

    @Override
    public void writeBody(OutgoingEventRequestMessage requestMessage) throws UnsupportedDataException {
        logger.trace("Writing body of: {}", requestMessage);

        try {

            Document d = newDocument();
            Element propertysetElement = writePropertysetElement(d);

            writeProperties(d, propertysetElement, requestMessage);

            requestMessage.setBody(UpnpMessage.BodyType.STRING, toString(d));

            if (logger.isTraceEnabled()) {
                logger.trace(
                        "===================================== GENA BODY BEGIN ============================================");
                logger.trace(requestMessage.getBody().toString());
                logger.trace(
                        "====================================== GENA BODY END =============================================");
            }

        } catch (Exception e) {
            throw new UnsupportedDataException("Can't transform message payload: " + e.getMessage(), e);
        }
    }

    @Override
    public void readBody(IncomingEventRequestMessage requestMessage) throws UnsupportedDataException {

        logger.trace("Reading body of: {}", requestMessage);
        if (logger.isTraceEnabled()) {
            logger.trace(
                    "===================================== GENA BODY BEGIN ============================================");
            logger.trace(requestMessage.getBody() != null ? requestMessage.getBody().toString() : "null");
            logger.trace(
                    "-===================================== GENA BODY END ============================================");
        }

        String body = getMessageBody(requestMessage);
        try {
            Document d = readDocument(new InputSource(new StringReader(body)), this);

            Element propertysetElement = readPropertysetElement(d);

            readProperties(propertysetElement, requestMessage);

        } catch (Exception e) {
            throw new UnsupportedDataException("Can't transform message payload: " + e.getMessage(), e, body);
        }
    }

    /* ##################################################################################################### */

    protected Element writePropertysetElement(Document d) {
        Element propertysetElement = d.createElementNS(Constants.NS_UPNP_EVENT_10, "e:propertyset");
        d.appendChild(propertysetElement);
        return propertysetElement;
    }

    protected Element readPropertysetElement(Document d) {

        Element propertysetElement = d.getDocumentElement();
        if (propertysetElement == null || !getUnprefixedNodeName(propertysetElement).equals("propertyset")) {
            throw new RuntimeException("Root element was not 'propertyset'");
        }
        return propertysetElement;
    }

    /* ##################################################################################################### */

    protected void writeProperties(Document d, Element propertysetElement, OutgoingEventRequestMessage message) {
        for (StateVariableValue stateVariableValue : message.getStateVariableValues()) {
            Element propertyElement = d.createElementNS(Constants.NS_UPNP_EVENT_10, "e:property");
            propertysetElement.appendChild(propertyElement);
            XMLUtil.appendNewElement(d, propertyElement, stateVariableValue.getStateVariable().getName(),
                    stateVariableValue.toString());
        }
    }

    protected void readProperties(Element propertysetElement, IncomingEventRequestMessage message) {
        NodeList propertysetElementChildren = propertysetElement.getChildNodes();

        StateVariable[] stateVariables = message.getService().getStateVariables();

        for (int i = 0; i < propertysetElementChildren.getLength(); i++) {
            Node propertysetChild = propertysetElementChildren.item(i);

            if (propertysetChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (getUnprefixedNodeName(propertysetChild).equals("property")) {

                NodeList propertyChildren = propertysetChild.getChildNodes();

                for (int j = 0; j < propertyChildren.getLength(); j++) {
                    Node propertyChild = propertyChildren.item(j);

                    if (propertyChild.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    String stateVariableName = getUnprefixedNodeName(propertyChild);
                    for (StateVariable stateVariable : stateVariables) {
                        if (stateVariable.getName().equals(stateVariableName)) {
                            logger.trace("Reading state variable value: {}", stateVariableName);
                            String value = XMLUtil.getTextContent(propertyChild);
                            try {
                                message.getStateVariableValues().add(new StateVariableValue(stateVariable, value));
                            } catch (InvalidValueException e) {
                                logger.debug("Value {} for the state variable {} ignored: {}", value, stateVariableName,
                                        e.getMessage());
                            }
                            break;
                        }
                    }

                }
            }
        }
    }

    /* ##################################################################################################### */

    protected String getMessageBody(UpnpMessage message) throws UnsupportedDataException {
        if (!message.isBodyNonEmptyString()) {
            throw new UnsupportedDataException("Can't transform null or non-string/zero-length body of: " + message);
        }
        return message.getBodyString().trim();
    }

    protected String toString(Document d) throws Exception {
        // Just to be safe, no newline at the end
        String output = XMLUtil.documentToString(d);
        while (output.endsWith("\n") || output.endsWith("\r")) {
            output = output.substring(0, output.length() - 1);
        }

        return output;
    }

    protected String getUnprefixedNodeName(Node node) {
        return node.getPrefix() != null ? node.getNodeName().substring(node.getPrefix().length() + 1)
                : node.getNodeName();
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        logger.warn(e.toString());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
}
