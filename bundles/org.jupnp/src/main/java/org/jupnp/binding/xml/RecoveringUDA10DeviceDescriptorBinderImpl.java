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
package org.jupnp.binding.xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.util.SpecificationViolationReporter;
import org.jupnp.xml.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

/**
 * @author Michael Pujos
 * @author Kai Kreuzer - added faulty descriptors as found by Belkin WeMo
 * @author Roland Edelhoff - avoid description of Sonos group devices
 * @author Jochen Hiller - use SpecificationViolationReporter, change logger to be final
 */
public class RecoveringUDA10DeviceDescriptorBinderImpl extends UDA10DeviceDescriptorBinderImpl {

    private final Logger logger = LoggerFactory.getLogger(RecoveringUDA10DeviceDescriptorBinderImpl.class);

    @Override
    public <D extends Device> D describe(D undescribedDevice, String descriptorXml)
            throws DescriptorBindingException, ValidationException {

        D device = null;
        DescriptorBindingException originalException = null;
        try {
            try {
                if (descriptorXml != null) {
                    descriptorXml = descriptorXml.trim(); // Always trim whitespace
                }
                String fixedXml = fixMimeTypes(descriptorXml);
                fixedXml = fixWrongNamespaces(fixedXml);
                fixedXml = fixWemoMakerUDN(fixedXml);
                device = super.describe(undescribedDevice, fixedXml);

                // Ignore Sonos group device since they have the same UDN as the corresponding player device
                // and contains useless device details and services. The group device will be announced first
                // after pairing the player to Sonos and therefore it will be stored in the registry instead of the
                // player.
                if (isSonosGroupDevice(device)) {
                    throw new IllegalArgumentException("Ignore Sonos group devices due to invalid descriptor content.");
                }
                return device;

            } catch (DescriptorBindingException e) {
                logger.warn("Regular parsing failed", e);
                originalException = e;
            } catch (IllegalArgumentException e) {
                handleInvalidDescriptor(descriptorXml, new DescriptorBindingException(e.getMessage()));
            }

            String fixedXml;
            // The following modifications are not cumulative!

            fixedXml = fixGarbageLeadingChars(descriptorXml);
            if (fixedXml != null) {
                try {
                    device = super.describe(undescribedDevice, fixedXml);
                    return device;
                } catch (DescriptorBindingException e) {
                    logger.warn("Removing leading garbage didn't work", e);
                }
            }

            fixedXml = fixGarbageTrailingChars(descriptorXml, originalException);
            if (fixedXml != null) {
                try {
                    device = super.describe(undescribedDevice, fixedXml);
                    return device;
                } catch (DescriptorBindingException e) {
                    logger.warn("Removing trailing garbage didn't work", e);
                }
            }

            // Try to fix "up to five" missing namespace declarations
            DescriptorBindingException lastException = originalException;
            fixedXml = descriptorXml;
            for (int retryCount = 0; retryCount < 5; retryCount++) {
                fixedXml = fixMissingNamespaces(fixedXml, lastException);
                if (fixedXml != null) {
                    try {
                        device = super.describe(undescribedDevice, fixedXml);
                        return device;
                    } catch (DescriptorBindingException e) {
                        logger.warn("Fixing namespace prefix didn't work", e);
                        lastException = e;
                    }
                } else {
                    break; // We can stop, no more namespace fixing can be done
                }
            }

            handleInvalidDescriptor(descriptorXml, originalException);

        } catch (ValidationException e) {
            device = handleInvalidDevice(descriptorXml, device, e);
            if (device != null) {
                return device;
            }
        }
        throw new IllegalStateException("No device produced, did you swallow exceptions in your subclass?");
    }

    private String fixGarbageLeadingChars(String descriptorXml) {
        /*
         * Recover this:
         *
         * HTTP/1.1 200 OK
         * Content-Length: 4268
         * Content-Type: text/xml; charset="utf-8"
         * Server: Microsoft-Windows/6.2 UPnP/1.0 UPnP-Device-Host/1.0 Microsoft-HTTPAPI/2.0
         * Date: Sun, 07 Apr 2013 02:11:30 GMT
         *
         * @7:5 in java.io.StringReader@407f6b00) : HTTP/1.1 200 OK
         * Content-Length: 4268
         * Content-Type: text/xml; charset="utf-8"
         * Server: Microsoft-Windows/6.2 UPnP/1.0 UPnP-Device-Host/1.0 Microsoft-HTTPAPI/2.0
         * Date: Sun, 07 Apr 2013 02:11:30 GMT
         *
         * <?xml version="1.0"?>...
         */

        int index = descriptorXml.indexOf("<?xml");
        if (index == -1) {
            return descriptorXml;
        }
        return descriptorXml.substring(index);
    }

    protected String fixGarbageTrailingChars(String descriptorXml, DescriptorBindingException e) {
        int index = descriptorXml.indexOf("</root>");
        if (index == -1) {
            SpecificationViolationReporter.report("No closing </root> element in descriptor");
            return null;
        }
        if (descriptorXml.length() != index + "</root>".length()) {
            SpecificationViolationReporter.report("Detected garbage characters after <root> node, removing");
            return descriptorXml.substring(0, index) + "</root>";
        }
        return null;
    }

    protected String fixMimeTypes(String descriptorXml) {
        if (descriptorXml.contains("<mimetype>jpg</mimetype>")) {
            SpecificationViolationReporter.report("Detected invalid mimetype 'jpg', replacing it with 'image/jpeg'");
            return descriptorXml.replaceAll("<mimetype>jpg</mimetype>", "<mimetype>image/jpeg</mimetype>");
        }
        return descriptorXml;
    }

