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
package org.jupnp.transport.impl;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jupnp.model.Constants;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.NoNetworkException;
import org.jupnp.util.Iterators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of network interface and address configuration/discovery.
 * <p>
 * This implementation has been tested on Windows XP, Windows Vista, Mac OS X 10.8,
 * and whatever kernel ships in Ubuntu 9.04. This implementation does not support IPv6.
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - added multicast response port
 * @author Laurent Garnier - added new parameter to provide a list of network interfaces to consider
 */
public class NetworkAddressFactoryImpl implements NetworkAddressFactory {

    // Ephemeral port is the default
    public static final int DEFAULT_TCP_HTTP_LISTEN_PORT = 0;

    // Ephemeral port is the default
    public static final int DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT = 0;

    private final Logger logger = LoggerFactory.getLogger(NetworkAddressFactoryImpl.class);

    protected final Set<String> useInterfaces = new HashSet<>();
    protected final Set<String> useAddresses = new HashSet<>();

    protected final List<NetworkInterface> networkInterfaces = new ArrayList<>();
    protected final List<InetAddress> bindAddresses = new ArrayList<>();

    protected int streamListenPort;
    protected int multicastResponsePort;

    /**
     * Defaults to an ephemeral port.
     */
    public NetworkAddressFactoryImpl() throws InitializationException {
        this(DEFAULT_TCP_HTTP_LISTEN_PORT, DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT);
    }

    public NetworkAddressFactoryImpl(int streamListenPort, int multicastResponsePort) throws InitializationException {
        this(streamListenPort, multicastResponsePort, null);
    }

    public NetworkAddressFactoryImpl(int streamListenPort, int multicastResponsePort, String interfaces)
            throws InitializationException {
        String useInterfacesString = interfaces != null && !interfaces.isBlank() ? interfaces
                : System.getProperty(SYSTEM_PROPERTY_NET_IFACES);
        if (useInterfacesString != null) {
            String[] userInterfacesStrings = useInterfacesString.split(",");
            useInterfaces.addAll(Arrays.asList(userInterfacesStrings));
        }

        String useAddressesString = System.getProperty(SYSTEM_PROPERTY_NET_ADDRESSES);
        if (useAddressesString != null) {
            String[] useAddressesStrings = useAddressesString.split(",");
            useAddresses.addAll(Arrays.asList(useAddressesStrings));
        }

        discoverNetworkInterfaces();
        discoverBindAddresses();

        if (networkInterfaces.isEmpty() || bindAddresses.isEmpty()) {
            logger.warn("No usable network interface or addresses found");
            if (requiresNetworkInterface()) {
                throw new NoNetworkException("Could not discover any usable network interfaces and/or addresses");
            }
        }

        this.streamListenPort = streamListenPort;
        this.multicastResponsePort = multicastResponsePort;
    }

    /**
     * @return <code>true</code> (the default) if a <code>MissingNetworkInterfaceException</code> should be thrown
     */
    protected boolean requiresNetworkInterface() {
        return true;
    }

    @Override
    public void logInterfaceInformation() {
        synchronized (networkInterfaces) {
            if (networkInterfaces.isEmpty()) {
                logger.info("No network interface to display!");
                return;
            }
            for (NetworkInterface networkInterface : networkInterfaces) {
                try {
                    logInterfaceInformation(networkInterface);
                } catch (SocketException e) {
                    logger.warn("Exception while logging network interface information", e);
                }
            }
        }
    }

