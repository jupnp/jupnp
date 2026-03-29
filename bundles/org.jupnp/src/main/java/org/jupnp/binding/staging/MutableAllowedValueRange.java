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
package org.jupnp.binding.staging;

/**
 * @author Christian Bauer
 */
public class MutableAllowedValueRange {

    // TODO: UPNP VIOLATION: Some devices (Netgear Router again...) send empty elements, so use some sane defaults
    // TODO: UPNP VIOLATION: The WANCommonInterfaceConfig example XML is even wrong, it does not include a <maximum>
    // element!
    public Long minimum = 0L;
    public Long maximum = Long.MAX_VALUE;
    public Long step = 1L;
}
