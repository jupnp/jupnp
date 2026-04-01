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
package org.jupnp.gena;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.mock.MockUpnpServiceConfiguration;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.gena.IncomingEventRequestMessage;
import org.jupnp.model.message.gena.OutgoingEventRequestMessage;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.state.StateVariableValue;
import org.jupnp.transport.impl.GENAEventProcessorImpl;
import org.jupnp.transport.spi.GENAEventProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

class EventXMLProcessingTest {

    public static final String EVENT_MSG = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>"
            + "<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">" + "<e:property>" + "<Status>0</Status>"
            + "</e:property>" + "<e:property>" + "<SomeVar></SomeVar>" + "</e:property>" + "</e:propertyset>";

    @Test
    void writeReadRequest() throws Exception {
        MockUpnpService upnpService = new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public GENAEventProcessor getGenaEventProcessor() {
                return new GENAEventProcessorImpl();
            }
        });
        writeReadRequest(upnpService);
    }

    void writeReadRequest(MockUpnpService upnpService) throws Exception {

        LocalDevice localDevice = GenaSampleData.createTestDevice(GenaSampleData.LocalTestService.class);
        LocalService localService = localDevice.getServices()[0];

        List<URL> urls = List.of(SampleData.getLocalBaseURL());

        LocalGENASubscription subscription = new LocalGENASubscription(localService, 1800, urls) {
            void failed(Exception e) {
                throw new RuntimeException("TEST SUBSCRIPTION FAILED: " + e);
            }

            @Override
            public void ended(CancelReason reason) {
            }

            @Override
            public void established() {
            }

            @Override
            public void eventReceived() {
            }
        };

        OutgoingEventRequestMessage outgoingCall = new OutgoingEventRequestMessage(subscription,
                subscription.getCallbackURLs().get(0));

        upnpService.getConfiguration().getGenaEventProcessor().writeBody(outgoingCall);

        assertTrue(xmlDocumentsEqual((String) outgoingCall.getBody(), EVENT_MSG));

        StreamRequestMessage incomingStream = new StreamRequestMessage(outgoingCall);

        RemoteDevice remoteDevice = SampleData.createRemoteDevice();
        RemoteService remoteService = SampleData.getFirstService(remoteDevice);

        IncomingEventRequestMessage incomingCall = new IncomingEventRequestMessage(incomingStream, remoteService);

        upnpService.getConfiguration().getGenaEventProcessor().readBody(incomingCall);

        assertEquals(2, incomingCall.getStateVariableValues().size());

        boolean gotValueOne = false;
        boolean gotValueTwo = false;
        for (StateVariableValue stateVariableValue : incomingCall.getStateVariableValues()) {
            if (stateVariableValue.getStateVariable().getName().equals("Status")) {
                gotValueOne = !(Boolean) stateVariableValue.getValue();
            }
            if (stateVariableValue.getStateVariable().getName().equals("SomeVar")) {
                // TODO: So... can it be null at all? It has a default value...
                gotValueTwo = stateVariableValue.getValue() == null;
            }
        }
        assertTrue(gotValueOne && gotValueTwo);
    }

    /**
     * Used to compare the two given xmls for equality regardless of property order.
     * 
     * @param xml1
     * @param xml2
     * @return
     * @throws SAXException
     * @throws IOException
     * @throws ParserConfigurationException
     */
    private boolean xmlDocumentsEqual(String xml1, String xml2)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        InputSource is1 = new InputSource();
        is1.setCharacterStream(new StringReader(xml1));

        InputSource is2 = new InputSource();
        is2.setCharacterStream(new StringReader(xml2));

        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document1 = builder.parse(is1);
        document1.normalizeDocument();

        Document document2 = builder.parse(is2);
        document2.normalizeDocument();

        boolean childrenEq = nodeListsEqual(document1.getChildNodes(), document2.getChildNodes());

        if (!childrenEq) {
            return false;
        }

        document1.removeChild(document1.getChildNodes().item(0));
        document2.removeChild(document2.getChildNodes().item(0));

        return document1.isEqualNode(document2);
    }

    private boolean nodeListsEqual(NodeList childNodes1, NodeList childNodes2) {

        // compare the two node sets
        for (int i = 0; i < childNodes1.getLength(); i++) {
            Node item1 = childNodes1.item(i);
            boolean found = false;
            for (int j = 0; j < childNodes2.getLength(); j++) {
                Node item2 = childNodes1.item(j);
                if (item1.isEqualNode(item2)) {
                    found = nodeListsEqual(item1.getChildNodes(), item2.getChildNodes());
                    break;
                }
            }

            if (!found) {
                return false;
            }

        }

        return true;
    }
}
