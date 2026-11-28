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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpResponse;

class UpnpMessageTest {

    @Test
    void emptyBodyString() {
        StreamResponseMessage message = new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
        message.setBody("");

        assertEquals("", message.getBodyString());
        assertFalse(message.isBodyNonEmptyString());
    }

    @Test
    void bodyCharactersWithOnlyByteOrderMark() {
        StreamResponseMessage message = new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
        message.getHeaders().add("Content-Type", "text/xml; charset=\"utf-16\"");

        // The UTF-16 decoder consumes the byte order mark, the body has no characters
        message.setBodyCharacters(new byte[] { (byte) 0xFE, (byte) 0xFF });

        assertEquals("", message.getBodyString());
        assertFalse(message.isBodyNonEmptyString());
    }

    @Test
    void byteOrderMarkIsRemovedFromBodyString() {
        StreamResponseMessage message = new StreamResponseMessage(new UpnpResponse(UpnpResponse.Status.OK));
        message.setBody("\ufeff<foo/>");

        assertEquals("<foo/>", message.getBodyString());
        assertTrue(message.isBodyNonEmptyString());
    }
}
