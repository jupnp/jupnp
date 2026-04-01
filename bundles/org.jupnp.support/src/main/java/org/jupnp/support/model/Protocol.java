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
package org.jupnp.support.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public enum Protocol {

    ALL(ProtocolInfo.WILDCARD),
    HTTP_GET("http-get"),
    RTSP_RTP_UDP("rtsp-rtp-udp"),
    INTERNAL("internal"),
    IEC61883("iec61883"),
    XBMC_GET("xbmc-get"),
    OTHER("other");

    private static final Logger LOGGER = LoggerFactory.getLogger(Protocol.class);

    private final String protocolString;

    Protocol(String protocolString) {
        this.protocolString = protocolString;
    }

    @Override
    public String toString() {
        return protocolString;
    }

    public static Protocol value(String s) {
        for (Protocol protocol : values()) {
            if (protocol.toString().equals(s)) {
                return protocol;
            }
        }
        LOGGER.info("Unsupported OTHER protocol string: {}", s);
        return OTHER;
    }
}
