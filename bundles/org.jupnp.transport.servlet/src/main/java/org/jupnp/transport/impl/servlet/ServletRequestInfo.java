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
package org.jupnp.transport.impl.servlet;

import java.util.Enumeration;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.jupnp.http.RequestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HttpServletRequest}-based counterparts of {@link RequestInfo}'s device-detection helpers, plus debug
 * logging utilities for servlet-based transports. Split out from {@link RequestInfo} (which stays in core, and
 * has no servlet dependency) because {@link org.jupnp.model.profile.RemoteClientInfo} only ever needs the
 * {@code String}-based overloads there -- core has no reason to depend on {@code javax.servlet} for these.
 *
 * @author Christian Bauer
 * @author Michael Pujos
 */
public class ServletRequestInfo {

    public static void reportRequest(StringBuilder builder, HttpServletRequest req) {
        builder.append("Request: ");
        builder.append(req.getMethod());
        builder.append(' ');
        builder.append(req.getRequestURL());
        String queryString = req.getQueryString();
        if (queryString != null) {
            builder.append('?');
            builder.append(queryString);
        }

        builder.append(" - ");

        String sessionId = req.getRequestedSessionId();
        if (sessionId != null) {
            builder.append("\nSession ID: ");
        }
        if (sessionId == null) {
            builder.append("No Session");
        } else if (req.isRequestedSessionIdValid()) {
            builder.append(sessionId);
            builder.append(" (from ");
            if (req.isRequestedSessionIdFromCookie()) {
                builder.append("cookie)\n");
            } else if (req.isRequestedSessionIdFromURL()) {
                builder.append("url)\n");
            } else {
                builder.append("unknown)\n");
            }
        } else {
            builder.append("Invalid Session ID\n");
        }
    }

    public static void reportParameters(StringBuilder builder, HttpServletRequest req) {
        Enumeration<String> names = req.getParameterNames();
        if (names == null) {
            return;
        }

        if (names.hasMoreElements()) {
            builder.append("Parameters:\n");
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String[] values = req.getParameterValues(name);
                if (values != null) {
                    for (String value : values) {
                        builder.append("    ").append(name).append(" = ").append(value).append('\n');
                    }
                }
            }
        }
    }

    public static void reportHeaders(StringBuilder builder, HttpServletRequest req) {
        Enumeration<String> names = req.getHeaderNames();
        if (names == null) {
            return;
        }
        if (names.hasMoreElements()) {
            builder.append("Headers:\n");
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                String value = req.getHeader(name);
                builder.append("    ").append(name).append(": ").append(value).append('\n');
            }
        }
    }

    public static void reportCookies(StringBuilder builder, HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return;
        }
        int l = cookies.length;
        if (l > 0) {
            builder.append("Cookies:\n");
            for (Cookie cookie : cookies) {
                builder.append("    ").append(cookie.getName()).append(" = ").append(cookie.getValue()).append('\n');
            }
        }
    }

    public static void reportClient(StringBuilder builder, HttpServletRequest req) {
        builder.append("Remote Address: ").append(req.getRemoteAddr()).append("\n");
        if (!req.getRemoteAddr().equals(req.getRemoteHost())) {
            builder.append("Remote Host: ").append(req.getRemoteHost()).append("\n");
        }
        builder.append("Remote Port: ").append(req.getRemotePort()).append("\n");
        if (req.getRemoteUser() != null) {
            builder.append("Remote User: ").append(req.getRemoteUser()).append("\n");
        }
    }

    public static boolean isPS3Request(HttpServletRequest request) {
        return RequestInfo.isPS3Request(request.getHeader("User-Agent"), request.getHeader("X-AV-Client-Info"));
    }

    public static boolean isJRiverRequest(HttpServletRequest request) {
        return RequestInfo.isJRiverRequest(request.getHeader("User-Agent"));
    }

    public static boolean isXbox360Request(HttpServletRequest request) {
        return RequestInfo.isXbox360Request(request.getHeader("User-Agent"), request.getHeader("Server"));
    }

    public static boolean isXbox360AlbumArtRequest(HttpServletRequest request) {
        return "true".equals(request.getParameter("albumArt")) && isXbox360Request(request);
    }

    public static void dumpRequestHeaders(long timestamp, HttpServletRequest request) {
        dumpRequestHeaders(timestamp, "REQUEST HEADERS", request);
    }

    public static void dumpRequestString(long timestamp, HttpServletRequest request) {
        LoggerFactory.getLogger(ServletRequestInfo.class).info(getRequestInfoString(timestamp, request));
    }

    public static void dumpRequestHeaders(long timestamp, String text, HttpServletRequest request) {
        Logger logger = LoggerFactory.getLogger(ServletRequestInfo.class);
        logger.info(text);
        dumpRequestString(timestamp, request);
        Enumeration<String> headers = request.getHeaderNames();
        if (headers != null) {
            while (headers.hasMoreElements()) {
                String headerName = headers.nextElement();
                logger.info("{}: {}", headerName, request.getHeader(headerName));
            }
        }
        logger.info("----------------------------------------");
    }

    public static String getRequestInfoString(long timestamp, HttpServletRequest request) {
        return String.format("%s %s %s %s %s %d", request.getMethod(), request.getRequestURI(), request.getProtocol(),
                request.getParameterMap(), request.getRemoteAddr(), timestamp);
    }

    public static String getRequestFullURL(HttpServletRequest req) {

        String scheme = req.getScheme(); // http
        String serverName = req.getServerName(); // hostname.com
        int serverPort = req.getServerPort(); // 80
        String contextPath = req.getContextPath(); // /mywebapp
        String servletPath = req.getServletPath(); // /servlet/MyServlet
        String pathInfo = req.getPathInfo(); // /a/b;c=123
        String queryString = req.getQueryString(); // d=789

        // Reconstruct original requesting URL
        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if ((serverPort != 80) && (serverPort != 443)) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath);

        if (pathInfo != null) {
            url.append(pathInfo);
        }
        if (queryString != null) {
            url.append("?").append(queryString);
        }
        return url.toString();
    }
}
