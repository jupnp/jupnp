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
package org.jupnp.model.message;

/**
 * A response message, with a status code and message (OK, NOT FOUND, etc).
 *
 * @author Christian Bauer
 */
public class UpnpResponse extends UpnpOperation {

    public enum Status {

        OK(200, "OK"),
        BAD_REQUEST(400, "Bad Request"),
        NOT_FOUND(404, "Not Found"),
        METHOD_NOT_SUPPORTED(405, "Method Not Supported"),
        PRECONDITION_FAILED(412, "Precondition Failed"),
        UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        NOT_IMPLEMENTED(501, "Not Implemented");

        private final int statusCode;
        private final String statusMsg;

        Status(int statusCode, String statusMsg) {
            this.statusCode = statusCode;
            this.statusMsg = statusMsg;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusMsg() {
            return statusMsg;
        }

        public static Status getByStatusCode(int statusCode) {
            for (Status status : values()) {
                if (status.getStatusCode() == statusCode) {
                    return status;
                }
            }
            return null;
        }
    }

    private int statusCode;
    private String statusMessage;

    public UpnpResponse(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public UpnpResponse(Status status) {
        this.statusCode = status.getStatusCode();
        this.statusMessage = status.getStatusMsg();
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * @return <code>true</code> if the status code was equal or creater 300.
     */
    public boolean isFailed() {
        return statusCode >= 300;
    }

    /**
     * @return The concatenated string of status code and status message (same as {@link #toString()}.
     */
    public String getResponseDetails() {
        return getStatusCode() + " " + getStatusMessage();
    }

    @Override
    public String toString() {
        return getResponseDetails();
    }
}
