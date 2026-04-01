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
 * Encpasulates optional metadata about the model of a device.
 *
 * @author Christian Bauer
 */
public class ModelDetails {

    private String modelName;
    private String modelDescription;
    private String modelNumber;
    private URI modelURI;

    ModelDetails() {
    }

    public ModelDetails(String modelName) {
        this.modelName = modelName;
    }

    public ModelDetails(String modelName, String modelDescription) {
        this.modelName = modelName;
        this.modelDescription = modelDescription;
    }

    public ModelDetails(String modelName, String modelDescription, String modelNumber) {
        this.modelName = modelName;
        this.modelDescription = modelDescription;
        this.modelNumber = modelNumber;
    }

    public ModelDetails(String modelName, String modelDescription, String modelNumber, URI modelURI) {
        this.modelName = modelName;
        this.modelDescription = modelDescription;
        this.modelNumber = modelNumber;
        this.modelURI = modelURI;
    }

    public ModelDetails(String modelName, String modelDescription, String modelNumber, String modelURI)
            throws IllegalArgumentException {
        this.modelName = modelName;
        this.modelDescription = modelDescription;
        this.modelNumber = modelNumber;
        this.modelURI = URI.create(modelURI);
    }

    public String getModelName() {
        return modelName;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public URI getModelURI() {
        return modelURI;
    }
}
