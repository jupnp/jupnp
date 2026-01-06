/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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
package org.jupnp.model.message.header;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms known and standardized UPnP/HTTP headers from/to string representation.
 * <p>
 * The {@link #newInstance(org.jupnp.model.message.header.UpnpHeader.Type, String)} method
 * attempts to instantiate the best header subtype for a given header (name) and string value.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class UpnpHeader<T> {

    /**
     * Maps a standardized UPnP header to potential header subtypes.
     */
    public enum Type {

        USN("USN", new LinkedHashMap<>() {
            {
                put(USNRootDeviceHeader.class, USNRootDeviceHeader::new);
                put(DeviceUSNHeader.class, DeviceUSNHeader::new);
                put(ServiceUSNHeader.class, ServiceUSNHeader::new);
                put(UDNHeader.class, UDNHeader::new);
            }
        }),
        NT("NT", new LinkedHashMap<>() {
            {
                put(RootDeviceHeader.class, RootDeviceHeader::new);
                put(UDADeviceTypeHeader.class, UDADeviceTypeHeader::new);
                put(UDAServiceTypeHeader.class, UDAServiceTypeHeader::new);
                put(DeviceTypeHeader.class, DeviceTypeHeader::new);
                put(ServiceTypeHeader.class, ServiceTypeHeader::new);
                put(UDNHeader.class, UDNHeader::new);
                put(NTEventHeader.class, NTEventHeader::new);
            }
        }),
        NTS("NTS", Map.of(NTSHeader.class, NTSHeader::new)),
        HOST("HOST", Map.of(HostHeader.class, HostHeader::new)),
        SERVER("SERVER", Map.of(ServerHeader.class, ServerHeader::new)),
        LOCATION("LOCATION", Map.of(LocationHeader.class, LocationHeader::new)),
        MAX_AGE("CACHE-CONTROL", Map.of(MaxAgeHeader.class, MaxAgeHeader::new)),
        USER_AGENT("USER-AGENT", Map.of(UserAgentHeader.class, UserAgentHeader::new)),
        CONTENT_TYPE("CONTENT-TYPE", Map.of(ContentTypeHeader.class, ContentTypeHeader::new)),
        MAN("MAN", Map.of(MANHeader.class, MANHeader::new)),
        MX("MX", Map.of(MXHeader.class, MXHeader::new)),
        ST("ST", new LinkedHashMap<>() {
            {
                put(STAllHeader.class, STAllHeader::new);
                put(RootDeviceHeader.class, RootDeviceHeader::new);
                put(UDADeviceTypeHeader.class, UDADeviceTypeHeader::new);
                put(UDAServiceTypeHeader.class, UDAServiceTypeHeader::new);
                put(DeviceTypeHeader.class, DeviceTypeHeader::new);
                put(ServiceTypeHeader.class, ServiceTypeHeader::new);
                put(UDNHeader.class, UDNHeader::new);
            }
        }),
        EXT("EXT", Map.of(EXTHeader.class, EXTHeader::new)),
        SOAPACTION("SOAPACTION", Map.of(SoapActionHeader.class, SoapActionHeader::new)),
        TIMEOUT("TIMEOUT", Map.of(TimeoutHeader.class, TimeoutHeader::new)),
        CALLBACK("CALLBACK", Map.of(CallbackHeader.class, CallbackHeader::new)),
        SID("SID", Map.of(SubscriptionIdHeader.class, SubscriptionIdHeader::new)),
        SEQ("SEQ", Map.of(EventSequenceHeader.class, EventSequenceHeader::new)),
        RANGE("RANGE", Map.of(RangeHeader.class, RangeHeader::new)),
        CONTENT_RANGE("CONTENT-RANGE", Map.of(ContentRangeHeader.class, ContentRangeHeader::new)),
        PRAGMA("PRAGMA", Map.of(PragmaHeader.class, PragmaHeader::new)),
        EXT_IFACE_MAC("X-CLING-IFACE-MAC", Map.of(InterfaceMacHeader.class, InterfaceMacHeader::new)),
        EXT_AV_CLIENT_INFO("X-AV-CLIENT-INFO", Map.of(AVClientInfoHeader.class, AVClientInfoHeader::new));

        private static final Map<String, Type> byName = new HashMap<>() {
            {
                for (Type t : Type.values()) {
                    put(t.getHttpName(), t);
                }
            }
        };

        private final String httpName;
        private final Map<Class<? extends UpnpHeader>, Supplier<? extends UpnpHeader>> headerTypes;

        Type(String httpName, Map<Class<? extends UpnpHeader>, Supplier<? extends UpnpHeader>> headerClass) {
            this.httpName = httpName;
            this.headerTypes = headerClass;
        }

        public String getHttpName() {
            return httpName;
        }

        public Map<Class<? extends UpnpHeader>, Supplier<? extends UpnpHeader>> getHeaderTypes() {
            return headerTypes;
        }

        public boolean isValidHeaderType(Class<? extends UpnpHeader> clazz) {
            if (headerTypes.containsKey(clazz)) {
                return true;
            }

            for (Class<? extends UpnpHeader> permissibleType : headerTypes.keySet()) {
                if (permissibleType.isAssignableFrom(clazz)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @param httpName A case-insensitive HTTP header name.
         */
        public static Type getByHttpName(String httpName) {
            if (httpName == null) {
                return null;
            }
            return byName.get(httpName.toUpperCase(Locale.ENGLISH));
        }
    }

    private T value;

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    /**
     * @param s This header's value as a string representation.
     * @throws InvalidHeaderException If the value is invalid for this UPnP header.
     */
    public abstract void setString(String s) throws InvalidHeaderException;

    /**
     * @return A string representing this header's value.
     */
    public abstract String getString();

    /**
     * Create a new instance of a {@link UpnpHeader} subtype that matches the given type and value.
     * <p>
     * This method iterates through all potential header subtype classes as declared in {@link Type}.
     * It creates a new instance of the subtype class and calls its {@link #setString(String)} method.
     * If no {@link org.jupnp.model.message.header.InvalidHeaderException} is thrown, the subtype
     * instance is returned.
     * </p>
     *
     * @param type The type (or name) of the header.
     * @param headerValue The value of the header.
     * @return The best matching header subtype instance, or <code>null</code> if no subtype can be found.
     */
    public static UpnpHeader newInstance(UpnpHeader.Type type, String headerValue) {
        final Logger logger = LoggerFactory.getLogger(UpnpHeader.class);

        // Try all the UPnP headers and see if one matches our value parsers
        Map<Class<? extends UpnpHeader>, Supplier<? extends UpnpHeader>> headerTypes = type.getHeaderTypes();
        UpnpHeader upnpHeader = null;

        for (Map.Entry<Class<? extends UpnpHeader>, Supplier<? extends UpnpHeader>> headerType : headerTypes
                .entrySet()) {
            Class<? extends UpnpHeader> headerClass = headerType.getKey();
            Supplier<? extends UpnpHeader> factory = headerType.getValue();
            try {
                logger.trace("Trying to parse '{}' with class: {}", type, headerClass.getSimpleName());
                upnpHeader = factory.get();
                if (headerValue != null) {
                    upnpHeader.setString(headerValue);
                }
                break;
            } catch (InvalidHeaderException e) {
                logger.trace("Invalid header value for tested type: {} - {}", headerClass.getSimpleName(),
                        e.getMessage());
                upnpHeader = null;
            }

        }
        return upnpHeader;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") '" + getValue() + "'";
    }
}
