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
package org.jupnp.protocol.sync;

import java.net.URI;

import org.jupnp.UpnpService;
import org.jupnp.binding.xml.DescriptorBindingException;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.ContentTypeHeader;
import org.jupnp.model.message.header.ServerHeader;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.resource.DeviceDescriptorResource;
import org.jupnp.model.resource.IconResource;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.resource.ServiceDescriptorResource;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles reception of device/service descriptor and icon retrieval messages.
 *
 * <p>
 * Requested device and service XML descriptors are generated on-the-fly for every request.
 * </p>
 * <p>
 * Descriptor XML is dynamically generated depending on the control point - some control
 * points require different metadata than others for the same device and services.
 * </p>
 *
 * @author Christian Bauer
 */
public class ReceivingRetrieval extends ReceivingSync<StreamRequestMessage, StreamResponseMessage> {

    private final Logger logger = LoggerFactory.getLogger(ReceivingRetrieval.class);

    public ReceivingRetrieval(UpnpService upnpService, StreamRequestMessage inputMessage) {
        super(upnpService, inputMessage);
    }

    @Override
    protected StreamResponseMessage executeSync() throws RouterException {

        if (!getInputMessage().hasHostHeader()) {
            logger.trace("Ignoring message, missing HOST header: {}", getInputMessage());
            return new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.PRECONDITION_FAILED));
        }

        URI requestedURI = getInputMessage().getOperation().getURI();

        Resource foundResource = getUpnpService().getRegistry().getResource(requestedURI);

        if (foundResource == null) {
            foundResource = onResourceNotFound(requestedURI);
            if (foundResource == null) {
                logger.trace("No local resource found: {}", getInputMessage());
                return null;
            }
        }

        return createResponse(requestedURI, foundResource);
    }

    protected StreamResponseMessage createResponse(URI requestedURI, Resource resource) {

        StreamResponseMessage response;

        try {

            if (DeviceDescriptorResource.class.isAssignableFrom(resource.getClass())) {

                logger.trace("Found local device matching relative request URI: {}", requestedURI);
                LocalDevice device = (LocalDevice) resource.getModel();

                DeviceDescriptorBinder deviceDescriptorBinder = getUpnpService().getConfiguration()
                        .getDeviceDescriptorBinderUDA10();
                String deviceDescriptor = deviceDescriptorBinder.generate(device, getRemoteClientInfo(),
                        getUpnpService().getConfiguration().getNamespace());
                response = new StreamResponseMessage(deviceDescriptor,
                        new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE));
            } else if (ServiceDescriptorResource.class.isAssignableFrom(resource.getClass())) {

                logger.trace("Found local service matching relative request URI: {}", requestedURI);
                LocalService service = (LocalService) resource.getModel();

                ServiceDescriptorBinder serviceDescriptorBinder = getUpnpService().getConfiguration()
                        .getServiceDescriptorBinderUDA10();
                String serviceDescriptor = serviceDescriptorBinder.generate(service);
                response = new StreamResponseMessage(serviceDescriptor,
                        new ContentTypeHeader(ContentTypeHeader.DEFAULT_CONTENT_TYPE));

            } else if (IconResource.class.isAssignableFrom(resource.getClass())) {

                logger.trace("Found local icon matching relative request URI: {}", requestedURI);
                Icon icon = (Icon) resource.getModel();
                response = new StreamResponseMessage(icon.getData(), icon.getMimeType());

            } else {

                logger.trace("Ignoring GET for found local resource: {}", resource);
                return null;
            }

        } catch (DescriptorBindingException e) {
            logger.warn("Error generating requested device/service descriptor", e);
            response = new StreamResponseMessage(UpnpResponse.Status.INTERNAL_SERVER_ERROR);
        }

        response.getHeaders().add(UpnpHeader.Type.SERVER, new ServerHeader());

        return response;
    }

    /**
     * Called if the {@link org.jupnp.registry.Registry} had no result.
     *
     * @param requestedURIPath The requested URI path
     * @return <code>null</code> or your own {@link Resource}
     */
    protected Resource onResourceNotFound(URI requestedURIPath) {
        return null;
    }
}
