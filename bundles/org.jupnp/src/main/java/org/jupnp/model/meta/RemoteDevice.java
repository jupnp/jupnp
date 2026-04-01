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

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jupnp.model.Namespace;
import org.jupnp.model.ValidationException;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.resource.ServiceEventCallbackResource;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.util.URIUtil;

/**
 * The metadata of a device that was discovered on the network.
 *
 * @author Christian Bauer
 */
public class RemoteDevice extends Device<RemoteDeviceIdentity, RemoteDevice, RemoteService> {

    public RemoteDevice(RemoteDeviceIdentity identity) throws ValidationException {
        super(identity);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, RemoteService service)
            throws ValidationException {
        super(identity, type, details, null, new RemoteService[] { service });
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, RemoteService service,
            RemoteDevice embeddedDevice) throws ValidationException {
        super(identity, type, details, null, new RemoteService[] { service }, new RemoteDevice[] { embeddedDevice });
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, RemoteService[] services)
            throws ValidationException {
        super(identity, type, details, null, services);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, RemoteService[] services,
            RemoteDevice[] embeddedDevices) throws ValidationException {
        super(identity, type, details, null, services, embeddedDevices);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, Icon icon,
            RemoteService service) throws ValidationException {
        super(identity, type, details, new Icon[] { icon }, new RemoteService[] { service });
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, Icon icon,
            RemoteService service, RemoteDevice embeddedDevice) throws ValidationException {
        super(identity, type, details, new Icon[] { icon }, new RemoteService[] { service },
                new RemoteDevice[] { embeddedDevice });
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, Icon icon,
            RemoteService[] services) throws ValidationException {
        super(identity, type, details, new Icon[] { icon }, services);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, Icon icon,
            RemoteService[] services, RemoteDevice[] embeddedDevices) throws ValidationException {
        super(identity, type, details, new Icon[] { icon }, services, embeddedDevices);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, Icon[] icons,
            RemoteService service) throws ValidationException {
        super(identity, type, details, icons, new RemoteService[] { service });
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, Icon[] icons,
            RemoteService service, RemoteDevice embeddedDevice) throws ValidationException {
        super(identity, type, details, icons, new RemoteService[] { service }, new RemoteDevice[] { embeddedDevice });
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, Icon[] icons,
            RemoteService[] services) throws ValidationException {
        super(identity, type, details, icons, services);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, DeviceType type, DeviceDetails details, Icon[] icons,
            RemoteService[] services, RemoteDevice[] embeddedDevices) throws ValidationException {
        super(identity, type, details, icons, services, embeddedDevices);
    }

    public RemoteDevice(RemoteDeviceIdentity identity, UDAVersion version, DeviceType type, DeviceDetails details,
            Icon[] icons, RemoteService[] services, RemoteDevice[] embeddedDevices) throws ValidationException {
        super(identity, version, type, details, icons, services, embeddedDevices);
    }

    @Override
    public RemoteService[] getServices() {
        return this.services != null ? this.services : new RemoteService[0];
    }

    @Override
    public RemoteDevice[] getEmbeddedDevices() {
        return this.embeddedDevices != null ? this.embeddedDevices : new RemoteDevice[0];
    }

    public URL normalizeURI(URI relativeOrAbsoluteURI) {

        // TODO: I have one device (Netgear 834DG DSL Router) that sends a <URLBase>, and even that is wrong (port)!
        // This can be fixed by "re-enabling" UPnP in the upnpService after a reboot, it will then use the right port...
        // return URIUtil.createAbsoluteURL(getDescriptorURL(), relativeOrAbsoluteURI);

        if (getDetails() != null && getDetails().getBaseURL() != null) {
            // If we have an <URLBase>, all URIs are relative to it
            return URIUtil.createAbsoluteURL(getDetails().getBaseURL(), relativeOrAbsoluteURI);
        } else {
            // Otherwise, they are relative to the descriptor location
            return URIUtil.createAbsoluteURL(getIdentity().getDescriptorURL(), relativeOrAbsoluteURI);
        }
    }

    @Override
    public RemoteDevice newInstance(UDN udn, UDAVersion version, DeviceType type, DeviceDetails details, Icon[] icons,
            RemoteService[] services, List<RemoteDevice> embeddedDevices) throws ValidationException {
        return new RemoteDevice(new RemoteDeviceIdentity(udn, getIdentity()), version, type, details, icons, services,
                !embeddedDevices.isEmpty() ? embeddedDevices.toArray(new RemoteDevice[embeddedDevices.size()]) : null);
    }

    @Override
    public RemoteService newInstance(ServiceType serviceType, ServiceId serviceId, URI descriptorURI, URI controlURI,
            URI eventSubscriptionURI, Action<RemoteService>[] actions, StateVariable<RemoteService>[] stateVariables)
            throws ValidationException {
        return new RemoteService(serviceType, serviceId, descriptorURI, controlURI, eventSubscriptionURI, actions,
                stateVariables);
    }

    @Override
    public RemoteDevice[] toDeviceArray(Collection<RemoteDevice> col) {
        return col.toArray(new RemoteDevice[col.size()]);
    }

    @Override
    public RemoteService[] newServiceArray(int size) {
        return new RemoteService[size];
    }

    @Override
    public RemoteService[] toServiceArray(Collection<RemoteService> col) {
        return col.toArray(new RemoteService[col.size()]);
    }

    @Override
    public Resource[] discoverResources(Namespace namespace) {
        List<Resource> discovered = new ArrayList<>();

        // Services
        for (RemoteService service : getServices()) {
            if (service == null) {
                continue;
            }
            discovered.add(new ServiceEventCallbackResource(namespace.getEventCallbackPath(service), service));
        }

        // Embedded devices
        if (hasEmbeddedDevices()) {
            for (Device embeddedDevice : getEmbeddedDevices()) {
                if (embeddedDevice == null) {
                    continue;
                }
                discovered.addAll(Arrays.asList(embeddedDevice.discoverResources(namespace)));
            }
        }

        return discovered.toArray(new Resource[discovered.size()]);
    }

    @Override
    public RemoteDevice getRoot() {
        if (isRoot()) {
            return this;
        }
        RemoteDevice current = this;
        while (current.getParentDevice() != null) {
            current = current.getParentDevice();
        }
        return current;
    }

    @Override
    public RemoteDevice findDevice(UDN udn) {
        return find(udn, this);
    }
}
