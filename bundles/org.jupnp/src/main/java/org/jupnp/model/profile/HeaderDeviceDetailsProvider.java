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
package org.jupnp.model.profile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jupnp.model.meta.DeviceDetails;

/**
 * Selects device details based on a regex and the client's HTTP headers.
 * <p>
 * This provider will lookup and match a {@link DeviceDetails} entry in a
 * given map that is keyed by HTTP header name and a regular expression pattern.
 * If the control point sent an HTTP header that matches an entry's name,
 * and the value of the control points header matches the pattern of the entry,
 * the value of the entry is applied. This is a case-insensitive pattern match.
 * </p>
 *
 * @author Mario Franco
 * @author Christian Bauer
 */
public class HeaderDeviceDetailsProvider implements DeviceDetailsProvider {

    public static class Key {

        final String headerName;
        final String valuePattern;
        final Pattern pattern;

        public Key(String headerName, String valuePattern) {
            this.headerName = headerName;
            this.valuePattern = valuePattern;
            this.pattern = Pattern.compile(valuePattern, Pattern.CASE_INSENSITIVE);
        }

        public String getHeaderName() {
            return headerName;
        }

        public String getValuePattern() {
            return valuePattern;
        }

        public boolean isValuePatternMatch(String value) {
            return pattern.matcher(value).matches();
        }
    }

    private final DeviceDetails defaultDeviceDetails;
    private final Map<Key, DeviceDetails> headerDetails;

    public HeaderDeviceDetailsProvider(DeviceDetails defaultDeviceDetails) {
        this(defaultDeviceDetails, null);
    }

    public HeaderDeviceDetailsProvider(DeviceDetails defaultDeviceDetails, Map<Key, DeviceDetails> headerDetails) {
        this.defaultDeviceDetails = defaultDeviceDetails;
        this.headerDetails = headerDetails != null ? headerDetails : new HashMap<>();
    }

    public DeviceDetails getDefaultDeviceDetails() {
        return defaultDeviceDetails;
    }

    public Map<Key, DeviceDetails> getHeaderDetails() {
        return headerDetails;
    }

    @Override
    public DeviceDetails provide(RemoteClientInfo info) {
        if (info == null || info.getRequestHeaders().isEmpty()) {
            return getDefaultDeviceDetails();
        }

        for (Key key : getHeaderDetails().keySet()) {
            List<String> headerValues;
            if ((headerValues = info.getRequestHeaders().get(key.getHeaderName())) == null) {
                continue;
            }
            for (String headerValue : headerValues) {
                if (key.isValuePatternMatch(headerValue)) {
                    return getHeaderDetails().get(key);
                }
            }
        }
        return getDefaultDeviceDetails();
    }
}
