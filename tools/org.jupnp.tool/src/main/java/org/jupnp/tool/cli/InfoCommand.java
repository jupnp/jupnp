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

import java.util.List;

import org.jupnp.UpnpService;
import org.jupnp.model.message.header.STAllHeader;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jochen Hiller - Initial contribution
 * @author Jochen Hiller - set verbose level of SpecificationViolationReporter
 */
public class InfoCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfoCommand.class);
    private final JUPnPTool tool;
    private StringBuilder sb;

    public InfoCommand(JUPnPTool tool) {
        this.tool = tool;
    }

    public int run(List<String> ipAddressOrUdns, boolean verbose) {
        LOGGER.info("Show information for devices {}", flatList(ipAddressOrUdns));
        if (verbose) {
            SpecificationViolationReporter.enableReporting();
        } else {
            LOGGER.debug("Disable UPnP specification violation reportings");
            SpecificationViolationReporter.disableReporting();
        }

        UpnpService upnpService = tool.createUpnpService();
        upnpService.startup();
        upnpService.getControlPoint().search(new STAllHeader());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            // ignore
        }

        Registry registry = upnpService.getRegistry();
        for (RemoteDevice device : registry.getRemoteDevices()) {
            String ipAddress = device.getIdentity().getDescriptorURL().getHost();
            String udn = device.getIdentity().getUdn().getIdentifierString();
            LOGGER.info("ip: {}, udn={}", ipAddress, udn);

            for (String ipAddressOrUdn : ipAddressOrUdns) {
                boolean match = false;
                if (isSameUdn(udn, ipAddressOrUdn)) {
                    match = true;
                } else {
                    try {
                        if (isSameIpAddress(ipAddress, ipAddressOrUdn)) {
                            match = true;
                        }
                    } catch (IllegalArgumentException e) {
                        // ignore errors
                    }
                }
                if (match) {
                    showDeviceInfo(device, ipAddressOrUdn, verbose);
                }
            }
        }
        upnpService.shutdown();

        return JUPnPTool.RC_OK;
    }

    private void showDeviceInfo(RemoteDevice device, String searchCriteria, boolean verbose) {
        LOGGER.info("{}", device);
        sb = new StringBuilder();
        sb.append("Device info for ");
        sb.append(searchCriteria);
        print("UDN", device.getIdentity().getUdn().getIdentifierString());
        print("Display", device.getDisplayString());
        print("Model.Name", device.getDetails().getModelDetails().getModelName());
        print("Model.Description", device.getDetails().getModelDetails().getModelDescription());
        print("Model.Number", device.getDetails().getModelDetails().getModelNumber());
        print("Model.URI", device.getDetails().getModelDetails().getModelURI());
        print("DescriptorURL", device.getIdentity().getDescriptorURL());
        print("SerialNumber", device.getDetails().getSerialNumber());
        print("FriendlyName", device.getDetails().getFriendlyName());
        print("UPC", device.getDetails().getUpc());
        print("Manufacturer", device.getDetails().getManufacturerDetails().getManufacturer());
        print("ManufacturerURI", device.getDetails().getManufacturerDetails().getManufacturerURI());
        print("PresentationURI", device.getDetails().getPresentationURI());
        if (verbose) {
            print("Type", device.getType().getType());
            print("Type.Display", device.getType().getDisplayString());
            print("Type.Version", String.valueOf(device.getType().getVersion()));
            print("UDA.Version", device.getVersion().getMajor() + "." + device.getVersion().getMinor());
            print("isRootDevice", String.valueOf(device.isRoot()));
            print("isFullyHydrated", String.valueOf(device.isFullyHydrated()));
        }

        sb.append("\n");
        tool.printStdout(sb.toString());
    }

    private void print(final String desc, final Object o) {
        if (o == null) {
            // do skip this property, as null makes no sense
            return;
        } else {
            String s;
            if (o instanceof String) {
                if (((String) o).isEmpty()) {
                    s = "\"\"";
                } else {
                    s = (String) o;
                }
            } else {
                s = String.valueOf(o);
            }
            sb.append("\n");
            sb.append(desc);
            sb.append("\t\t\t\t", 0, 3 - (desc.length() / 8));
            sb.append(s);
        }
    }

    private String flatList(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (String s : l) {
            sb.append(s);
            sb.append(" ");
        }
        return sb.toString();
    }

    private boolean isSameIpAddress(String ip1, String ip2) {
        return IpAddressUtils.isSameIpAddress(ip1, ip2);
    }

    private boolean isSameUdn(String udn1, String udn2) {
        if (udn1 == null || udn2 == null) {
            return false;
        }
        return udn1.equals(udn2);
    }
}
