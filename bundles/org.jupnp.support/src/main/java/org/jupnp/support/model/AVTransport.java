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
package org.jupnp.support.model;

import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.lastchange.LastChange;

/**
 * State of one logical instance of the AV Transport service.
 *
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class AVTransport {

    protected final UnsignedIntegerFourBytes instanceID;
    protected final LastChange lastChange;
    protected MediaInfo mediaInfo;
    protected TransportInfo transportInfo;
    protected PositionInfo positionInfo;
    protected DeviceCapabilities deviceCapabilities;
    protected TransportSettings transportSettings;

    public AVTransport(UnsignedIntegerFourBytes instanceID, LastChange lastChange, StorageMedium possiblePlayMedium) {
        this(instanceID, lastChange, new StorageMedium[] { possiblePlayMedium });
    }

    public AVTransport(UnsignedIntegerFourBytes instanceID, LastChange lastChange, StorageMedium[] possiblePlayMedia) {
        this.instanceID = instanceID;
        this.lastChange = lastChange;
        setDeviceCapabilities(new DeviceCapabilities(possiblePlayMedia));
        setMediaInfo(new MediaInfo());
        setTransportInfo(new TransportInfo());
        setPositionInfo(new PositionInfo());
        setTransportSettings(new TransportSettings());
    }

    public UnsignedIntegerFourBytes getInstanceId() {
        return instanceID;
    }

    public LastChange getLastChange() {
        return lastChange;
    }

    public MediaInfo getMediaInfo() {
        return mediaInfo;
    }

    public void setMediaInfo(MediaInfo mediaInfo) {
        this.mediaInfo = mediaInfo;
    }

    public TransportInfo getTransportInfo() {
        return transportInfo;
    }

    public void setTransportInfo(TransportInfo transportInfo) {
        this.transportInfo = transportInfo;
    }

    public PositionInfo getPositionInfo() {
        return positionInfo;
    }

    public void setPositionInfo(PositionInfo positionInfo) {
        this.positionInfo = positionInfo;
    }

    public DeviceCapabilities getDeviceCapabilities() {
        return deviceCapabilities;
    }

    public void setDeviceCapabilities(DeviceCapabilities deviceCapabilities) {
        this.deviceCapabilities = deviceCapabilities;
    }

    public TransportSettings getTransportSettings() {
        return transportSettings;
    }

    public void setTransportSettings(TransportSettings transportSettings) {
        this.transportSettings = transportSettings;
    }
}
