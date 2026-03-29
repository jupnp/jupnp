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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Common functionality for test cases using JUPnPTool.
 * 
 * @author Jochen Hiller - Initial contribution
 */
public abstract class AbstractTestCase {

    protected JUPnPTool tool;
    protected ByteArrayOutputStream out;
    protected ByteArrayOutputStream err;

    public void createSilentTool() {
        // will write output to buffers to check content later in tests
        this.out = new ByteArrayOutputStream();
        this.err = new ByteArrayOutputStream();
        this.tool = new JUPnPToolWithRedirectionOfOutput(new PrintStream(out), new PrintStream(err));
    }

    public void releaseSilentTool() {
        try {
            this.err.close();
            this.err = null;
            this.out.close();
            this.out = null;
            this.tool = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void checkCommandLine(final JUPnPTool tool, final int rcExpected, final String argsAsString) {
        final String[] args = argsAsString.isBlank() ? new String[0] : argsAsString.split(" ");
        final int rc = tool.doMain(args);
        assertEquals(rcExpected, rc);
    }

    public void resetStreams() {
        this.out.reset();
        this.err.reset();
    }
}
