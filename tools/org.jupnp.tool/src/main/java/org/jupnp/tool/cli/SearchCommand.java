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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jupnp.UpnpService;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jochen Hiller - Initial contribution
 * @author Jochen Hiller - set verbose level of SpecificationViolationReporter
 * @author Amit Kumar Mondal - Added feature to display all endpoints from same presentation URL
 */
public class SearchCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchCommand.class);
    private final JUPnPTool tool;

    public SearchCommand(JUPnPTool tool) {
        this.tool = tool;
    }

    public int run(int timeout, String sortBy, String filter, boolean verbose) {
        // This will create necessary network resources for UPnP right away
        LOGGER.debug("Starting jUPnP search...");
        if (verbose) {
            SpecificationViolationReporter.enableReporting();
        } else {
            LOGGER.debug("Disable UPnP specification violation reportings");
            SpecificationViolationReporter.disableReporting();
        }
        UpnpService upnpService = tool.createUpnpService();
        upnpService.startup();

        SearchResultPrinter printer = new SearchResultPrinter(sortBy, verbose);
        if (!hasToSort(sortBy)) {
            upnpService.getRegistry().addListener(new SearchRegistryListener(printer, sortBy, filter, verbose));
        }
        printer.printHeader();

        // Send a search message to all devices and services, they should
        // respond soon
        LOGGER.debug("Sending SEARCH message to all devices...");
        upnpService.getControlPoint().search(new STAllHeader());

        // Let's wait "timeout" for them to respond
        LOGGER.debug("Waiting {} seconds before shutting down...", timeout);
        try {
            Thread.sleep(timeout * 1000);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting before shutdown", e);
        }

        LOGGER.debug("Processing results...");
        Registry registry = upnpService.getRegistry();

        for (RemoteDevice device : registry.getRemoteDevices()) {
            handleRemoteDevice(device, printer, filter);
        }

        printer.printBody();

        // Release all resources and advertise BYEBYE to other UPnP devices
        LOGGER.debug("Stopping jUPnP...");
        try {
            upnpService.shutdown();
        } catch (Exception e) {
            LOGGER.error("Error during shutdown", e);
        }
        LOGGER.debug("Stopped jUPnP...");

        return JUPnPTool.RC_OK;
    }

    private void handleRemoteDevice(RemoteDevice device, SearchResultPrinter searchResult, String filter) {
        if (device.isRoot()) {
            String ipAddress = device.getIdentity().getDescriptorURL().getHost();
            String model = device.getDetails().getModelDetails().getModelName();
            String manu = device.getDetails().getManufacturerDetails().getManufacturer();
            String udn = device.getIdentity().getUdn().getIdentifierString();
            String name = device.getDisplayString();
            String serialNumber = device.getDetails().getSerialNumber();
            // some devices will return "null" as serialNumber
            // TODO needs check where this happens in JUPnP
            if (serialNumber == null || "null".equals(serialNumber)) {
                serialNumber = "-";
            }

            String fullDeviceInformationString = ipAddress + "\n" + model + "\n" + manu + "\n" + udn + "\n"
                    + serialNumber + "\n" + name;
            boolean filterOK = false;
            if ("*".equals(filter)) {
                filterOK = true;
            } else if (fullDeviceInformationString.contains(filter)) {
                LOGGER.debug("Filter check: filter '{}' matched '{}'", filter, fullDeviceInformationString);
                filterOK = true;
            } else {
                LOGGER.debug("Filter check: filter '{}' NOT matched '{}'", filter, fullDeviceInformationString);
            }

            // filter out: very simple: details from above should include this text
            if (filterOK) {
                searchResult.add(ipAddress, model, serialNumber, manu, udn);
            }
        }
    }

    class SearchRegistryListener implements RegistryListener {

        private final SearchResultPrinter printer;
        private final String sortBy;
        private final String filter;
        private final boolean verbose;

        public SearchRegistryListener(SearchResultPrinter printer, String sortBy, String filter, boolean verbose) {
            this.printer = printer;
            this.sortBy = sortBy;
            this.filter = filter;
            this.verbose = verbose;
        }

        @Override
        public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
            // ignore
        }

        @Override
        public void remoteDeviceDiscoveryFailed(Registry registry, RemoteDevice device, Exception e) {
            // ignore
        }

        @Override
        public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
            handleRemoteDevice(device, printer, filter);
        }

        @Override
        public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
            // ignore
        }

        @Override
        public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
            // ignore
        }

        @Override
        public void localDeviceAdded(Registry registry, LocalDevice device) {
            // ignore
        }

        @Override
        public void localDeviceRemoved(Registry registry, LocalDevice device) {
            // ignore
        }

        @Override
        public void beforeShutdown(Registry registry) {
            // ignore
        }

        @Override
        public void afterShutdown() {
            // ignore
        }
    }

    /**
     * Will contain search results, will filter out duplicate ip addresses.
     */
    public class SearchResultPrinter {

        private class Result {
            private String ipAddress;
            private String model;
            private String serialNumber;
            private String manufacturer;
            private String udn;

            public Result(String i, String m, String s, String manu, String udn) {
                this.ipAddress = i;
                this.model = m;
                this.serialNumber = s;
                this.manufacturer = manu;
                this.udn = udn;
            }
        }

        private final int[] columnWidth = new int[] { 17, 25, 25, 25, 25 };
        private final List<Result> results = new ArrayList<>();
        private final List<String> udns = new ArrayList<>();
        private final String sortBy;
        private final boolean verbose;

        public SearchResultPrinter(String sortBy, boolean verbose) {
            this.sortBy = sortBy;
            this.verbose = verbose;
        }

        public void printHeader() {
            if (hasToSort(sortBy)) {
                // nothing to do, header will be printed later
                return;
            }
            String msg;
            if (verbose) {
                msg = fixedWidth("IP address", columnWidth[0]) + fixedWidth("Model", columnWidth[1])
                        + fixedWidth("Manufacturer", columnWidth[2]) + fixedWidth("SerialNumber", columnWidth[3])
                        + fixedWidth("UDN", columnWidth[4]);
            } else {
                msg = fixedWidth("IP address", columnWidth[0]) + fixedWidth("Model", columnWidth[1])
                        + fixedWidth("SerialNumber", columnWidth[3]);
            }
            tool.printStdout(msg);
        }

        public void add(String ip, String model, String serialNumber, String manu, String udn) {
            if (!udns.contains(udn)) {
                results.add(new Result(ip, model, serialNumber, manu, udn));
                if (!hasToSort(sortBy)) {
                    String msg;
                    if (verbose) {
                        msg = fixedWidth(ip, columnWidth[0]) + fixedWidth(model, columnWidth[1])
                                + fixedWidth(manu, columnWidth[2]) + fixedWidth(serialNumber, columnWidth[3])
                                + fixedWidth(udn, columnWidth[4]);
                    } else {
                        msg = fixedWidth(ip, columnWidth[0]) + fixedWidth(model, columnWidth[1])
                                + fixedWidth(serialNumber, columnWidth[3]);
                    }
                    tool.printStdout(msg);
                }
                udns.add(udn);
            }
        }

        public void printBody() {
            if (!hasToSort(sortBy)) {
                // nothing to do, results have been printed during add()
                return;
            }
            String msg = asBody();
            tool.printStdout(msg);
        }

        public String asBody() {
            // sort now
            sortResults(sortBy);
            // convert map to table
            List<String[]> table = new ArrayList<>();
            if (verbose) {
                table.add(new String[] { "IP address", "Model", "Manufacturer", "SerialNumber", "UDN" });
            } else {
                table.add(new String[] { "IP address", "Model", "SerialNumber" });
            }
            for (Result result : results) {
                if (verbose) {
                    table.add(new String[] { result.ipAddress, result.model, result.manufacturer, result.serialNumber,
                            result.udn });
                } else {
                    table.add(new String[] { result.ipAddress, result.model, result.serialNumber });
                }
            }
            String msg = PrintUtils.printTable(table, 4);
            // if only one line: no device found
            if (results.isEmpty()) {
                msg = msg + "<no device found>";
            }
            return msg;
        }

        private void sortResults(final String columnName) {
            Comparator<Result> comparator = (o1, o2) -> {
                if ("ip".equals(columnName)) {
                    return IpAddressUtils.compareIpAddress(o1.ipAddress, o2.ipAddress);
                } else if ("model".equals(columnName)) {
                    return o1.model.compareTo(o2.model);
                } else if ("serialNumber".equals(columnName)) {
                    return o1.serialNumber.compareTo(o2.serialNumber);
                } else if ("manufacturer".equals(columnName)) {
                    return o1.manufacturer.compareTo(o2.manufacturer);
                } else if ("udn".equals(columnName)) {
                    return o1.udn.compareTo(o2.udn);
                } else {
                    return 0;
                }
            };
            results.sort(comparator);
        }

        private static final String STRING_WITH_SPACES = "                           ";

        private String fixedWidth(String s, int width) {
            if (s == null) {
                return STRING_WITH_SPACES.substring(0, width);
            } else if (s.length() >= width) {
                return s;
            } else {
                return s + STRING_WITH_SPACES.substring(0, width - s.length());
            }
        }
    }

    private boolean hasToSort(String sortBy) {
        return !"none".equals(sortBy);
    }
}
