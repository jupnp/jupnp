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
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.mock.MockUpnpServiceConfiguration;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.control.IncomingActionRequestMessage;
import org.jupnp.model.message.control.IncomingActionResponseMessage;
import org.jupnp.model.message.control.OutgoingActionRequestMessage;
import org.jupnp.model.message.control.OutgoingActionResponseMessage;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.SoapActionHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.SoapActionType;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;
import org.jupnp.transport.spi.SOAPActionProcessor;

class ActionXMLProcessingTest {

    public static final String ENCODED_REQUEST = "<?xml version=\"1.0\"?>\n" + " <s:Envelope\n"
            + "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
            + "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "   <s:Body>\n"
            + "     <u:SetSomeValue xmlns:u=\"urn:schemas-upnp-org:service:SwitchPower:1\">\n"
            + "       <SomeValue>This is encoded: &lt;</SomeValue>\n" + "     </u:SetSomeValue>\n" + "   </s:Body>\n"
            + " </s:Envelope>";

    public static final String ALIAS_ENCODED_REQUEST = "<?xml version=\"1.0\"?>\n" + " <s:Envelope\n"
            + "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n"
            + "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" + "   <s:Body>\n"
            + "     <u:SetSomeValue xmlns:u=\"urn:schemas-upnp-org:service:SwitchPower:1\">\n"
            + "       <SomeValue1>This is encoded: &lt;</SomeValue1>\n" + "     </u:SetSomeValue>\n" + "   </s:Body>\n"
            + " </s:Envelope>";

    static SOAPActionProcessor[][] getProcessors() {
        return new SOAPActionProcessor[][] { { new SOAPActionProcessorImpl() } };
    }

