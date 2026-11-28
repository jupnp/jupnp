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
package org.jupnp.support;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.control.IncomingActionRequestMessage;
import org.jupnp.model.message.header.SoapActionHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.SoapActionType;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.UnsignedIntegerTwoBytes;
import org.jupnp.support.avtransport.impl.AVTransportService;
import org.jupnp.support.model.Channel;
import org.jupnp.support.renderingcontrol.AbstractAudioRenderingControl;
import org.jupnp.support.renderingcontrol.RenderingControlException;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;

public class AvServiceSoapArgumentTest {

    @Test
    void selectPresetAcceptsNamesProvidedByTheRenderer() throws Exception {
        LocalService<TestRenderer> service = new AnnotationLocalServiceBinder().read(TestRenderer.class);
        service.setManager(new DefaultServiceManager<>(service, TestRenderer.class));
        TestRenderer renderer = service.getManager().getImplementation();
        assertEquals("FactoryDefaults,Movie",
                renderer.listPresets(AbstractAudioRenderingControl.getDefaultInstanceID()));

        assertNull(selectPreset(service, "FactoryDefaults").getFailure());
        assertNull(selectPreset(service, "Movie").getFailure());
        assertEquals("Movie", renderer.selectedPreset);

        assertEquals(701, selectPreset(service, "Unknown").getFailure().getErrorCode());
        assertEquals("Movie", renderer.selectedPreset);
    }

    @Test
    void getVolumeAcceptsTheStandardBottomChannel() throws Exception {
        LocalService<TestRenderer> service = new AnnotationLocalServiceBinder().read(TestRenderer.class);
        service.setManager(new DefaultServiceManager<>(service, TestRenderer.class));

        ActionInvocation<LocalService> invocation = readAction(service, "GetVolume", "<Channel>B</Channel>");
        service.getExecutor(invocation.getAction()).execute(invocation);

        assertNull(invocation.getFailure());
        assertEquals(new UnsignedIntegerTwoBytes(42), invocation.getOutput("CurrentVolume").getValue());
    }

    @Test
    void unsupportedAvTransportModesKeepTheirActionSpecificFaultCodes() throws Exception {
        LocalService<?> service = new AnnotationLocalServiceBinder().read(AVTransportService.class);
        assertArgumentError(710, service, "Seek", "<Unit>Unknown</Unit><Target>0</Target>");
        assertArgumentError(712, service, "SetPlayMode", "<NewPlayMode>Unknown</NewPlayMode>");
        assertArgumentError(713, service, "SetRecordQualityMode",
                "<NewRecordQualityMode>Unknown</NewRecordQualityMode>");
        assertDoesNotThrow(() -> readAction(service, "Seek", "<Unit>TRACK_NR</Unit><Target>1</Target>"));
    }

    private static void assertArgumentError(int code, LocalService<?> service, String action, String arguments) {
        UnsupportedDataException failure = assertThrows(UnsupportedDataException.class,
                () -> readAction(service, action, arguments));
        assertEquals(code, assertInstanceOf(ActionException.class, failure.getCause()).getErrorCode());
    }

    private static ActionInvocation<LocalService> selectPreset(LocalService<?> service, String name) throws Exception {
        ActionInvocation<LocalService> invocation = readAction(service, "SelectPreset",
                "<PresetName>" + name + "</PresetName>");
        service.getExecutor(invocation.getAction()).execute(invocation);
        return invocation;
    }

    private static ActionInvocation<LocalService> readAction(LocalService<?> service, String action, String arguments)
            throws Exception {
        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, URI.create("/control"));
        request.getHeaders().add(UpnpHeader.Type.SOAPACTION,
                new SoapActionHeader(new SoapActionType(service.getServiceType(), action)));
        request.setBody(UpnpMessage.BodyType.STRING,
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>" + "<u:" + action
                        + " xmlns:u=\"" + service.getServiceType() + "\">" + "<InstanceID>0</InstanceID>" + arguments
                        + "</u:" + action + "></s:Body></s:Envelope>");
        IncomingActionRequestMessage incoming = new IncomingActionRequestMessage(request, service);
        ActionInvocation<LocalService> invocation = new ActionInvocation<>(incoming.getAction());
        new SOAPActionProcessorImpl().readBody(incoming, invocation);
        return invocation;
    }

    public static class TestRenderer extends AbstractAudioRenderingControl {

        private String selectedPreset;

        @Override
        public String listPresets(UnsignedIntegerFourBytes instance) {
            return "FactoryDefaults,Movie";
        }

        @Override
        @UpnpAction
        public void selectPreset(@UpnpInputArgument(name = "InstanceID") UnsignedIntegerFourBytes instance,
                @UpnpInputArgument(name = "PresetName") String name) throws RenderingControlException {
            if (!"Movie".equals(name)) {
                super.selectPreset(instance, name);
            }
            selectedPreset = name;
        }

        @Override
        public boolean getMute(UnsignedIntegerFourBytes instance, String channel) {
            return false;
        }

        @Override
        public void setMute(UnsignedIntegerFourBytes instance, String channel, boolean mute) {
        }

        @Override
        public UnsignedIntegerTwoBytes getVolume(UnsignedIntegerFourBytes instance, String channel) {
            return new UnsignedIntegerTwoBytes("B".equals(channel) ? 42 : 0);
        }

        @Override
        public void setVolume(UnsignedIntegerFourBytes instance, String channel, UnsignedIntegerTwoBytes volume) {
        }

        @Override
        protected Channel[] getCurrentChannels() {
            return new Channel[] { Channel.Master, Channel.B };
        }

        @Override
        public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
            return new UnsignedIntegerFourBytes[] { getDefaultInstanceID() };
        }
    }
}
