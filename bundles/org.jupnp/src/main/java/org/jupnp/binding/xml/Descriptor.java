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

import org.w3c.dom.Node;

/**
 * Utility interface with static declarations of all "strings".
 *
 * @author Christian Bauer
 */
public abstract class Descriptor {

    public interface Device {

        String NAMESPACE_URI = "urn:schemas-upnp-org:device-1-0";
        String DLNA_NAMESPACE_URI = "urn:schemas-dlna-org:device-1-0";
        String DLNA_PREFIX = "dlna";
        String SEC_NAMESPACE_URI = "http://www.sec.co.kr/dlna";
        String SEC_PREFIX = "sec";

        enum ELEMENT {
            root,
            specVersion,
            major,
            minor,
            URLBase,
            device,
            UDN,
            X_DLNADOC,
            X_DLNACAP,
            ProductCap,
            X_ProductCap,
            deviceType,
            friendlyName,
            manufacturer,
            manufacturerURL,
            modelDescription,
            modelName,
            modelNumber,
            modelURL,
            presentationURL,
            UPC,
            serialNumber,
            iconList,
            icon,
            width,
            height,
            depth,
            url,
            mimetype,
            serviceList,
            service,
            serviceType,
            serviceId,
            SCPDURL,
            controlURL,
            eventSubURL,
            deviceList;

            public static ELEMENT valueOrNullOf(String s) {
                try {
                    return valueOf(s);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }

            public boolean equals(Node node) {
                return toString().equals(node.getLocalName());
            }
        }
    }

    public interface Service {

        String NAMESPACE_URI = "urn:schemas-upnp-org:service-1-0";

        enum ELEMENT {
            scpd,
            specVersion,
            major,
            minor,
            actionList,
            action,
            name,
            argumentList,
            argument,
            direction,
            relatedStateVariable,
            retval,
            serviceStateTable,
            stateVariable,
            dataType,
            defaultValue,
            allowedValueList,
            allowedValue,
            allowedValueRange,
            minimum,
            maximum,
            step;

            public static ELEMENT valueOrNullOf(String s) {
                try {
                    return valueOf(s);
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }

            public boolean equals(Node node) {
                return toString().equals(node.getLocalName());
            }
        }

        enum ATTRIBUTE {
            sendEvents
        }
    }
}
