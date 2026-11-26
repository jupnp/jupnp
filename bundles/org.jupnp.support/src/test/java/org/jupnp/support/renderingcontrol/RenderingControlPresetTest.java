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
package org.jupnp.support.renderingcontrol;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.model.DefaultServiceManager;
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
import org.jupnp.support.model.Channel;
import org.jupnp.transport.impl.SOAPActionProcessorImpl;

public class RenderingControlPresetTest {

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

    private static ActionInvocation<LocalService> selectPreset(LocalService<?> service, String name) throws Exception {
        StreamRequestMessage request = new StreamRequestMessage(UpnpRequest.Method.POST, URI.create("/control"));
        request.getHeaders().add(UpnpHeader.Type.SOAPACTION,
                new SoapActionHeader(new SoapActionType(service.getServiceType(), "SelectPreset")));
        request.setBody(UpnpMessage.BodyType.STRING,
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"><s:Body>"
                        + "<u:SelectPreset xmlns:u=\"urn:schemas-upnp-org:service:RenderingControl:1\">"
                        + "<InstanceID>0</InstanceID><PresetName>" + name + "</PresetName>"
                        + "</u:SelectPreset></s:Body></s:Envelope>");
        IncomingActionRequestMessage incoming = new IncomingActionRequestMessage(request, service);
        ActionInvocation<LocalService> invocation = new ActionInvocation<>(incoming.getAction());
        new SOAPActionProcessorImpl().readBody(incoming, invocation);
        service.getExecutor(incoming.getAction()).execute(invocation);
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
            return new UnsignedIntegerTwoBytes(0);
        }

        @Override
        public void setVolume(UnsignedIntegerFourBytes instance, String channel, UnsignedIntegerTwoBytes volume) {
        }

        @Override
        protected Channel[] getCurrentChannels() {
            return new Channel[] { Channel.Master };
        }

        @Override
        public UnsignedIntegerFourBytes[] getCurrentInstanceIds() {
            return new UnsignedIntegerFourBytes[] { getDefaultInstanceID() };
        }
    }
}
