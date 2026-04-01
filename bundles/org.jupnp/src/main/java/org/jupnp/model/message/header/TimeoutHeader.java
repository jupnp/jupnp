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
package org.jupnp.model.message.header;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jupnp.model.UserConstants;

/**
 * @author Christian Bauer
 */
public class TimeoutHeader extends UpnpHeader<Integer> {

    // It's probably OK to assume that "infinite" means 4000 years?
    public static final Integer INFINITE_VALUE = Integer.MAX_VALUE;

    public static final Pattern PATTERN = Pattern.compile("Second-(?:([0-9]+)|infinite)");

    /**
     * Defaults to {@link org.jupnp.model.UserConstants#DEFAULT_SUBSCRIPTION_DURATION_SECONDS}.
     */
    public TimeoutHeader() {
        setValue(UserConstants.DEFAULT_SUBSCRIPTION_DURATION_SECONDS);
    }

    public TimeoutHeader(int timeoutSeconds) {
        setValue(timeoutSeconds);
    }

    public TimeoutHeader(Integer timeoutSeconds) {
        setValue(timeoutSeconds);
    }

    @Override
    public void setString(String s) throws InvalidHeaderException {

        Matcher matcher = PATTERN.matcher(s);
        if (!matcher.matches()) {
            throw new InvalidHeaderException("Can't parse timeout seconds integer from: " + s);
        }

        if (matcher.group(1) != null) {
            setValue(Integer.parseInt(matcher.group(1)));
        } else {
            setValue(INFINITE_VALUE);
        }
    }

    @Override
    public String getString() {
        return "Second-" + (getValue().equals(INFINITE_VALUE) ? "infinite" : getValue());
    }
}