    @ParameterizedTest
    @MethodSource("getProcessors")
    void writeReadRequest(final SOAPActionProcessor processor) throws Exception {

        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("SetTarget");
        ActionInvocation actionInvocation = new ActionInvocation(action);
        actionInvocation.setInput("NewTargetValue", true);

        // The control URL doesn't matter
        OutgoingActionRequestMessage outgoingCall = new OutgoingActionRequestMessage(actionInvocation,
                SampleData.getLocalBaseURL());

        MockUpnpService upnpService = new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public SOAPActionProcessor getSoapActionProcessor() {
                return processor;
            }
        });

        upnpService.getConfiguration().getSoapActionProcessor().writeBody(outgoingCall, actionInvocation);

        StreamRequestMessage incomingStream = new StreamRequestMessage(outgoingCall);
        IncomingActionRequestMessage incomingCall = new IncomingActionRequestMessage(incomingStream, svc);

        actionInvocation = new ActionInvocation(incomingCall.getAction());

        upnpService.getConfiguration().getSoapActionProcessor().readBody(incomingCall, actionInvocation);

        assertEquals(1, actionInvocation.getInput().length);
        assertEquals("NewTargetValue", actionInvocation.getInput()[0].getArgument().getName());
    }

    @ParameterizedTest
    @MethodSource("getProcessors")
    void writeReadResponse(final SOAPActionProcessor processor) throws Exception {

        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("GetTarget");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        OutgoingActionResponseMessage outgoingCall = new OutgoingActionResponseMessage(action);
        actionInvocation.setOutput("RetTargetValue", true);

        MockUpnpService upnpService = new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public SOAPActionProcessor getSoapActionProcessor() {
                return processor;
            }
        });

        upnpService.getConfiguration().getSoapActionProcessor().writeBody(outgoingCall, actionInvocation);

        StreamResponseMessage incomingStream = new StreamResponseMessage(outgoingCall);
        IncomingActionResponseMessage incomingCall = new IncomingActionResponseMessage(incomingStream);

        actionInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(incomingCall, actionInvocation);

        assertEquals("RetTargetValue", actionInvocation.getOutput()[0].getArgument().getName());
    }

    @ParameterizedTest
    @MethodSource("getProcessors")
    void writeFailureReadFailure(final SOAPActionProcessor processor) throws Exception {
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("GetTarget");
        ActionInvocation actionInvocation = new ActionInvocation(action);
        actionInvocation.setFailure(new ActionException(ErrorCode.ACTION_FAILED, "A test string"));

        OutgoingActionResponseMessage outgoingCall = new OutgoingActionResponseMessage(
                UpnpResponse.Status.INTERNAL_SERVER_ERROR);

        MockUpnpService upnpService = new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public SOAPActionProcessor getSoapActionProcessor() {
                return processor;
            }
        });

        upnpService.getConfiguration().getSoapActionProcessor().writeBody(outgoingCall, actionInvocation);

        StreamResponseMessage incomingStream = new StreamResponseMessage(outgoingCall);
        IncomingActionResponseMessage incomingCall = new IncomingActionResponseMessage(incomingStream);

        actionInvocation = new ActionInvocation(action);
        upnpService.getConfiguration().getSoapActionProcessor().readBody(incomingCall, actionInvocation);

        assertEquals(ErrorCode.ACTION_FAILED.getCode(), actionInvocation.getFailure().getErrorCode());
        // Note the period at the end of the test string!
        assertEquals(ErrorCode.ACTION_FAILED.getDescription() + ". A test string.",
                actionInvocation.getFailure().getMessage());
    }

    @ParameterizedTest
    @MethodSource("getProcessors")
    void readEncodedRequest(final SOAPActionProcessor processor) throws Exception {
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("SetSomeValue");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        MockUpnpService upnpService = new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public SOAPActionProcessor getSoapActionProcessor() {
                return processor;
            }
        });

        StreamRequestMessage streamRequest = new StreamRequestMessage(UpnpRequest.Method.POST,
                URI.create("http://some.uri"));
        streamRequest.getHeaders().add(UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8));
        streamRequest.getHeaders().add(UpnpHeader.Type.SOAPACTION,
                new SoapActionHeader(new SoapActionType(action.getService().getServiceType(), action.getName())));
        streamRequest.setBody(UpnpMessage.BodyType.STRING, ENCODED_REQUEST);

        IncomingActionRequestMessage request = new IncomingActionRequestMessage(streamRequest, svc);

        upnpService.getConfiguration().getSoapActionProcessor().readBody(request, actionInvocation);

        assertEquals("This is encoded: <", actionInvocation.getInput()[0].toString());
    }

    @ParameterizedTest
    @MethodSource("getProcessors")
    void readEncodedRequestWithAlias(final SOAPActionProcessor processor) throws Exception {
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("SetSomeValue");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        MockUpnpService upnpService = new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public SOAPActionProcessor getSoapActionProcessor() {
                return processor;
            }
        });

        StreamRequestMessage streamRequest = new StreamRequestMessage(UpnpRequest.Method.POST,
                URI.create("http://some.uri"));
        streamRequest.getHeaders().add(UpnpHeader.Type.CONTENT_TYPE,
                new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8));
        streamRequest.getHeaders().add(UpnpHeader.Type.SOAPACTION,
                new SoapActionHeader(new SoapActionType(action.getService().getServiceType(), action.getName())));
        streamRequest.setBody(UpnpMessage.BodyType.STRING, ALIAS_ENCODED_REQUEST);

        IncomingActionRequestMessage request = new IncomingActionRequestMessage(streamRequest, svc);

        upnpService.getConfiguration().getSoapActionProcessor().readBody(request, actionInvocation);

        assertEquals("This is encoded: <", actionInvocation.getInput()[0].toString());
        assertEquals("This is encoded: <", actionInvocation.getInput("SomeValue").toString());
    }

    @ParameterizedTest
    @MethodSource("getProcessors")
    void writeDecodedResponse(final SOAPActionProcessor processor) throws Exception {
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceExtended.class);
        LocalService svc = ld.getServices()[0];

        Action action = svc.getAction("GetSomeValue");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        MockUpnpService upnpService = new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public SOAPActionProcessor getSoapActionProcessor() {
                return processor;
            }
        });

        OutgoingActionResponseMessage response = new OutgoingActionResponseMessage(action);
        actionInvocation.setOutput("SomeValue", "This is decoded: &<>'\"");

        upnpService.getConfiguration().getSoapActionProcessor().writeBody(response, actionInvocation);

        // Note that quotes are not encoded because this text is not an XML attribute value!
        assertTrue(response.getBodyString().contains("<SomeValue>This is decoded: &amp;&lt;&gt;'\"</SomeValue>"));
    }
}
