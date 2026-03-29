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
package org.jupnp.osgi.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.Service;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPService;

public class UPnPDeviceImpl implements UPnPDevice {
    private Device<?, ?, ?> device;
    private UPnPServiceImpl[] services;
    private Hashtable<String, UPnPService> servicesIndex;
    private UPnPIconImpl[] icons;
    private Dictionary<String, Object> descriptions = new Hashtable<>();

    public UPnPDeviceImpl(Device<?, ?, ?> device) {
        this.device = device;
        DeviceDetails deviceDetails = device.getDetails();
        ManufacturerDetails manufacturerDetails = deviceDetails.getManufacturerDetails();
        ModelDetails modelDetails = deviceDetails.getModelDetails();

        /* DEVICE CATEGORY */
        descriptions.put(org.osgi.service.device.Constants.DEVICE_CATEGORY,
                new String[] { UPnPDevice.DEVICE_CATEGORY });

        // mandatory properties
        if (!device.isRoot()) {
            Device<?, ?, ?> parent = device.getParentDevice();
            descriptions.put(UPnPDevice.PARENT_UDN, parent.getIdentity().getUdn().toString());
        }

        if (device.getEmbeddedDevices() != null) {
            List<String> list = new ArrayList<>();

            for (Device<?, ?, ?> embedded : device.getEmbeddedDevices()) {
                list.add(embedded.getIdentity().getUdn().toString());
            }

            descriptions.put(UPnPDevice.CHILDREN_UDN, list.toArray(new String[0]));
        }

        descriptions.put(UPnPDevice.FRIENDLY_NAME, deviceDetails.getFriendlyName());
        descriptions.put(UPnPDevice.MANUFACTURER, manufacturerDetails.getManufacturer());
        descriptions.put(UPnPDevice.TYPE, device.getType().toString());
        descriptions.put(UPnPDevice.UDN, device.getIdentity().getUdn().toString());

        // optional properties (but recommended)
        if (modelDetails.getModelDescription() != null) {
            descriptions.put(UPnPDevice.MODEL_DESCRIPTION, modelDetails.getModelDescription());
        }
        if (modelDetails.getModelNumber() != null) {
            descriptions.put(UPnPDevice.MODEL_NUMBER, modelDetails.getModelNumber());
        }
        if (deviceDetails.getPresentationURI() != null) {
            descriptions.put(UPnPDevice.PRESENTATION_URL, deviceDetails.getPresentationURI().toString());
        }
        if (deviceDetails.getSerialNumber() != null) {
            descriptions.put(UPnPDevice.SERIAL_NUMBER, deviceDetails.getSerialNumber());
        }

        // optional properties
        if (manufacturerDetails.getManufacturerURI() != null) {
            descriptions.put(UPnPDevice.MANUFACTURER_URL, manufacturerDetails.getManufacturerURI().toString());
        }
        if (modelDetails.getModelName() != null) {
            descriptions.put(UPnPDevice.MODEL_NAME, modelDetails.getModelName());
        }
        if (modelDetails.getModelURI() != null) {
            descriptions.put(UPnPDevice.MODEL_URL, modelDetails.getModelURI().toString());
        }
        if (deviceDetails.getUpc() != null) {
            descriptions.put(UPnPDevice.UPC, deviceDetails.getUpc());
        }

        if (device.getServices() != null && device.getServices().length != 0) {
            List<UPnPServiceImpl> list = new ArrayList<>();
            servicesIndex = new Hashtable<>();

            for (Service<?, ?> service : device.getServices()) {
                UPnPServiceImpl item = new UPnPServiceImpl(service);
                list.add(item);
                servicesIndex.put(item.getId(), item);
            }

            services = list.toArray(new UPnPServiceImpl[0]);
        }

        if (device.getIcons() != null && device.getIcons().length != 0) {
            List<UPnPIconImpl> list = new ArrayList<>();

            for (Icon icon : device.getIcons()) {
                UPnPIconImpl item = new UPnPIconImpl(icon);
                list.add(item);
            }

            icons = list.toArray(new UPnPIconImpl[0]);
        }
    }

    @Override
    public UPnPService getService(String serviceId) {
        return servicesIndex != null ? servicesIndex.get(serviceId) : null;
    }

    @Override
    public UPnPService[] getServices() {
        return services;
    }

    @Override
    public UPnPIcon[] getIcons(String locale) {
        return icons;
    }

    @Override
    public Dictionary getDescriptions(String locale) {
        return descriptions;
    }

    public Device<?, ?, ?> getDevice() {
        return device;
    }
}