    protected String fixWrongNamespaces(String descriptorXml) {
        if (descriptorXml.contains("<root xmlns=\"urn:Belkin:device-1-0\">")) {
            SpecificationViolationReporter
                    .report("Detected invalid root namespace 'urn:Belkin', replacing it with 'urn:schemas-upnp-org'");
            return descriptorXml.replaceAll("<root xmlns=\"urn:Belkin:device-1-0\">",
                    "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">");
        }
        return descriptorXml;
    }

    protected String fixMissingNamespaces(String descriptorXml, DescriptorBindingException e) {
        // Windows: org.jupnp.binding.xml.DescriptorBindingException: Could not parse device descriptor:
        // org.jupnp.xml.ParserException: org.xml.sax.SAXParseException: The prefix "dlna" for element "dlna:X_DLNADOC"
        // is not bound.
        // Android: org.xmlpull.v1.XmlPullParserException: undefined prefix: dlna (position:START_TAG
        // <{null}dlna:X_DLNADOC>@19:17 in java.io.StringReader@406dff48)

        // We can only handle certain exceptions, depending on their type and message
        Throwable cause = e.getCause();
        if (!(cause instanceof SAXParseException || cause instanceof ParserException)) {
            return null;
        }
        String message = cause.getMessage();
        if (message == null) {
            return null;
        }

        Pattern pattern = Pattern.compile("The prefix \"(.*)\" for element"); // Windows
        Matcher matcher = pattern.matcher(message);
        if (!matcher.find() || matcher.groupCount() != 1) {
            pattern = Pattern.compile("undefined prefix: ([^ ]*)"); // Android
            matcher = pattern.matcher(message);
            if (!matcher.find() || matcher.groupCount() != 1) {
                return null;
            }
        }

        String missingNS = matcher.group(1);
        SpecificationViolationReporter.report("Fixing missing namespace declaration for: {}", missingNS);

        // Extract <root> attributes
        pattern = Pattern.compile("<root([^>]*)");
        matcher = pattern.matcher(descriptorXml);
        if (!matcher.find() || matcher.groupCount() != 1) {
            logger.trace("Could not find <root> element attributes");
            return null;
        }

        String rootAttributes = matcher.group(1);
        logger.trace("Preserving existing <root> element attributes/namespace declarations: {}", matcher.group(0));

        // Extract <root> body
        pattern = Pattern.compile("<root[^>]*>(.*)</root>", Pattern.DOTALL);
        matcher = pattern.matcher(descriptorXml);
        if (!matcher.find() || matcher.groupCount() != 1) {
            logger.trace("Could not extract body of <root> element");
            return null;
        }

        String rootBody = matcher.group(1);

        // Add missing namespace, it only matters that it is defined, not that it is correct
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + "<root "
                + String.format("xmlns:%s=\"urn:schemas-dlna-org:device-1-0\"", missingNS) + rootAttributes + ">"
                + rootBody + "</root>";

        // TODO: Should we match different undeclared prefixes with their correct namespace?
        // So if it's "dlna" we use "urn:schemas-dlna-org:device-1-0" etc.
    }

    // Belkin WeMo Maker contains illegal strings in UDN values
    protected String fixWemoMakerUDN(String descriptorXml) {
        if (descriptorXml.contains(":sensor:switch")) {
            SpecificationViolationReporter.report("Detected invalid UDN value ':sensor:switch', replacing it");
            descriptorXml = descriptorXml.replaceAll(":sensor:switch", "");
            return descriptorXml.replaceAll(":sensor:switch", "");
        }
        return descriptorXml;
    }

    /**
     * Handle processing errors while reading XML descriptors.
     * <p/>
     * <p>
     * Typically you want to log this problem or create an error report, and in any
     * case, throw a {@link DescriptorBindingException} to notify the caller of the
     * binder of this failure. The default implementation simply rethrows the
     * given exception.
     * </p>
     *
     * @param xml The original XML causing the parsing failure.
     * @param exception The original exception while parsing the XML.
     */
    protected void handleInvalidDescriptor(String xml, DescriptorBindingException exception)
            throws DescriptorBindingException {
        throw exception;
    }

    /**
     * Handle processing errors while binding XML descriptors.
     * <p/>
     * <p>
     * Typically you want to log this problem or create an error report. You
     * should throw a {@link ValidationException} to notify the caller of the
     * binder of failure. The default implementation simply rethrows the
     * given exception.
     * </p>
     * <p>
     * This method gives you a final chance to fix the problem, instead of
     * throwing an exception, you could try to create valid {@link Device}
     * model and return it.
     * </p>
     *
     * @param xml The original XML causing the binding failure.
     * @param device The unfinished {@link Device} that failed validation
     * @param exception The errors found when validating the {@link Device} model.
     * @return Device A "fixed" {@link Device} model, instead of throwing an exception.
     */
    protected <D extends Device> D handleInvalidDevice(String xml, D device, ValidationException exception)
            throws ValidationException {
        throw exception;
    }

    private <D extends Device> boolean isSonosGroupDevice(D device) {
        if (device instanceof RemoteDevice) {
            RemoteDevice rd = (RemoteDevice) device;
            return isGroupInformationAvailable(rd)
                    && rd.getDetails().getManufacturerDetails().getManufacturer().toLowerCase().contains("sonos")
                    && rd.getType().toString().contains("urn:smartspeaker-audio:device:SpeakerGroup")
                    && rd.getIdentity().getDescriptorURL().toString().endsWith("group_description.xml");
        }
        return false;
    }

    private boolean isGroupInformationAvailable(RemoteDevice rd) {
        return rd != null && rd.getType() != null && rd.getIdentity() != null && rd.getDetails() != null
                && rd.getDetails().getManufacturerDetails() != null
                && rd.getDetails().getManufacturerDetails().getManufacturer() != null
                && rd.getIdentity().getDescriptorURL() != null;
    }
}
