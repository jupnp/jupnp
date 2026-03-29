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
package org.jupnp.android;

import java.lang.reflect.Field;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;

import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.spi.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This factory tries to work around and patch some Android bugs.
 *
 * @author Michael Pujos
 * @author Christian Bauer
 */
public class AndroidNetworkAddressFactory extends NetworkAddressFactoryImpl {

    private final Logger logger = LoggerFactory.getLogger(AndroidNetworkAddressFactory.class);

    public AndroidNetworkAddressFactory(int streamListenPort, int multicastResponsePort) {
        super(streamListenPort, multicastResponsePort);
    }

    @Override
    protected boolean requiresNetworkInterface() {
        return false;
    }

    @Override
    protected boolean isUsableAddress(NetworkInterface networkInterface, InetAddress address) {
        boolean result = super.isUsableAddress(networkInterface, address);
        if (result) {
            // TODO: Workaround Android DNS reverse lookup issue, still a problem on ICS+?
            // http://4thline.org/projects/mailinglists.html#nabble-td3011461
            String hostName = address.getHostAddress();

            Field field0;
            Object target;

            try {

                try {
                    field0 = InetAddress.class.getDeclaredField("holder");
                    field0.setAccessible(true);
                    target = field0.get(address);
                    field0 = target.getClass().getDeclaredField("hostName");
                } catch (NoSuchFieldException e) {
                    // Let's try the non-OpenJDK variant
                    field0 = InetAddress.class.getDeclaredField("hostName");
                    target = address;
                }

                if (field0 != null && target != null && hostName != null) {
                    field0.setAccessible(true);
                    field0.set(target, hostName);
                } else {
                    return false;
                }

            } catch (Exception e) {
                logger.error("Failed injecting hostName to work around Android InetAddress DNS bug: {}", address, e);
                return false;
            }
        }
        return result;
    }

    @Override
    public InetAddress getLocalAddress(NetworkInterface networkInterface, boolean isIPv6, InetAddress remoteAddress) {
        // TODO: This is totally random because we can't access low level InterfaceAddress on Android!
        for (InetAddress localAddress : getInetAddresses(networkInterface)) {
            if (isIPv6 && localAddress instanceof Inet6Address) {
                return localAddress;
            }
            if (!isIPv6 && localAddress instanceof Inet4Address) {
                return localAddress;
            }
        }
        throw new IllegalStateException(
                "Can't find any IPv4 or IPv6 address on interface: " + networkInterface.getDisplayName());
    }

    @Override
    protected void discoverNetworkInterfaces() throws InitializationException {
        try {
            super.discoverNetworkInterfaces();
        } catch (Exception e) {
            // TODO: ICS bug on some models with network interface disappearing while enumerated
            // http://code.google.com/p/android/issues/detail?id=33661
            logger.warn("Exception while enumerating network interfaces, trying once more", e);
            super.discoverNetworkInterfaces();
        }
    }
}
