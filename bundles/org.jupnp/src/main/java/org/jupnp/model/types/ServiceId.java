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
package org.jupnp.model.types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jupnp.model.Constants;
import org.jupnp.util.SpecificationViolationReporter;

/**
 * Represents a service identifier, for example <code>urn:my-domain-namespace:serviceId:MyService123</code>
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public class ServiceId {

    public static final String UNKNOWN = "UNKNOWN";

    public static final Pattern PATTERN = Pattern
            .compile("urn:(" + Constants.REGEX_NAMESPACE + "):serviceId:(" + Constants.REGEX_ID + ")");

    // Note: 'service' vs. 'serviceId'
    public static final Pattern BROKEN_PATTERN = Pattern
            .compile("urn:(" + Constants.REGEX_NAMESPACE + "):service:(" + Constants.REGEX_ID + ")");

    public static final Pattern UPNP_VIOLATION = Pattern.compile("urn:(" + Constants.REGEX_NAMESPACE + "):serviceId:");

    private String namespace;
    private String id;

    public ServiceId(String namespace, String id) {
        if (namespace != null && !namespace.matches(Constants.REGEX_NAMESPACE)) {
            throw new IllegalArgumentException("Service ID namespace contains illegal characters");
        }
        this.namespace = namespace;

        if (id != null && !id.matches(Constants.REGEX_ID)) {
            throw new IllegalArgumentException("Service ID suffix too long (64) or contains illegal characters");
        }
        this.id = id;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getId() {
        return id;
    }

    public static ServiceId valueOf(String s) throws InvalidValueException {
        ServiceId serviceId = null;

        // First try UDAServiceId parse
        try {
            serviceId = UDAServiceId.valueOf(s);
        } catch (Exception e) {
            // Ignore
        }

        if (serviceId != null) {
            return serviceId;
        }

        // Now try a generic ServiceId parse
        Matcher matcher = ServiceId.PATTERN.matcher(s);
        if (matcher.matches() && matcher.groupCount() >= 2) {
            return new ServiceId(matcher.group(1), matcher.group(2));
        }

        matcher = ServiceId.BROKEN_PATTERN.matcher(s);
        if (matcher.matches() && matcher.groupCount() >= 2) {
            return new ServiceId(matcher.group(1), matcher.group(2));
        }

        // TODO: UPNP VIOLATION: Kodak Media Server doesn't provide any service ID token
        // urn:upnp-org:serviceId:
        matcher = ServiceId.UPNP_VIOLATION.matcher(s);
        if (matcher.matches() && matcher.groupCount() >= 1) {
            SpecificationViolationReporter.report("No service ID token, defaulting to {}: {}", UNKNOWN, s);
            return new ServiceId(matcher.group(1), UNKNOWN);
        }

        // TODO: UPNP VIOLATION: PS Audio Bridge has invalid service IDs
        String[] tokens = s.split(":");
        if (tokens.length == 4) {
            SpecificationViolationReporter.report("Trying a simple colon-split of: {}", s);
            return new ServiceId(tokens[1], tokens[3]);
        }

        throw new InvalidValueException("Can't parse service ID string (namespace/id): " + s);
    }

    @Override
    public String toString() {
        return "urn:" + getNamespace() + ":serviceId:" + getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof ServiceId)) {
            return false;
        }

        ServiceId serviceId = (ServiceId) o;

        if (!id.equals(serviceId.id)) {
            return false;
        }
        if (!namespace.equals(serviceId.namespace)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }
}
