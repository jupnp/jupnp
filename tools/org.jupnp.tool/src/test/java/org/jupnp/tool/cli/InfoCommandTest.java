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
package org.jupnp.tool.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This test cases checks info command.
 * 
 * @author Jochen Hiller - Initial contribution
 */
class InfoCommandTest extends AbstractTestCase {

    @BeforeEach
    void setUp() {
        // use standard tool to see output in console
        this.tool = new JUPnPTool();
    }

    @AfterEach
    void tearDown() {
        this.tool = null;
    }

    @Test
    void testInfoArgsMissing() {
        checkCommandLine(tool, JUPnPTool.RC_MISSING_ARGUMENTS, "info");
    }

    @Test
    void testInfoIpAddress() {
        checkCommandLine(tool, JUPnPTool.RC_OK, "--loglevel=INFO --verbose info 192.168.3.106 192.168.3.1");
        checkCommandLine(tool, JUPnPTool.RC_OK, "info 192.168.3.106 192.168.3.107");
    }

    @Test
    void testInfoUDN() {
        checkCommandLine(tool, JUPnPTool.RC_OK, "--verbose info DCS-2332L-123456789012");
        checkCommandLine(tool, JUPnPTool.RC_OK, "info DCS-2332L-123456789012 12345678-1234-1234-1234-123456789012");
    }
}
