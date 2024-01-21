/**
 * Copyright (C) 2014 4th Line GmbH, Switzerland and others
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
 */

package org.jupnp.binding.xml;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jupnp.binding.staging.MutableDevice;
import org.jupnp.binding.staging.MutableIcon;
import org.jupnp.binding.staging.MutableService;
import org.jupnp.binding.staging.MutableUDAVersion;
import org.jupnp.binding.xml.Descriptor.Device.ELEMENT;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.Device;
import org.jupnp.model.types.DLNACaps;
import org.jupnp.model.types.DLNADoc;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.util.MimeType;
import org.jupnp.util.SpecificationViolationReporter;
import org.jupnp.xml.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A JAXP SAX parser implementation, which is actually slower than the DOM implementation (on desktop and on Android)!
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter, make logger final
 */
public class UDA10DeviceDescriptorBinderSAXImpl extends UDA10DeviceDescriptorBinderImpl {

    private final Logger log = LoggerFactory.getLogger(DeviceDescriptorBinder.class);

    @Override
    public <D extends Device> D describe(D undescribedDevice, String descriptorXml)
            throws DescriptorBindingException, ValidationException {

        if (descriptorXml == null || descriptorXml.length() == 0) {
            throw new DescriptorBindingException("Null or empty descriptor");
        }

        try {
            log.trace("Populating device from XML descriptor: {}", undescribedDevice);

            // Read the XML into a mutable descriptor graph

            SAXParser parser = new SAXParser();

            MutableDevice descriptor = new MutableDevice();
            new RootHandler(descriptor, parser);

            parser.parse(new InputSource(
                    // TODO: UPNP VIOLATION: Virgin Media Superhub sends trailing spaces/newlines after last XML
                    // element, need to trim()
                    new StringReader(descriptorXml.trim())));

            // Build the immutable descriptor graph
            return (D) descriptor.build(undescribedDevice);

        } catch (ValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DescriptorBindingException("Could not parse device descriptor", ex);
        }
    }

    protected static class RootHandler extends DeviceDescriptorHandler<MutableDevice> {

