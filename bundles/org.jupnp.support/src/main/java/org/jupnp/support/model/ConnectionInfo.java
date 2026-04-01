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

import java.util.Objects;

import org.jupnp.model.ServiceReference;

/**
 * Immutable type encapsulating the state of a single connection.
 *
 * @author Alessio Gaeta
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class ConnectionInfo {

    public enum Status {
        OK,
        ContentFormatMismatch,
        InsufficientBandwidth,
        UnreliableChannel,
        Unknown
    }

    public enum Direction {
        Output,
        Input;

        public Direction getOpposite() {
            return this.equals(Output) ? Input : Output;
        }
    }

    protected final int connectionID;

    protected final int rcsID;
    protected final int avTransportID;

    protected final ProtocolInfo protocolInfo;

    protected final ServiceReference peerConnectionManager;
    protected final int peerConnectionID;

    protected final Direction direction;
    protected Status connectionStatus = Status.Unknown;

    /**
     * Creates a default instance with values expected for the default connection ID "0".
     * <p>
     * The ConnectionManager 1.0 specification says:
     * </p>
     * <p>
     * If optional action PrepareForConnection is not implemented then (limited) connection
     * information can be retrieved for ConnectionID 0. The device should return all known
     * information:
     * </p>
     * <ul>
     * <li>RcsID should be 0 or -1</li>
     * <li>AVTransportID should be 0 or -1</li>
     * <li>ProtocolInfo should contain accurate information if it is known, otherwhise
     * it should be NULL (empty string)</li>
     * <li>PeerConnectionManager should be NULL (empty string)</li>
     * <li>PeerConnectionID should be -1</li>
     * <li>Direction should be Input or Output</li>
     * <li>Status should be OK or Unknown</li>
     * </ul>
     */
    public ConnectionInfo() {
        this(0, 0, 0, null, null, -1, Direction.Input, Status.Unknown);
    }

    public ConnectionInfo(int connectionID, int rcsID, int avTransportID, ProtocolInfo protocolInfo,
            ServiceReference peerConnectionManager, int peerConnectionID, Direction direction,
            Status connectionStatus) {
        this.connectionID = connectionID;
        this.rcsID = rcsID;
        this.avTransportID = avTransportID;
        this.protocolInfo = protocolInfo;
        this.peerConnectionManager = peerConnectionManager;
        this.peerConnectionID = peerConnectionID;
        this.direction = direction;
        this.connectionStatus = connectionStatus;
    }

    public int getConnectionID() {
        return connectionID;
    }

    public int getRcsID() {
        return rcsID;
    }

    public int getAvTransportID() {
        return avTransportID;
    }

    public ProtocolInfo getProtocolInfo() {
        return protocolInfo;
    }

    public ServiceReference getPeerConnectionManager() {
        return peerConnectionManager;
    }

    public int getPeerConnectionID() {
        return peerConnectionID;
    }

    public Direction getDirection() {
        return direction;
    }

    public synchronized Status getConnectionStatus() {
        return connectionStatus;
    }

    public synchronized void setConnectionStatus(Status connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConnectionInfo that = (ConnectionInfo) o;

        if (avTransportID != that.avTransportID) {
            return false;
        }
        if (connectionID != that.connectionID) {
            return false;
        }
        if (peerConnectionID != that.peerConnectionID) {
            return false;
        }
        if (rcsID != that.rcsID) {
            return false;
        }
        if (connectionStatus != that.connectionStatus) {
            return false;
        }
        if (direction != that.direction) {
            return false;
        }
        if (!Objects.equals(peerConnectionManager, that.peerConnectionManager)) {
            return false;
        }
        if (!Objects.equals(protocolInfo, that.protocolInfo)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = connectionID;
        result = 31 * result + rcsID;
        result = 31 * result + avTransportID;
        result = 31 * result + (protocolInfo != null ? protocolInfo.hashCode() : 0);
        result = 31 * result + (peerConnectionManager != null ? peerConnectionManager.hashCode() : 0);
        result = 31 * result + peerConnectionID;
        result = 31 * result + direction.hashCode();
        result = 31 * result + connectionStatus.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") ID: " + getConnectionID() + ", Status: " + getConnectionStatus();
    }
}
