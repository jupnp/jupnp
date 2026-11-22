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
package org.jupnp.support.model.dlna.message;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.jupnp.support.model.dlna.message.header.DLNAHeader;
import org.jupnp.support.model.dlna.message.header.SupportedHeader;

class DLNAHeadersTest {

    // Takes more than a minute to split with a regular expression that starts with \s*
    private static final String LONG_RUN_OF_WHITESPACE = "a" + " ".repeat(200_000) + "b";

    @Test
    void parseSupported() {
        DLNAHeaders headers = new DLNAHeaders();
        headers.add("Supported", "dlna.announce, dlna.rtx ,dlna.rtx-dup;");

        assertArrayEquals(new String[] { "dlna.announce", "dlna.rtx", "dlna.rtx-dup" },
                headers.getFirstHeader(DLNAHeader.Type.Supported, SupportedHeader.class).getValue());
    }

    @Test
    void parseSupportedWithLongRunOfWhitespaceInLinearTime() {
        DLNAHeaders headers = new DLNAHeaders();
        headers.add("Supported", LONG_RUN_OF_WHITESPACE);

        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertNotNull(headers.getFirstHeader(DLNAHeader.Type.Supported, SupportedHeader.class)));
    }

    @Test
    void parsePragmaWithLongRunOfWhitespaceInLinearTime() {
        assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> assertNotNull(DLNAHeader.newInstance(DLNAHeader.Type.PRAGMA, LONG_RUN_OF_WHITESPACE)));
    }
}
