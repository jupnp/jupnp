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

import org.junit.jupiter.api.Test;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleServiceOne;
import org.jupnp.mock.MockRouter;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.SoapActionHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableEventDetails;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.profile.ClientInfo;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.transport.RouterException;

class ActionInvokeOutgoingTest {

    // @formatter:off
    public static final String RESPONSE_SUCCESSFUL = "<?xml version=\"1.0\"?>\n" +
        " <s:Envelope\n" +
        "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
        "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
        "   <s:Body>\n" +
        "     <u:GetTargetResponse xmlns:u=\"urn:schemas-upnp-org:service:SwitchPower:1\">\n" +
        "       <RetTargetValue>0</RetTargetValue>\n" +
        "     </u:GetTargetResponse>\n" +
        "   </s:Body>\n" +
        " </s:Envelope>";

    public static final String RESPONSE_QUERY_VARIABLE = "<?xml version=\"1.0\"?>\n" +
        " <s:Envelope\n" +
        "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
        "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
        "   <s:Body>\n" +
        "     <u:QueryStateVariableResponse xmlns:u=\"urn:schemas-upnp-org:control-1-0\">\n" +
        "       <return>0</return>\n" +
        "     </u:QueryStateVariableResponse>\n" +
        "   </s:Body>\n" +
        " </s:Envelope>";

    public static final String RESPONSE_FAILURE = "<?xml version=\"1.0\"?>\n" +
        " <s:Envelope\n" +
        "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
        "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
        "   <s:Body>\n" +
        "     <s:Fault>\n" +
        "       <faultcode>s:Client</faultcode>\n" +
        "       <faultstring>UPnPError</faultstring>\n" +
        "       <detail>\n" +
        "         <UPnPError xmlns=\"urn:schemas-upnp-org:control-1-0\">\n" +
        "           <errorCode>611</errorCode>\n" +
        "           <errorDescription>A test string</errorDescription>\n" +
        "         </UPnPError>\n" +
        "       </detail>\n" +
        "     </s:Fault>\n" +
        "   </s:Body>\n" +
        " </s:Envelope>";

    public static final String RESPONSE_NEGATIVE_VALUE = "<?xml version=\"1.0\"?>\n" +
        " <s:Envelope\n" +
        "     xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
        "     s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
        "   <s:Body>\n" +
        "     <u:GetNegativeValueResponse xmlns:u=\"urn:schemas-upnp-org:service:MyService:1\">\n" +
        "       <Result>-1</Result>\n" + // That's an illegal value for this state var!
        "     </u:GetNegativeValueResponse>\n" +
        "   </s:Body>\n" +
        " </s:Envelope>";
    // @formatter:on

    @Test
    void callLocalGet() throws Exception {

        // Registry local device and its service
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        LocalDevice ld = ActionSampleData.createTestDevice();
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        Action action = service.getAction("GetTarget");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        final boolean[] assertions = new boolean[1];
        ActionCallback callback = new ActionCallback(actionInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                assertions[0] = true;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                assertions[0] = false;
            }
        };

        upnpService.getControlPoint().execute(callback);

