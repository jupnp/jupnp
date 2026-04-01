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

import java.io.PrintStream;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.internal.DefaultConsole;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * This class is the main class for the jupnptool.
 * 
 * @author Jochen Hiller - Initial contribution
 * @author Jochen Hiller - Added pool configuration arguments, added
 *         jul-over-slf4j logging
 * @author Jochen Hiller - removed jul-over-slf4j bride, as slf4j is now primary API
 */
public class JUPnPTool {

    public static final int RC_OK = 0;
    public static final int RC_HELP = 1;
    public static final int RC_INVALID_OPTION = 2;
    public static final int RC_MISSING_ARGUMENTS = 3;

    public static final String TOOL_NAME = "jupnptool";

    private static final String COMMAND_SEARCH = "search";
    private static final String COMMAND_INFO = "info";
    private static final String COMMAND_NOP = "nop";

    private static final long DEFAULT_TIMEOUT = 10L;

    private final Logger logger = LoggerFactory.getLogger(JUPnPTool.class);

    protected PrintStream outputStream;
    protected PrintStream errorStream;

    /** Local created service, make it available for testing purposes. */
    protected UpnpService upnpService;

    /** Holds the pool configuration. */
    private String poolConfiguration;

    /** Holds the multicastResponsePort. */
    private Integer multicastResponsePort;

    public static void main(String[] args) {
        JUPnPTool tool = new JUPnPTool();
        int rc = tool.doMain(args);
        System.exit(rc);
    }

    public JUPnPTool() {
        this(System.out, System.err);
    }

    public JUPnPTool(PrintStream out, PrintStream err) {
        this.outputStream = out;
        this.errorStream = err;
    }

    public int doMain(String[] args) {
        // parse command line arguments with jCommander
        JCommander commander = new JCommander(new CommandLineArgs());
        commander.addCommand(COMMAND_SEARCH, new SearchCommandArgs());
        commander.addCommand(COMMAND_INFO, new InfoCommandArgs());
        commander.addCommand(COMMAND_NOP, new NopCommandArgs());
        commander.setConsole(new DefaultConsole(outputStream));
        commander.setProgramName(TOOL_NAME);
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            printStderr(e.getLocalizedMessage());
            printToolUsage(commander);
            return RC_INVALID_OPTION;
        }
        List<Object> objs = commander.getObjects();
        CommandLineArgs cmdLineArgs = (CommandLineArgs) objs.get(0);

        // if logging enabled, use other logback XML file
        if (cmdLineArgs.isLoggingEnabled()) {
            setLogging("logback-enabled.xml", cmdLineArgs.logLevel);
        } else {
            setLogging("logback.xml", "OFF");
        }

        // check if pool has been configured, preserve that
        if (cmdLineArgs.poolConfig != null) {
            poolConfiguration = cmdLineArgs.poolConfig;
        }
        // multicast response port
        if (cmdLineArgs.multicastResponsePort != null) {
            multicastResponsePort = cmdLineArgs.multicastResponsePort;
        }

