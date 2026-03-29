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
package org.jupnp.binding.staging;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.Service;
import org.jupnp.model.meta.UDAVersion;
import org.jupnp.model.types.DLNACaps;
import org.jupnp.model.types.DLNADoc;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDN;

/**
 * @author Christian Bauer
 */
public class MutableDevice {

    public UDN udn;
    public MutableUDAVersion udaVersion = new MutableUDAVersion();
    public URL baseURL;
    public String deviceType;
    public String friendlyName;
    public String manufacturer;
    public URI manufacturerURI;
    public String modelName;
    public String modelDescription;
    public String modelNumber;
    public URI modelURI;
    public String serialNumber;
    public String upc;
    public URI presentationURI;
    public List<DLNADoc> dlnaDocs = new ArrayList<>();
    public DLNACaps dlnaCaps;
    public List<MutableIcon> icons = new ArrayList<>();
    public List<MutableService> services = new ArrayList<>();
    public List<MutableDevice> embeddedDevices = new ArrayList<>();
    public MutableDevice parentDevice;

    public Device build(Device prototype) throws ValidationException {
        // Note how all embedded devices inherit the version and baseURL of the root!
        return build(prototype, createDeviceVersion(), baseURL);
    }

    public Device build(Device prototype, UDAVersion deviceVersion, URL baseURL) throws ValidationException {

        List<Device> embeddedDevicesList = new ArrayList<>();
        for (MutableDevice embeddedDevice : embeddedDevices) {
            embeddedDevicesList.add(embeddedDevice.build(prototype, deviceVersion, baseURL));
        }
        return prototype.newInstance(udn, deviceVersion, createDeviceType(), createDeviceDetails(baseURL),
                createIcons(), createServices(prototype), embeddedDevicesList);
    }

    public UDAVersion createDeviceVersion() {
        return new UDAVersion(udaVersion.major, udaVersion.minor);
    }

    public DeviceType createDeviceType() {
        return DeviceType.valueOf(deviceType);
    }

    public DeviceDetails createDeviceDetails(URL baseURL) {
        return new DeviceDetails(baseURL, friendlyName, new ManufacturerDetails(manufacturer, manufacturerURI),
                new ModelDetails(modelName, modelDescription, modelNumber, modelURI), serialNumber, upc,
                presentationURI, dlnaDocs.toArray(new DLNADoc[dlnaDocs.size()]), dlnaCaps);
    }

    public Icon[] createIcons() {
        Icon[] iconArray = new Icon[icons.size()];
        int i = 0;
        for (MutableIcon icon : icons) {
            iconArray[i++] = icon.build();
        }
        return iconArray;
    }

    public Service[] createServices(Device prototype) throws ValidationException {
        Service[] services = prototype.newServiceArray(this.services.size());
        int i = 0;
        for (MutableService service : this.services) {
            services[i++] = service.build(prototype);
        }
        return services;
    }
}