        assertNull(actionInvocation.getFailure());
        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertTrue(assertions[0]);
        assertEquals(1, actionInvocation.getOutput().length);
        assertEquals("0", actionInvocation.getOutput()[0].toString());
    }

    @Test
    void callLocalWrongAction() throws Exception {
        // Registry local device and its service
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        LocalDevice ld = ActionSampleData.createTestDevice();
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        assertNull(service.getAction("NonExistentAction"));
    }

    @Test
    void callLocalSetException() throws Exception {
        // Registry local device and its service
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();
        LocalDevice ld = ActionSampleData.createTestDevice(ActionSampleData.LocalTestServiceThrowsException.class);
        LocalService service = ld.getServices()[0];
        upnpService.getRegistry().addDevice(ld);

        Action action = service.getAction("SetTarget");
        ActionInvocation actionInvocation = new ActionInvocation(action);

        actionInvocation.setInput("NewTargetValue", true);

        final boolean[] assertions = new boolean[1];
        ActionCallback callback = new ActionCallback(actionInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                assertions[0] = false;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                assertNull(operation); // Local calls don't have an operation
                assertions[0] = true;
            }
        };

        upnpService.getControlPoint().execute(callback);

        assertNotNull(actionInvocation.getFailure());
        assertEquals(0, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertTrue(assertions[0]);

        assertEquals(ErrorCode.ACTION_FAILED.getCode(), actionInvocation.getFailure().getErrorCode());
        assertEquals(ErrorCode.ACTION_FAILED.getDescription() + ". Something is wrong.",
                actionInvocation.getFailure().getMessage());
    }

    @Test
    void callRemoteGet() {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {

                    @Override
                    public StreamResponseMessage send(StreamRequestMessage msg) throws RouterException {
                        return super.send(msg);
                    }

                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] { new StreamResponseMessage(RESPONSE_SUCCESSFUL) };
                    }
                };
            }
        };
        upnpService.startup();

        // Register remote device and its service
        RemoteDevice device = SampleData.createRemoteDevice();
        Service<RemoteDevice, RemoteService> service = SampleData.getFirstService(device);
        upnpService.getRegistry().addDevice(device);

        Action action = service.getAction("GetTarget");

        UpnpHeaders extraHeaders = new UpnpHeaders();
        extraHeaders.add(UpnpHeader.Type.USER_AGENT.getHttpName(), "MyCustom/Agent");
        extraHeaders.add("X-Custom-Header", "foo");

        ActionInvocation actionInvocation = new ActionInvocation(action, new ClientInfo(extraHeaders));

        final boolean[] assertions = new boolean[1];
        ActionCallback callback = new ActionCallback(actionInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                assertions[0] = true;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                assertions[0] = false;
            }
        };

        upnpService.getControlPoint().execute(callback);

        assertNull(actionInvocation.getFailure());
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertTrue(assertions[0]);

        StreamRequestMessage request = upnpService.getRouter().getSentStreamRequestMessages().get(0);

        // Mandatory headers
        assertEquals(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8.toString(),
                request.getHeaders().getFirstHeaderString(UpnpHeader.Type.CONTENT_TYPE));
        assertEquals("\"" + SampleServiceOne.getThisServiceType() + "#GetTarget\"",
                request.getHeaders().getFirstHeaderString(UpnpHeader.Type.SOAPACTION));

        // The extra headers
        assertEquals("MyCustom/Agent", request.getHeaders().getFirstHeaderString(UpnpHeader.Type.USER_AGENT));
        assertEquals("foo", request.getHeaders().getFirstHeader("X-CUSTOM-HEADER"));

        assertEquals(1, actionInvocation.getOutput().length);
        assertEquals("0", actionInvocation.getOutput()[0].toString());
    }

    @Test
    void callRemoteGetFailure() {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] { new StreamResponseMessage(
                                new UpnpResponse(UpnpResponse.Status.INTERNAL_SERVER_ERROR), RESPONSE_FAILURE) };
                    }
                };
            }
        };
        upnpService.startup();

        // Registery remote device and its service
        RemoteDevice device = SampleData.createRemoteDevice();
        Service<RemoteDevice, RemoteService> service = SampleData.getFirstService(device);
        upnpService.getRegistry().addDevice(device);

        Action action = service.getAction("GetTarget");

        ActionInvocation actionInvocation = new ActionInvocation(action);
        final boolean[] assertions = new boolean[1];
        ActionCallback callback = new ActionCallback(actionInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                assertions[0] = false;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                assertEquals(UpnpResponse.Status.INTERNAL_SERVER_ERROR.getStatusCode(), operation.getStatusCode());
                assertions[0] = true;
            }
        };

        upnpService.getControlPoint().execute(callback);

        assertNotNull(actionInvocation.getFailure());
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertTrue(assertions[0]);
        assertEquals(ErrorCode.INVALID_CONTROL_URL.getCode(), actionInvocation.getFailure().getErrorCode());
        assertEquals("A test string", actionInvocation.getFailure().getMessage());
    }

    @Test
    void callRemoteGetNotFoundFailure() {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] {
                                new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.NOT_FOUND)) };
                    }
                };
            }
        };
        upnpService.startup();

        // Registry remote device and its service
        RemoteDevice device = SampleData.createRemoteDevice();
        Service<RemoteDevice, RemoteService> service = SampleData.getFirstService(device);
        upnpService.getRegistry().addDevice(device);

        Action action = service.getAction("GetTarget");

        ActionInvocation actionInvocation = new ActionInvocation(action);
        final boolean[] assertions = new boolean[1];
        ActionCallback callback = new ActionCallback(actionInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                assertions[0] = false;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                assertEquals(operation.getStatusCode(), UpnpResponse.Status.NOT_FOUND.getStatusCode());
                assertions[0] = true;
            }
        };

        upnpService.getControlPoint().execute(callback);

        assertNotNull(actionInvocation.getFailure());
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertTrue(assertions[0]);
        assertEquals(ErrorCode.ACTION_FAILED.getCode(), actionInvocation.getFailure().getErrorCode());
        assertEquals(
                ErrorCode.ACTION_FAILED.getDescription() + ". Non-recoverable remote execution failure: 404 Not Found.",
                actionInvocation.getFailure().getMessage());
    }

    @Test
    void callRemoteGetNoResponse() {
        MockUpnpService upnpService = new MockUpnpService();
        upnpService.startup();

        // Registry remote device and its service
        RemoteDevice device = SampleData.createRemoteDevice();
        Service<RemoteDevice, RemoteService> service = SampleData.getFirstService(device);
        upnpService.getRegistry().addDevice(device);

        Action action = service.getAction("GetTarget");

        ActionInvocation actionInvocation = new ActionInvocation(action);
        final boolean[] assertions = new boolean[1];
        ActionCallback callback = new ActionCallback(actionInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                assertions[0] = false;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                assertNull(operation);
                assertions[0] = true;
            }
        };

        upnpService.getControlPoint().execute(callback);

        assertNotNull(actionInvocation.getFailure());
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertTrue(assertions[0]);
        assertEquals(ErrorCode.ACTION_FAILED.getCode(), actionInvocation.getFailure().getErrorCode());
        assertEquals(ErrorCode.ACTION_FAILED.getDescription() + ". Connection error or no response received.",
                actionInvocation.getFailure().getMessage());
    }

    @Test
    void callRemoteNegativeValue() throws Exception {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] { new StreamResponseMessage(RESPONSE_NEGATIVE_VALUE) };
                    }
                };
            }
        };
        upnpService.startup();

        // Registry remote device and its service
        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity(), new UDADeviceType("MyDevice"),
                new DeviceDetails("JustATest"),
                new RemoteService(new UDAServiceType("MyService"), new UDAServiceId("MyService"),
                        URI.create("/scpd.xml"), URI.create("/control"), URI.create("/events"),
                        new Action[] { new Action("GetNegativeValue",
                                new ActionArgument[] { new ActionArgument("Result", "NegativeValue",
                                        ActionArgument.Direction.OUT) }) },
                        new StateVariable[] { new StateVariable("NegativeValue",
                                new StateVariableTypeDetails(Datatype.Builtin.UI4.getDatatype()),
                                new StateVariableEventDetails(false)) }));

        upnpService.getRegistry().addDevice(device);

        Action<RemoteService> action = device.getServices()[0].getAction("GetNegativeValue");

        ActionInvocation actionInvocation = new ActionInvocation(action);
        final boolean[] assertions = new boolean[1];
        ActionCallback callback = new ActionCallback(actionInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                assertions[0] = true;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                assertions[0] = false;
            }
        };

        upnpService.getControlPoint().execute(callback);

        assertNull(actionInvocation.getFailure());
        // The illegal "-1" value should have been converted (with warning) to 0
        assertEquals(new UnsignedIntegerFourBytes(0), actionInvocation.getOutput("Result").getValue());
    }

    @Test
    void callRemoteQueryStateVariable() {
        MockUpnpService upnpService = new MockUpnpService() {
            @Override
            protected MockRouter createRouter() {
                return new MockRouter(getConfiguration(), getProtocolFactory()) {
                    @Override
                    public StreamResponseMessage[] getStreamResponseMessages() {
                        return new StreamResponseMessage[] { new StreamResponseMessage(RESPONSE_QUERY_VARIABLE) };
                    }
                };
            }
        };
        upnpService.startup();

        // Registery remote device and its service
        RemoteDevice device = SampleData.createRemoteDevice();
        Service<RemoteDevice, RemoteService> service = SampleData.getFirstService(device);
        upnpService.getRegistry().addDevice(device);

        Action action = service.getQueryStateVariableAction();
        ActionInvocation actionInvocation = new ActionInvocation(action);
        actionInvocation.setInput("varName", "Target");

        final boolean[] assertions = new boolean[1];
        ActionCallback callback = new ActionCallback(actionInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                assertions[0] = true;
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                assertions[0] = false;
            }
        };

        upnpService.getControlPoint().execute(callback);

        assertNull(actionInvocation.getFailure());
        assertEquals(1, upnpService.getRouter().getSentStreamRequestMessages().size());
        assertTrue(assertions[0]);
        assertEquals(ContentTypeHeader.DEFAULT_CONTENT_TYPE_UTF8.toString(),
                upnpService.getRouter().getSentStreamRequestMessages().get(0).getHeaders()
                        .getFirstHeader(UpnpHeader.Type.CONTENT_TYPE, ContentTypeHeader.class).getString());
        assertEquals("\"urn:schemas-upnp-org:control-1-0#QueryStateVariable\"",
                upnpService.getRouter().getSentStreamRequestMessages().get(0).getHeaders()
                        .getFirstHeader(UpnpHeader.Type.SOAPACTION, SoapActionHeader.class).getString());
        assertEquals(1, actionInvocation.getOutput().length);
        assertEquals("return", actionInvocation.getOutput()[0].getArgument().getName());
        assertEquals("0", actionInvocation.getOutput()[0].toString());
    }
}