        public RootHandler(MutableDevice instance, SAXParser parser) {
            super(instance, parser);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

            if (element.equals(SpecVersionHandler.EL)) {
                MutableUDAVersion udaVersion = new MutableUDAVersion();
                getInstance().udaVersion = udaVersion;
                new SpecVersionHandler(udaVersion, this);
            }

            if (element.equals(DeviceHandler.EL)) {
                new DeviceHandler(getInstance(), this);
            }
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case URLBase:
                    try {
                        String urlString = getCharacters();
                        if (urlString != null && urlString.length() > 0) {
                            // We hope it's RFC 2396 and RFC 2732 compliant
                            getInstance().baseURL = new URL(urlString);
                        }
                    } catch (Exception ex) {
                        throw new SAXException("Invalid URLBase", ex);
                    }
                    break;
            }
        }
    }

    protected static class SpecVersionHandler extends DeviceDescriptorHandler<MutableUDAVersion> {

        public static final ELEMENT EL = ELEMENT.specVersion;

        public SpecVersionHandler(MutableUDAVersion instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case major:
                    String majorVersion = getCharacters().trim();
                    if (!majorVersion.equals("1")) {
                        SpecificationViolationReporter
                                .report("Unsupported UDA major version, ignoring: " + majorVersion, null);
                        majorVersion = "1";
                    }
                    getInstance().major = Integer.valueOf(majorVersion);
                    break;
                case minor:
                    String minorVersion = getCharacters().trim();
                    if (!minorVersion.equals("0")) {
                        SpecificationViolationReporter
                                .report("Unsupported UDA minor version, ignoring: " + minorVersion, null);
                        minorVersion = "0";
                    }
                    getInstance().minor = Integer.valueOf(minorVersion);
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceHandler extends DeviceDescriptorHandler<MutableDevice> {

        public static final ELEMENT EL = ELEMENT.device;

        public DeviceHandler(MutableDevice instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {

            if (element.equals(IconListHandler.EL)) {
                List<MutableIcon> icons = new ArrayList();
                getInstance().icons = icons;
                new IconListHandler(icons, this);
            }

            if (element.equals(ServiceListHandler.EL)) {
                List<MutableService> services = new ArrayList();
                getInstance().services = services;
                new ServiceListHandler(services, this);
            }

            if (element.equals(DeviceListHandler.EL)) {
                List<MutableDevice> devices = new ArrayList();
                getInstance().embeddedDevices = devices;
                new DeviceListHandler(devices, this);
            }
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case deviceType:
                    getInstance().deviceType = getCharacters();
                    break;
                case friendlyName:
                    getInstance().friendlyName = getCharacters();
                    break;
                case manufacturer:
                    getInstance().manufacturer = getCharacters();
                    break;
                case manufacturerURL:
                    getInstance().manufacturerURI = parseURI(getCharacters());
                    break;
                case modelDescription:
                    getInstance().modelDescription = getCharacters();
                    break;
                case modelName:
                    getInstance().modelName = getCharacters();
                    break;
                case modelNumber:
                    getInstance().modelNumber = getCharacters();
                    break;
                case modelURL:
                    getInstance().modelURI = parseURI(getCharacters());
                    break;
                case presentationURL:
                    getInstance().presentationURI = parseURI(getCharacters());
                    break;
                case UPC:
                    getInstance().upc = getCharacters();
                    break;
                case serialNumber:
                    getInstance().serialNumber = getCharacters();
                    break;
                case UDN:
                    getInstance().udn = UDN.valueOf(getCharacters());
                    break;
                case X_DLNADOC:
                    String txt = getCharacters();
                    try {
                        getInstance().dlnaDocs.add(DLNADoc.valueOf(txt));
                    } catch (InvalidValueException ex) {
                        SpecificationViolationReporter.report("Invalid X_DLNADOC value, ignoring value: {}", txt);
                    }
                    break;
                case X_DLNACAP:
                    getInstance().dlnaCaps = DLNACaps.valueOf(getCharacters());
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class IconListHandler extends DeviceDescriptorHandler<List<MutableIcon>> {

        public static final ELEMENT EL = ELEMENT.iconList;

        public IconListHandler(List<MutableIcon> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(IconHandler.EL)) {
                MutableIcon icon = new MutableIcon();
                getInstance().add(icon);
                new IconHandler(icon, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class IconHandler extends DeviceDescriptorHandler<MutableIcon> {

        public static final ELEMENT EL = ELEMENT.icon;

        public IconHandler(MutableIcon instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case width:
                    getInstance().width = Integer.valueOf(getCharacters());
                    break;
                case height:
                    getInstance().height = Integer.valueOf(getCharacters());
                    break;
                case depth:
                    try {
                        getInstance().depth = Integer.valueOf(getCharacters());
                    } catch (NumberFormatException ex) {
                        SpecificationViolationReporter.report("Invalid icon depth '{}', using 16 as default: {}",
                                getCharacters(), ex);
                        getInstance().depth = 16;
                    }
                    break;
                case url:
                    getInstance().uri = parseURI(getCharacters());
                    break;
                case mimetype:
                    try {
                        getInstance().mimeType = getCharacters();
                        MimeType.valueOf(getInstance().mimeType);
                    } catch (IllegalArgumentException ex) {
                        SpecificationViolationReporter.report("Ignoring invalid icon mime type: {}",
                                getInstance().mimeType);
                        getInstance().mimeType = "";
                    }
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ServiceListHandler extends DeviceDescriptorHandler<List<MutableService>> {

        public static final ELEMENT EL = ELEMENT.serviceList;

        public ServiceListHandler(List<MutableService> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(ServiceHandler.EL)) {
                MutableService service = new MutableService();
                getInstance().add(service);
                new ServiceHandler(service, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            boolean last = element.equals(EL);
            if (last) {
                Iterator<MutableService> it = getInstance().iterator();
                while (it.hasNext()) {
                    MutableService service = it.next();
                    if (service.serviceType == null || service.serviceId == null)
                        it.remove();
                }
            }
            return last;
        }
    }

    protected static class ServiceHandler extends DeviceDescriptorHandler<MutableService> {

        public static final ELEMENT EL = ELEMENT.service;

        public ServiceHandler(MutableService instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            try {
                switch (element) {
                    case serviceType:
                        getInstance().serviceType = ServiceType.valueOf(getCharacters());
                        break;
                    case serviceId:
                        getInstance().serviceId = ServiceId.valueOf(getCharacters());
                        break;
                    case SCPDURL:
                        getInstance().descriptorURI = parseURI(getCharacters());
                        break;
                    case controlURL:
                        getInstance().controlURI = parseURI(getCharacters());
                        break;
                    case eventSubURL:
                        getInstance().eventSubscriptionURI = parseURI(getCharacters());
                        break;
                }
            } catch (InvalidValueException ex) {
                SpecificationViolationReporter.report("Skipping invalid service declaration. " + ex.getMessage(), null);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceListHandler extends DeviceDescriptorHandler<List<MutableDevice>> {

        public static final ELEMENT EL = ELEMENT.deviceList;

        public DeviceListHandler(List<MutableDevice> instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(DeviceHandler.EL)) {
                MutableDevice device = new MutableDevice();
                getInstance().add(device);
                new DeviceHandler(device, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class DeviceDescriptorHandler<I> extends SAXParser.Handler<I> {

        public DeviceDescriptorHandler(I instance) {
            super(instance);
        }

        public DeviceDescriptorHandler(I instance, SAXParser parser) {
            super(instance, parser);
        }

        public DeviceDescriptorHandler(I instance, DeviceDescriptorHandler parent) {
            super(instance, parent);
        }

        public DeviceDescriptorHandler(I instance, SAXParser parser, DeviceDescriptorHandler parent) {
            super(instance, parser, parent);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null)
                return;
            startElement(el, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null)
                return;
            endElement(el);
        }

        @Override
        protected boolean isLastElement(String uri, String localName, String qName) {
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            return el != null && isLastElement(el);
        }

        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
        }

        public void endElement(ELEMENT element) throws SAXException {
        }

        public boolean isLastElement(ELEMENT element) {
            return false;
        }
    }
}
