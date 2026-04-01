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
package org.jupnp.model.meta;

import java.net.InetAddress;
import java.net.URL;

import org.jupnp.model.ModelUtil;
import org.jupnp.model.message.discovery.IncomingNotificationRequest;
import org.jupnp.model.message.discovery.IncomingSearchResponse;
import org.jupnp.model.types.UDN;

/**
 * Additional identifying information only relevant for discovered remote devices.
 * <p>
 * This information always includes the URL of the device's descriptor, and the
 * local network interface address we should use in the future, because it is
 * guaranteed to be reachable by this remote device (e.g. when we build a local
 * callback URL).
 * </p>
 * <p>
 * Optional is the remote hosts interface MAC hardware address. If we have it, we
 * can use it to send Wake-On-LAN broadcasts if we think the remote host is not
 * reachable and might be sleeping. (Useful for "stateless" reconnecting control
 * points.)
 * </p>
 *
 * @author Christian Bauer
 */
public class RemoteDeviceIdentity extends DeviceIdentity {

    private final URL descriptorURL;
    private final byte[] interfaceMacAddress;
    private final InetAddress discoveredOnLocalAddress;

    public RemoteDeviceIdentity(UDN udn, RemoteDeviceIdentity template) {
        this(udn, template.getMaxAgeSeconds(), template.getDescriptorURL(), template.getInterfaceMacAddress(),
                template.getDiscoveredOnLocalAddress());
    }

    public RemoteDeviceIdentity(UDN udn, Integer maxAgeSeconds, URL descriptorURL, byte[] interfaceMacAddress,
            InetAddress discoveredOnLocalAddress) {
        super(udn, maxAgeSeconds);
        this.descriptorURL = descriptorURL;
        this.interfaceMacAddress = interfaceMacAddress;
        this.discoveredOnLocalAddress = discoveredOnLocalAddress;
    }

    public RemoteDeviceIdentity(IncomingNotificationRequest notificationRequest) {
        this(notificationRequest.getUDN(), notificationRequest.getMaxAge(), notificationRequest.getLocationURL(),
                notificationRequest.getInterfaceMacHeader(), notificationRequest.getLocalAddress());
    }

    public RemoteDeviceIdentity(IncomingSearchResponse searchResponse) {
        this(searchResponse.getRootDeviceUDN(), searchResponse.getMaxAge(), searchResponse.getLocationURL(),
                searchResponse.getInterfaceMacHeader(), searchResponse.getLocalAddress());
    }

    public URL getDescriptorURL() {
        return descriptorURL;
    }

    public byte[] getInterfaceMacAddress() {
        return interfaceMacAddress;
    }

    public InetAddress getDiscoveredOnLocalAddress() {
        return discoveredOnLocalAddress;
    }

    public byte[] getWakeOnLANBytes() {
        if (getInterfaceMacAddress() == null) {
            return null;
        }
        byte[] bytes = new byte[6 + 16 * getInterfaceMacAddress().length];
        for (int i = 0; i < 6; i++) {
            bytes[i] = (byte) 0xff;
        }
        for (int i = 6; i < bytes.length; i += getInterfaceMacAddress().length) {
            System.arraycopy(getInterfaceMacAddress(), 0, bytes, i, getInterfaceMacAddress().length);
        }
        return bytes;
    }

    @Override
    public String toString() {
        // Performance optimization, so we don't have to wrap all log("foo " + device) calls with isLoggable
        if (ModelUtil.ANDROID_RUNTIME) {
            return "(RemoteDeviceIdentity) UDN: " + getUdn() + ", Descriptor: " + getDescriptorURL();
        }
        return "(" + getClass().getSimpleName() + ") UDN: " + getUdn() + ", Descriptor: " + getDescriptorURL();
    }
}
