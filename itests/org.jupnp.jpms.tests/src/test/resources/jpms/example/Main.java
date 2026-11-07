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
package example;

import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.meta.LocalService;
import org.jupnp.support.avtransport.lastchange.AVTransportLastChangeParser;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.renderingcontrol.lastchange.RenderingControlLastChangeParser;
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.TransportConfigurationProvider;
import org.jupnp.transport.spi.StreamClient;

public class Main {

    public static void main(String[] args) throws Exception {
        requireModule(Main.class, "org.jupnp.jpms.example");
        requireModule(DefaultUpnpServiceConfiguration.class, "org.jupnp");
        requireModule(DIDLParser.class, "org.jupnp.support");
        if (ModuleLayer.boot().findModule("osgi.cmpn").isPresent()) {
            throw new AssertionError("Standalone usage must not require OSGi");
        }

        // The application only requires support, which must transitively expose core and java.xml.
        org.w3c.dom.Document document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        document.appendChild(document.createElement("example"));

        // Bind a private field in a package opened specifically to jUPnP.
        LocalService<ExampleService> service = new AnnotationLocalServiceBinder().read(ExampleService.class);
        service.setManager(new DefaultServiceManager<>(service, ExampleService.class));
        Object value = service.getAccessor("Status").read(service.getManager().getImplementation());
        if (!Boolean.TRUE.equals(value)) {
            throw new AssertionError("Failed to access private service state");
        }

        // Exercise XML parsing and bundled schema resources from both libraries.
        DIDLParser parser = new DIDLParser();
        parser.parse(parser.generate(new DIDLContent()));
        new AVTransportLastChangeParser().parse("<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/AVT/\">"
                + "<InstanceID val=\"0\"><TransportState val=\"STOPPED\"/></InstanceID></Event>");
        new RenderingControlLastChangeParser().parse("<Event xmlns=\"urn:schemas-upnp-org:metadata-1-0/RCS/\">"
                + "<InstanceID val=\"0\"><Volume channel=\"Master\" val=\"10\"/></InstanceID></Event>");

        // No requires clause for the transport: JPMS service binding must resolve its provider.
        TransportConfiguration<?, ?> transport = TransportConfigurationProvider.getDefaultTransportConfiguration();
        requireModule(transport.getClass(), args[0]);
        DefaultUpnpServiceConfiguration configuration = new DefaultUpnpServiceConfiguration();
        StreamClient<?> client = null;
        try {
            client = configuration.createStreamClient();
            requireModule(client.getClass(), args[0]);
        } finally {
            if (client != null) {
                client.stop();
            }
            configuration.shutdown();
        }
    }

    private static void requireModule(Class<?> type, String name) {
        Module module = type.getModule();
        if (!module.isNamed() || !name.equals(module.getName()) || module.getDescriptor().isAutomatic()) {
            throw new AssertionError("Expected explicit module " + name + " for " + type);
        }
    }

    @UpnpService(serviceId = @UpnpServiceId("Example"), serviceType = @UpnpServiceType(value = "Example", version = 1))
    public static class ExampleService {
        @UpnpStateVariable
        private boolean status = true;
    }
}
