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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class checks all command line options from tool.
 * 
 * It will use a "silent" tool which will redirect System.out and System.err to
 * streams, and will check the "printed" result in some test cases.
 * 
 * @author Jochen Hiller - Initial contribution
 */
class CommandLineTest extends AbstractTestCase {

    @BeforeEach
    void setUp() {
        createSilentTool();
    }

    @AfterEach
    void tearDown() {
        releaseSilentTool();
    }

    @Test
    void testShowUsage() {
        checkCommandLine(tool, JUPnPTool.RC_HELP, "-?");
    }

    @Test
    void testCommandLineHelpOK() {
        checkCommandLine(tool, JUPnPTool.RC_HELP, "");
        checkCommandLine(tool, JUPnPTool.RC_HELP, "-h");
        checkCommandLine(tool, JUPnPTool.RC_HELP, "-?");
        checkCommandLine(tool, JUPnPTool.RC_HELP, "--help");
        checkCommandLine(tool, JUPnPTool.RC_HELP, "-v");
        checkCommandLine(tool, JUPnPTool.RC_HELP, "-h -v");
        checkCommandLine(tool, JUPnPTool.RC_HELP, "--help --verbose");
        // check output, must contain 21x Usage
        // -1 as there is a result BEFORE first Usage
        // *4 as each usage message contains 4x usage text
        assertThat(out.toString().split("Usage").length - 1, is(equalTo(7 * 4)));
    }

    @Test
    void testCommandLineWrongOptionsProduceHelp() {
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "-x");
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--xxx");
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--blablub");
        // check output, must contain 9x usage
        String s = out.toString();
        // -1 as there is a result BEFORE first Usage
        // *4 as each usage message contains 3x usage text
        assertThat(s.split("Usage").length - 1, is(equalTo(3 * 4)));
        // check stderr, must contain 3x error message, and wrong options
        String e = err.toString();
        // -1 as there is a result BEFORE first Usage
        assertThat(e.split("Expected a command").length - 1, is(equalTo(3)));
        assertThat(e, containsString("-x"));
        assertThat(e, containsString("--xxx"));
        assertThat(e, containsString("--blablub"));
    }

    @Test
    void testCommandNop() {
        checkCommandLine(tool, JUPnPTool.RC_OK, "nop");
        assertThat(out.toString(), containsString("No operation"));
    }

    @Test
    void testLogLevelOK() {
        checkCommandLine(tool, JUPnPTool.RC_OK, "nop");
        checkCommandLine(tool, JUPnPTool.RC_OK, "-l=INFO nop");
        checkCommandLine(tool, JUPnPTool.RC_OK, "--loglevel=INFO nop");
        resetStreams();
        // do not check streams, will have some INFO messages

        checkCommandLine(tool, JUPnPTool.RC_OK, "--loglevel=WARN nop");
        assertThat(out.toString(), containsString("No operation"));
    }

    @Test
    void testLogLevelWithWrongOptions() {
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "-l=XYZ nop");
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--loglevel=ABC nop");
        // check output, must contain 6x usage
        String s = out.toString();
        // -1 as there is a result BEFORE first Usage
        // *4 as each usage message contains 3x usage text
        assertThat(s.split("Usage").length - 1, is(equalTo(2 * 4)));
    }

    /**
     * Will check whether log messages will NOT be in stdout.
     */
    @Test
    void testLogLevelDisabled() {
        checkCommandLine(tool, JUPnPTool.RC_OK, "nop");
        Logger l = LoggerFactory.getLogger(this.getClass());
        l.info("Some Info");
        assertThat(out.toString(), containsString("No operation"));
    }

    /**
     * Will check whether log messages will be in stdout.
     */
    @Test
    void testLogLevelEnabled() {
        checkCommandLine(tool, JUPnPTool.RC_OK, "--loglevel=INFO nop");
        Logger l = LoggerFactory.getLogger(this.getClass());
        l.info("Some Info");
        l.debug("Some Debug");
        String s = out.toString();
        assertThat(s, containsString("INFO  org.jupnp.tool.cli.CommandLineTest - Some Info"));
        // DEBUG must NOT be included
        // assertFalse(s.contains("DEBUG"));
        assertThat(s, not(containsString("DEBUG")));
    }

    @Test
    void testPoolConfigurationsOK() {
        CmdlineUPnPServiceConfiguration.setDebugStatistics(false);
        checkCommandLine(tool, JUPnPTool.RC_OK, "--pool=20,20 nop");
        assertThat(CmdlineUPnPServiceConfiguration.MAIN_POOL_SIZE, is(20));
        assertThat(CmdlineUPnPServiceConfiguration.ASYNC_POOL_SIZE, is(20));
        assertThat(MonitoredQueueingThreadPoolExecutor.DEBUG_STATISTICS, is(false));
    }

    @Test
    void testPoolConfigurationsWrong() {
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--pool=20,20,20,20 nop");
        assertThat(err.toString(), containsString("(not 2 or 3 parameters)"));
        resetStreams();

        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--pool=20 nop");
        assertThat(err.toString(), containsString("(not 2 or 3 parameters)"));
        resetStreams();

        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "-p=0,0 nop");
        assertThat(err.toString(), containsString("(all values must be greater than 0)"));
        resetStreams();

        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "-p=-1,20 nop");
        assertThat(err.toString(), containsString("(all values must be greater than 0)"));
        resetStreams();
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "-p=20,-1 nop");
        assertThat(err.toString(), containsString("(all values must be greater than 0)"));
        resetStreams();

        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--pool=20A,40 nop");
        assertThat(err.toString(), containsString("(numbers wrong)"));
        resetStreams();
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--pool=20,4B0 nop");
        assertThat(err.toString(), containsString("(numbers wrong)"));
        resetStreams();

        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--pool=20,40,WRONGOPTIONS nop");
        assertThat(err.toString(), containsString("(only stats allowed as last option)"));
        resetStreams();
    }

    @Test
    void testMulticastResponsPortOK() {
        checkCommandLine(tool, JUPnPTool.RC_OK, "--multicastResponsePort=1900 nop");
        checkCommandLine(tool, JUPnPTool.RC_OK, "-m=1900 nop");
        checkCommandLine(tool, JUPnPTool.RC_OK, "--multicastResponsePort=0 nop");
        checkCommandLine(tool, JUPnPTool.RC_OK, "-m=0 nop");
        checkCommandLine(tool, JUPnPTool.RC_OK, "--multicastResponsePort=65535 nop");
        checkCommandLine(tool, JUPnPTool.RC_OK, "-m=65535 nop");
    }

    @Test
    void testMulticastResponsPortWrong() {
        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--multicastResponsePort=ABC nop");
        assertThat(err.toString(), containsString("is not a valid number"));
        resetStreams();

        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--multicastResponsePort=-1 nop");
        assertThat(err.toString(), containsString("must be between 0..65535"));
        resetStreams();

        checkCommandLine(tool, JUPnPTool.RC_INVALID_OPTION, "--multicastResponsePort=65536 nop");
        assertThat(err.toString(), containsString("must be between 0..65535"));
        resetStreams();
    }
}
