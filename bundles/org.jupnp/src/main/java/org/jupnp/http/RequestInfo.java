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
package org.jupnp.http;

/**
 * {@code String}-based device-detection helpers used by {@link org.jupnp.model.profile.RemoteClientInfo},
 * independent of any particular transport implementation. The {@code javax.servlet.http.HttpServletRequest}-based
 * counterparts and debug-logging utilities that used to live here moved to
 * {@code org.jupnp.transport.impl.servlet.ServletRequestInfo} in the optional
 * {@code org.jupnp.transport.servlet} bundle (not {@code org.jupnp.http}: that package name is already taken by
 * this class, and OSGi doesn't support one package split across two bundles' exports). Nothing in core needs
 * them, and keeping them here would have made core depend on javax.servlet for no reason.
 *
 * @author Christian Bauer
 * @author Michael Pujos
 */
public class RequestInfo {

    public static boolean isPS3Request(String userAgent, String avClientInfo) {
        return ((userAgent != null && userAgent.contains("PLAYSTATION 3"))
                || (avClientInfo != null && avClientInfo.contains("PLAYSTATION 3")));
    }

    public static boolean isAndroidBubbleUPnPRequest(String userAgent) {
        return (userAgent != null && userAgent.contains("BubbleUPnP"));
    }

    public static boolean isJRiverRequest(String userAgent) {
        return userAgent != null && (userAgent.contains("J-River") || userAgent.contains("J. River"));
    }

    public static boolean isWMPRequest(String userAgent) {
        return userAgent != null && userAgent.contains("Windows-Media-Player") && !isJRiverRequest(userAgent);
    }

    public static boolean isXbox360Request(String userAgent, String server) {
        return (userAgent != null && (userAgent.contains("Xbox") || userAgent.contains("Xenon")))
                || (server != null && server.contains("Xbox"));
    }
}
