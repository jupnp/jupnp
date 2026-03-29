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

/**
 * Encpasulates optional metadata about a device's manufacturer.
 *
 * @author Christian Bauer
 */
public class ManufacturerDetails {

    private String manufacturer;
    private URI manufacturerURI;

    ManufacturerDetails() {
    }

    public ManufacturerDetails(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public ManufacturerDetails(URI manufacturerURI) {
        this.manufacturerURI = manufacturerURI;
    }

    public ManufacturerDetails(String manufacturer, URI manufacturerURI) {
        this.manufacturer = manufacturer;
        this.manufacturerURI = manufacturerURI;
    }

    public ManufacturerDetails(String manufacturer, String manufacturerURI) throws IllegalArgumentException {
        this.manufacturer = manufacturer;
        this.manufacturerURI = URI.create(manufacturerURI);
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public URI getManufacturerURI() {
        return manufacturerURI;
    }
}
