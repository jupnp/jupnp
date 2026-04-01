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
package org.jupnp.model;

/**
 * Shared and immutable settings.
 *
 * @author Christian Bauer
 */
public interface Constants {

    String SYSTEM_PROPERTY_ANNOUNCE_MAC_ADDRESS = "org.jupnp.network.announceMACAddress";

    int UPNP_MULTICAST_PORT = 1900;

    // this is the port to which search responses should be sent; 0 means an ephemeral port is used
    int UPNP_MULTICAST_RESPONSE_PORT = 0;

    String IPV4_UPNP_MULTICAST_GROUP = "239.255.255.250";

    String IPV6_UPNP_LINK_LOCAL_ADDRESS = "FF02::C";
    String IPV6_UPNP_SUBNET_ADDRESS = "FF03::C";
    String IPV6_UPNP_ADMINISTRATIVE_ADDRESS = "FF04::C";
    String IPV6_UPNP_SITE_LOCAL_ADDRESS = "FF05::C";
    String IPV6_UPNP_GLOBAL_ADDRESS = "FF0E::C";

    int MIN_ADVERTISEMENT_AGE_SECONDS = 1800;

    // Parsing rules for: deviceType, serviceType, serviceId (UDA 1.0, section 2.5)

    // TODO: UPNP VIOLATION: Microsoft Windows Media Player Sharing 4.0, X_MS_MediaReceiverRegistrar service has type
    // with periods instead of hyphens in the namespace!
    // UDA 1.0 spec: "Period characters in the vendor domain name MUST be replaced with hyphens in accordance with RFC
    // 2141"
    // TODO: UPNP VIOLATION: Azureus/Vuze 4.2.0.2 sends a URN as a service identifier, so we need to match colons!
    // TODO: UPNP VIOLATION: Intel UPnP Tools send dots in the service identifier suffix, match that...

    String REGEX_NAMESPACE = "[a-zA-Z0-9\\-\\.]+";
    String REGEX_TYPE = "[a-zA-Z_0-9\\-]{1,64}";
    String REGEX_ID = "[a-zA-Z_0-9\\-:\\.]{1,64}";

    /*
     * Must not contain a hyphen character (-, 2D Hex in UTF- 8). First character must be a USASCII letter (A-Z, a-z),
     * USASCII digit (0-9), an underscore ("_"), or a non-experimental Unicode letter or digit greater than U+007F.
     * Succeeding characters must be a USASCII letter (A-Z, a-z), USASCII digit (0-9), an underscore ("_"), a
     * period ("."), a Unicode combiningchar, an extender, or a non-experimental Unicode letter or digit greater
     * than U+007F. The first three letters must not be "XML" in any combination of case. Case sensitive.
     */
    // TODO: I have no idea how to match or what even is a "unicode extender character", neither does the Unicode book
    String REGEX_UDA_NAME = "[a-zA-Z0-9^-_\\p{L}\\p{N}]{1}[a-zA-Z0-9^-_\\.\\\\p{L}\\\\p{N}\\p{Mc}\\p{Sk}]*";

    // Random patentable "inventions" by MSFT
    String SOAP_NS_ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
    String SOAP_URI_ENCODING_STYLE = "http://schemas.xmlsoap.org/soap/encoding/";
    String NS_UPNP_CONTROL_10 = "urn:schemas-upnp-org:control-1-0";
    String NS_UPNP_EVENT_10 = "urn:schemas-upnp-org:event-1-0";

    // State variable prefixes
    String ARG_TYPE_PREFIX = "A_ARG_TYPE_";
}