    @Override
    public InetAddress getMulticastGroup() {
        try {
            return InetAddress.getByName(Constants.IPV4_UPNP_MULTICAST_GROUP);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getMulticastPort() {
        return Constants.UPNP_MULTICAST_PORT;
    }

    @Override
    public int getMulticastResponsePort() {
        return multicastResponsePort > 0 ? multicastResponsePort : DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT;
    }

    @Override
    public int getStreamListenPort() {
        return streamListenPort;
    }

    @Override
    public Iterator<NetworkInterface> getNetworkInterfaces() {
        return new Iterators.Synchronized<>(networkInterfaces) {
            @Override
            protected void synchronizedRemove(int index) {
                synchronized (networkInterfaces) {
                    networkInterfaces.remove(index);
                }
            }
        };
    }

    @Override
    public Iterator<InetAddress> getBindAddresses() {
        return new Iterators.Synchronized<>(bindAddresses) {
            @Override
            protected void synchronizedRemove(int index) {
                synchronized (bindAddresses) {
                    bindAddresses.remove(index);
                }
            }
        };
    }

    @Override
    public boolean hasUsableNetwork() {
        return !networkInterfaces.isEmpty() && !bindAddresses.isEmpty();
    }

    @Override
    public byte[] getHardwareAddress(InetAddress inetAddress) {
        try {
            NetworkInterface iface = NetworkInterface.getByInetAddress(inetAddress);
            return iface != null ? iface.getHardwareAddress() : null;
        } catch (Exception e) {
            logger.warn("Cannot get hardware address for: {}", inetAddress, e);
            // On Win32: java.lang.Error: IP Helper Library GetIpAddrTable function failed

            // On Android 4.0.3 NullPointerException with inetAddress != null

            // On Android "SocketException: No such device or address" when
            // switching networks (mobile -> WiFi)
            return null;
        }
    }

    @Override
    public InetAddress getBroadcastAddress(InetAddress inetAddress) {
        synchronized (networkInterfaces) {
            for (NetworkInterface iface : networkInterfaces) {
                for (InterfaceAddress interfaceAddress : getInterfaceAddresses(iface)) {
                    if (interfaceAddress != null && interfaceAddress.getAddress().equals(inetAddress)) {
                        return interfaceAddress.getBroadcast();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Short getAddressNetworkPrefixLength(InetAddress inetAddress) {
        synchronized (networkInterfaces) {
            for (NetworkInterface iface : networkInterfaces) {
                for (InterfaceAddress interfaceAddress : getInterfaceAddresses(iface)) {
                    if (interfaceAddress != null && interfaceAddress.getAddress().equals(inetAddress)) {
                        short prefix = interfaceAddress.getNetworkPrefixLength();
                        if (prefix > 0 && prefix < 32) {
                            return prefix; // some network cards return -1
                        }
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public InetAddress getLocalAddress(NetworkInterface networkInterface, boolean isIPv6, InetAddress remoteAddress) {

        // First try to find a local IP that is in the same subnet as the remote IP
        InetAddress localIPInSubnet = getBindAddressInSubnetOf(remoteAddress);
        if (localIPInSubnet != null) {
            return localIPInSubnet;
        }

        // There are two reasons why we end up here:
        //
        // - Windows Vista returns a 64 or 128 CIDR prefix if you ask it for the network prefix length of an IPv4
        // address!
        //
        // - We are dealing with genuine IPv6 addresses
        //
        // - Something is really wrong on the LAN and we received a multicast datagram from a source we can't reach via
        // IP
        logger.trace("Could not find local bind address in same subnet as: {}", remoteAddress.getHostAddress());

        // Next, just take the given interface (which is really totally random) and get the first address that we like
        for (InetAddress interfaceAddress : getInetAddresses(networkInterface)) {
            if (isIPv6 && interfaceAddress instanceof Inet6Address) {
                return interfaceAddress;
            }
            if (!isIPv6 && interfaceAddress instanceof Inet4Address) {
                return interfaceAddress;
            }
        }
        throw new IllegalStateException(
                "Can't find any IPv4 or IPv6 address on interface: " + networkInterface.getDisplayName());
    }

    protected List<InterfaceAddress> getInterfaceAddresses(NetworkInterface networkInterface) {
        return networkInterface.getInterfaceAddresses();
    }

    protected List<InetAddress> getInetAddresses(NetworkInterface networkInterface) {
        return Collections.list(networkInterface.getInetAddresses());
    }

    protected InetAddress getBindAddressInSubnetOf(InetAddress inetAddress) {
        synchronized (networkInterfaces) {
            for (NetworkInterface iface : networkInterfaces) {
                for (InterfaceAddress ifaceAddress : getInterfaceAddresses(iface)) {

                    synchronized (bindAddresses) {
                        if (ifaceAddress == null || !bindAddresses.contains(ifaceAddress.getAddress())) {
                            continue;
                        }
                    }

                    if (isInSubnet(inetAddress.getAddress(), ifaceAddress.getAddress().getAddress(),
                            ifaceAddress.getNetworkPrefixLength())) {
                        return ifaceAddress.getAddress();
                    }
                }

            }
        }
        return null;
    }

    protected boolean isInSubnet(byte[] ip, byte[] network, short prefix) {
        if (ip.length != network.length) {
            return false;
        }

        if (prefix / 8 > ip.length) {
            return false;
        }

        int i = 0;
        while (prefix >= 8 && i < ip.length) {
            if (ip[i] != network[i]) {
                return false;
            }
            i++;
            prefix -= 8;
        }
        if (i == ip.length) {
            return true;
        }
        final byte mask = (byte) ~((1 << 8 - prefix) - 1);

        return (ip[i] & mask) == (network[i] & mask);
    }

    protected void discoverNetworkInterfaces() throws InitializationException {
        try {

            Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaceEnumeration)) {
                logger.trace("Analyzing network interface: {}", iface.getDisplayName());
                if (isUsableNetworkInterface(iface)) {
                    logger.trace("Discovered usable network interface: {}", iface.getDisplayName());
                    synchronized (networkInterfaces) {
                        networkInterfaces.add(iface);
                    }
                } else {
                    logger.trace("Ignoring non-usable network interface: {}", iface.getDisplayName());
                }
            }

        } catch (Exception e) {
            throw new InitializationException("Could not not analyze local network interfaces", e);
        }
    }

    /**
     * Validation of every discovered network interface.
     * <p>
     * Override this method to customize which network interfaces are used.
     * </p>
     * <p>
     * The given implementation ignores interfaces which are
     * </p>
     * <ul>
     * <li>loopback (yes, we do not bind to lo0)</li>
     * <li>down</li>
     * <li>have no bound IP addresses</li>
     * <li>named "vmnet*" (OS X VMWare does not properly stop interfaces when it quits)</li>
     * <li>named "vnic*" (OS X Parallels interfaces should be ignored as well)</li>
     * <li>named "vboxnet*" (OS X Virtual Box interfaces should be ignored as well)</li>
     * <li>named "*virtual*" (VirtualBox interfaces, for example</li>
     * <li>named "ppp*"</li>
     * </ul>
     *
     * @param iface The interface to validate.
     * @return True if the given interface matches all validation criteria.
     * @throws Exception If any validation test failed with an un-recoverable error.
     */
    protected boolean isUsableNetworkInterface(NetworkInterface iface) throws Exception {
        if (!iface.isUp()) {
            logger.trace("Skipping network interface (down): {}", iface.getDisplayName());
            return false;
        }

        if (getInetAddresses(iface).isEmpty()) {
            logger.trace("Skipping network interface without bound IP addresses: {}", iface.getDisplayName());
            return false;
        }

        if (iface.isPointToPoint()) {
            logger.trace("Skipping point-to-point network interface: {}", iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).startsWith("vmnet") || (iface.getDisplayName() != null
                && iface.getDisplayName().toLowerCase(Locale.ENGLISH).contains("vmnet"))) {
            logger.trace("Skipping network interface (VMWare): {}", iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).startsWith("vnic")) {
            logger.trace("Skipping network interface (Parallels): {}", iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).startsWith("vboxnet")) {
            logger.trace("Skipping network interface (Virtual Box): {}", iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).contains("virtual")) {
            logger.trace("Skipping network interface (named '*virtual*'): {}", iface.getDisplayName());
            return false;
        }

        if (iface.getName().toLowerCase(Locale.ENGLISH).startsWith("ppp")) {
            logger.trace("Skipping network interface (PPP): {}", iface.getDisplayName());
            return false;
        }

        if (iface.isLoopback()) {
            logger.trace("Skipping network interface (ignoring loopback): {}", iface.getDisplayName());
            return false;
        }

        if (!useInterfaces.isEmpty() && !useInterfaces.contains(iface.getName())) {
            logger.trace("Skipping unwanted network interface (OSGi parameter 'interfaces' or -D {}): {}",
                    SYSTEM_PROPERTY_NET_IFACES, iface.getName());
            return false;
        }

        if (!iface.supportsMulticast()) {
            logger.warn("Network interface may not be multicast capable: {}", iface.getDisplayName());
        }

        return true;
    }

    protected void discoverBindAddresses() throws InitializationException {
        try {

            synchronized (networkInterfaces) {
                Iterator<NetworkInterface> it = networkInterfaces.iterator();
                while (it.hasNext()) {
                    NetworkInterface networkInterface = it.next();

                    logger.trace("Discovering addresses of interface: {}", networkInterface.getDisplayName());
                    int usableAddresses = 0;
                    for (InetAddress inetAddress : getInetAddresses(networkInterface)) {
                        if (inetAddress == null) {
                            logger.warn("Network has a null address: {}", networkInterface.getDisplayName());
                            continue;
                        }

                        if (isUsableAddress(networkInterface, inetAddress)) {
                            logger.trace("Discovered usable network interface address: {}",
                                    inetAddress.getHostAddress());
                            usableAddresses++;
                            synchronized (bindAddresses) {
                                bindAddresses.add(inetAddress);
                            }
                        } else {
                            logger.trace("Ignoring non-usable network interface address: {}",
                                    inetAddress.getHostAddress());
                        }
                    }

                    if (usableAddresses == 0) {
                        logger.trace("Network interface has no usable addresses, removing: {}",
                                networkInterface.getDisplayName());
                        it.remove();
                    }
                }
            }

        } catch (Exception e) {
            throw new InitializationException("Could not not analyze local network interfaces", e);
        }
    }

    /**
     * Validation of every discovered local address.
     * <p>
     * Override this method to customize which network addresses are used.
     * </p>
     * <p>
     * The given implementation ignores addresses which are
     * </p>
     * <ul>
     * <li>not IPv4</li>
     * <li>the local loopback (yes, we ignore 127.0.0.1)</li>
     * </ul>
     *
     * @param networkInterface The interface to validate.
     * @param address The address of this interface to validate.
     * @return True if the given address matches all validation criteria.
     */
    protected boolean isUsableAddress(NetworkInterface networkInterface, InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            logger.trace("Skipping unsupported non-IPv4 address: {}", address);
            return false;
        }

        if (address.isLoopbackAddress()) {
            logger.trace("Skipping loopback address: {}", address);
            return false;
        }

        if (!useAddresses.isEmpty() && !useAddresses.contains(address.getHostAddress())) {
            logger.trace("Skipping unwanted address: {}", address);
            return false;
        }

        return true;
    }

    protected void logInterfaceInformation(NetworkInterface networkInterface) throws SocketException {
        logger.info("---------------------------------------------------------------------------------");
        logger.info("Interface display name: {}", networkInterface.getDisplayName());
        if (networkInterface.getParent() != null) {
            logger.info("Parent Info: {}", networkInterface.getParent());
        }
        logger.info("Name: {}", networkInterface.getName());

        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();

        for (InetAddress inetAddress : Collections.list(inetAddresses)) {
            logger.info("InetAddress: {}", inetAddress);
        }

        List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();

        for (InterfaceAddress interfaceAddress : interfaceAddresses) {
            if (interfaceAddress == null) {
                logger.warn("Skipping null InterfaceAddress!");
                continue;
            }
            logger.info(" Interface Address");
            logger.info("  Address: {}", interfaceAddress.getAddress());
            logger.info("  Broadcast: {}", interfaceAddress.getBroadcast());
            logger.info("  Prefix length: {}", interfaceAddress.getNetworkPrefixLength());
        }

        Enumeration<NetworkInterface> subIfs = networkInterface.getSubInterfaces();

        for (NetworkInterface subIf : Collections.list(subIfs)) {
            if (subIf == null) {
                logger.warn("Skipping null NetworkInterface sub-interface");
                continue;
            }
            logger.info("\tSub Interface Display name: {}", subIf.getDisplayName());
            logger.info("\tSub Interface Name: {}", subIf.getName());
        }
        logger.info("Up? {}", networkInterface.isUp());
        logger.info("Loopback? {}", networkInterface.isLoopback());
        logger.info("PointToPoint? {}", networkInterface.isPointToPoint());
        logger.info("Supports multicast? {}", networkInterface.supportsMulticast());
        logger.info("Virtual? {}", networkInterface.isVirtual());
        logger.info("Hardware address: {}", Arrays.toString(networkInterface.getHardwareAddress()));
        logger.info("MTU: {}", networkInterface.getMTU());
    }
}
