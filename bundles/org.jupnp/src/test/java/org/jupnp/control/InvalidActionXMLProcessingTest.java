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
package org.jupnp.control;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.UpnpService;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.control.IncomingActionRequestMessage;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.SoapActionHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.SoapActionType;
import org.jupnp.util.io.IO;

/**
 * @author Christian Bauer
 */
class InvalidActionXMLProcessingTest {

    static String[][] getInvalidXMLFile() {
        return new String[][] { { "/invalidxml/control/request_missing_envelope.xml" },
                { "/invalidxml/control/request_missing_action_namespace.xml" },
                { "/invalidxml/control/request_invalid_action_namespace.xml" }, };
    }

    static String[][] getInvalidRecoverableXMLFile() {
        return new String[][] { { "/invalidxml/control/request_no_entityencoding.xml" },
                { "/invalidxml/control/request_wrong_termination.xml" }, };
    }

    static String[][] getInvalidUnrecoverableXMLFile() {
        return new String[][] { { "/invalidxml/control/unrecoverable/naim_unity.xml" }, };
    }

    /* ############################## TEST FAILURE ############################ */

    @ParameterizedTest
    @MethodSource("getInvalidXMLFile")
    void readRequestDefaultFailure(String invalidXMLFile) {
        assertThrows(UnsupportedDataException.class, () -> readRequest(invalidXMLFile, new MockUpnpService()));
    }

    @ParameterizedTest
    @MethodSource("getInvalidRecoverableXMLFile")
    void readRequestRecoverableFailure(String invalidXMLFile) {
        assertThrows(UnsupportedDataException.class, () -> readRequest(invalidXMLFile, new MockUpnpService()));
    }

    protected void readRequest(String invalidXMLFile, UpnpService upnpService) throws Exception {
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("SetSomeValue");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        StreamRequestMessage message = createRequestMessage(action, invalidXMLFile);
        IncomingActionRequestMessage request = new IncomingActionRequestMessage(message, svc);

        upnpService.getConfiguration().getSoapActionProcessor().readBody(request, actionInvocation);

        assertEquals("foo&bar", actionInvocation.getInput()[0].toString());
    }

    public StreamRequestMessage createRequestMessage(Action action, String xmlFile) throws Exception {
        StreamRequestMessage message = new StreamRequestMessage(UpnpRequest.Method.POST, URI.create("http://some.uri"));

        message.getHeaders().add(UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8));
        message.getHeaders().add(UpnpHeader.Type.SOAPACTION,
                new SoapActionHeader(new SoapActionType(action.getService().getServiceType(), action.getName())));
        message.setBody(IO.readLines(getClass().getResourceAsStream(xmlFile)));
        return message;
    }
}