        // dispatch commands
        if (Boolean.TRUE.equals(cmdLineArgs.doHelp)) {
            printToolUsage(commander);
            return RC_HELP;
        } else if (COMMAND_SEARCH.equals(commander.getParsedCommand())) {
            JCommander searchCommander = commander.getCommands().get(COMMAND_SEARCH);
            SearchCommandArgs searchArgs = (SearchCommandArgs) searchCommander.getObjects().get(0);
            int timeout = searchArgs.timeout;
            String sortBy = searchArgs.sortBy;
            String filter = searchArgs.filter;
            boolean verbose = cmdLineArgs.verbose;
            // if udn or manufacturer will be specified: auto-enable verbose
            if ("udn".equals(sortBy) || "manufacturer".equals(sortBy)) {
                verbose = true;
            }

            printToolStartMessage("Search for UPnP devices for " + timeout + " seconds sorted by " + sortBy
                    + " and filtered by " + filter);
            SearchCommand cmd = new SearchCommand(this);
            return cmd.run(timeout, sortBy, filter, verbose);
        } else if (COMMAND_INFO.equals(commander.getParsedCommand())) {
            JCommander infoCommander = commander.getCommands().get(COMMAND_INFO);
            InfoCommandArgs infoArgs = (InfoCommandArgs) infoCommander.getObjects().get(0);
            List<String> ipAddressOrUdns = infoArgs.ipAddressOrUdnList;
            boolean verbose = cmdLineArgs.verbose;

            if (ipAddressOrUdns == null || ipAddressOrUdns.isEmpty()) {
                return RC_MISSING_ARGUMENTS;
            }

            printToolStartMessage("Info for UPnP devices for " + ipAddressOrUdns);
            InfoCommand cmd = new InfoCommand(this);
            return cmd.run(ipAddressOrUdns, verbose);
        } else if (COMMAND_NOP.equals(commander.getParsedCommand())) {
            // for NOP command we create a UPnP service, start and shutdown
            // immediately. This helps during testing
            logger.debug("Starting jUPnP...");
            printToolStartMessage("No operation");

            upnpService = createUpnpService();
            upnpService.startup();
            try {
                logger.debug("Stopping jUPnP...");
                upnpService.shutdown();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
            return RC_OK;
        } else {
            printToolUsage(commander);
            return RC_HELP;
        }
    }

    // protected methods

    protected UpnpService createUpnpService() {
        return createUpnpService(DEFAULT_TIMEOUT);
    }

    protected UpnpService createUpnpService(long timeoutSeconds) {
        // sets the pool configuration
        if (poolConfiguration != null) {
            StringTokenizer tokenizer = new StringTokenizer(poolConfiguration, ",");
            int mainPoolSize = Integer.parseInt(tokenizer.nextToken());
            int asyncPoolSize = Integer.parseInt(tokenizer.nextToken());

            CmdlineUPnPServiceConfiguration.setPoolConfiguration(mainPoolSize, asyncPoolSize);
            // one token left for stats option?
            String stats = tokenizer.countTokens() == 1 ? tokenizer.nextToken() : null;
            if (CommandLineArgs.POOL_CONFIG_STATS_OPTION.equalsIgnoreCase(stats)) {
                CmdlineUPnPServiceConfiguration.setDebugStatistics(true);
            }
        }
        if (multicastResponsePort != null) {
            CmdlineUPnPServiceConfiguration.setMulticastResponsePort(multicastResponsePort);
        }

        return new UpnpServiceImpl(new CmdlineUPnPServiceConfiguration());
    }

    /**
     * Sets the logger to the resource name, and reset logback configuration.
     * 
     * @param resourceName
     *            either logback.xml, or logback-enabled.xml
     */
    protected void setLogging(String resourceName, String rootAppenderLogLevel) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            // will assume to find logback XML files in root of JAR file
            URL url = getClass().getResource("/" + resourceName);
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);

            // Call context.reset() to clear any previous configuration, e.g.
            // default configuration
            context.reset();
            configurator.doConfigure(url);

            ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory
                    .getLogger(Logger.ROOT_LOGGER_NAME);
            Level level = Level.valueOf(rootAppenderLogLevel);
            rootLogger.setLevel(level);

        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        // see https://issues.apache.org/jira/browse/SLING-3045
        // there can be sync issues when reconfiguring logback
        long now = new Date().getTime();
        StatusPrinter.printInCaseOfErrorsOrWarnings(context, now + 1000);
    }

    // package methods

    void printStdout(String msg) {
        this.outputStream.println(msg);
    }

    void printStderr(String msg) {
        this.errorStream.println(msg);
    }

    // private methods

    private void printToolStartMessage(String msg) {
        printStdout(getToolNameVersion() + ": " + msg
                + (poolConfiguration != null ? " (poolConfiguration='" + poolConfiguration + "'" : "")
                + (multicastResponsePort != null ? ", multicastResponsePort=" + multicastResponsePort : "") + ")");
    }

    private void printToolUsage(JCommander commander) {
        commander.usage();
    }

    private String getToolNameVersion() {
        String name = getClass().getPackage().getImplementationTitle();
        String version = getClass().getPackage().getImplementationVersion();
        return name + " (" + version + ")";
    }
}
