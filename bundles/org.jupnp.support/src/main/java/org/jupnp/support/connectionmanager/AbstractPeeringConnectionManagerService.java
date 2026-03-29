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
package org.jupnp.support.connectionmanager;

import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.controlpoint.ControlPoint;
import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.ServiceReference;
import org.jupnp.model.action.ActionException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.csv.CSV;
import org.jupnp.support.connectionmanager.callback.ConnectionComplete;
import org.jupnp.support.connectionmanager.callback.PrepareForConnection;
import org.jupnp.support.model.ConnectionInfo;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.support.model.ProtocolInfos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for setup and teardown of an arbitrary number of connections with a manager peer.
 *
 * @author Christian Bauer
 * @author Alessio Gaeta
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class AbstractPeeringConnectionManagerService extends ConnectionManagerService {

    private final Logger logger = LoggerFactory.getLogger(AbstractPeeringConnectionManagerService.class);

    protected AbstractPeeringConnectionManagerService(ConnectionInfo... activeConnections) {
        super(activeConnections);
    }

    protected AbstractPeeringConnectionManagerService(ProtocolInfos sourceProtocolInfo, ProtocolInfos sinkProtocolInfo,
            ConnectionInfo... activeConnections) {
        super(sourceProtocolInfo, sinkProtocolInfo, activeConnections);
    }

    protected AbstractPeeringConnectionManagerService(PropertyChangeSupport propertyChangeSupport,
            ProtocolInfos sourceProtocolInfo, ProtocolInfos sinkProtocolInfo, ConnectionInfo... activeConnections) {
        super(propertyChangeSupport, sourceProtocolInfo, sinkProtocolInfo, activeConnections);
    }

    protected synchronized int getNewConnectionId() {
        int currentHighestID = -1;
        for (Integer key : activeConnections.keySet()) {
            if (key > currentHighestID) {
                currentHighestID = key;
            }
        }
        return ++currentHighestID;
    }

    protected synchronized void storeConnection(ConnectionInfo info) {
        CSV<UnsignedIntegerFourBytes> oldConnectionIDs = getCurrentConnectionIDs();
        activeConnections.put(info.getConnectionID(), info);
        logger.debug("Connection stored, firing event: {}", info.getConnectionID());
        CSV<UnsignedIntegerFourBytes> newConnectionIDs = getCurrentConnectionIDs();
        getPropertyChangeSupport().firePropertyChange("CurrentConnectionIDs", oldConnectionIDs, newConnectionIDs);
    }

    protected synchronized void removeConnection(int connectionID) {
        CSV<UnsignedIntegerFourBytes> oldConnectionIDs = getCurrentConnectionIDs();
        activeConnections.remove(connectionID);
        logger.debug("Connection removed, firing event: {}", connectionID);
        CSV<UnsignedIntegerFourBytes> newConnectionIDs = getCurrentConnectionIDs();
        getPropertyChangeSupport().firePropertyChange("CurrentConnectionIDs", oldConnectionIDs, newConnectionIDs);
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "ConnectionID", stateVariable = "A_ARG_TYPE_ConnectionID", getterName = "getConnectionID"),
            @UpnpOutputArgument(name = "AVTransportID", stateVariable = "A_ARG_TYPE_AVTransportID", getterName = "getAvTransportID"),
            @UpnpOutputArgument(name = "RcsID", stateVariable = "A_ARG_TYPE_RcsID", getterName = "getRcsID") })
    public synchronized ConnectionInfo prepareForConnection(
            @UpnpInputArgument(name = "RemoteProtocolInfo", stateVariable = "A_ARG_TYPE_ProtocolInfo") ProtocolInfo remoteProtocolInfo,
            @UpnpInputArgument(name = "PeerConnectionManager", stateVariable = "A_ARG_TYPE_ConnectionManager") ServiceReference peerConnectionManager,
            @UpnpInputArgument(name = "PeerConnectionID", stateVariable = "A_ARG_TYPE_ConnectionID") int peerConnectionId,
            @UpnpInputArgument(name = "Direction", stateVariable = "A_ARG_TYPE_Direction") String direction)
            throws ActionException {

        int connectionId = getNewConnectionId();

        ConnectionInfo.Direction dir;
        try {
            dir = ConnectionInfo.Direction.valueOf(direction);
        } catch (Exception e) {
            throw new ConnectionManagerException(ErrorCode.ARGUMENT_VALUE_INVALID,
                    "Unsupported direction: " + direction);
        }

        logger.debug("Preparing for connection with local new ID {} and peer connection ID: {}", connectionId,
                peerConnectionId);

        ConnectionInfo newConnectionInfo = createConnection(connectionId, peerConnectionId, peerConnectionManager, dir,
                remoteProtocolInfo);

        storeConnection(newConnectionInfo);

        return newConnectionInfo;
    }

    @UpnpAction
    public synchronized void connectionComplete(
            @UpnpInputArgument(name = "ConnectionID", stateVariable = "A_ARG_TYPE_ConnectionID") int connectionID)
            throws ActionException {
        ConnectionInfo info = getCurrentConnectionInfo(connectionID);
        logger.debug("Closing connection ID {}", connectionID);
        closeConnection(info);
        removeConnection(connectionID);
    }

    /**
     * Generate a new local connection identifier, prepare the peer, store connection details.
     *
     * @return <code>-1</code> if the
     *         {@link #peerFailure(org.jupnp.model.action.ActionInvocation, org.jupnp.model.message.UpnpResponse, String)}
     *         method had to be called, otherwise the local identifier of the established connection.
     */
    public synchronized int createConnectionWithPeer(final ServiceReference localServiceReference,
            final ControlPoint controlPoint, final Service<?, ?> peerService, final ProtocolInfo protInfo,
            final ConnectionInfo.Direction direction) {

        // It is important that you synchronize the whole procedure, starting with getNewConnectionID(),
        // then preparing the connection on the peer, then storeConnection()

        final int localConnectionID = getNewConnectionId();

        logger.debug("Creating new connection ID {} with peer: {}", localConnectionID, peerService);
        final boolean[] failed = new boolean[1];
        new PrepareForConnection(peerService, controlPoint, protInfo, localServiceReference, localConnectionID,
                direction) {
            @Override
            public void received(ActionInvocation<?> invocation, int peerConnectionID, int rcsID, int avTransportID) {
                ConnectionInfo info = new ConnectionInfo(localConnectionID, rcsID, avTransportID, protInfo,
                        peerService.getReference(), peerConnectionID, direction.getOpposite(), // If I prepared you for
                                                                                               // output, then I do
                                                                                               // input
                        ConnectionInfo.Status.OK);
                storeConnection(info);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                AbstractPeeringConnectionManagerService.this.peerFailure(invocation, operation, defaultMsg);
                failed[0] = true;
            }
        }.run(); // Synchronous execution! We "reserved" a new connection ID earlier!

        return failed[0] ? -1 : localConnectionID;
    }

    /**
     * Close the connection with the peer, remove the connection details.
     */
    public synchronized void closeConnectionWithPeer(ControlPoint controlPoint, Service<?, ?> peerService,
            int connectionID) throws ActionException {
        closeConnectionWithPeer(controlPoint, peerService, getCurrentConnectionInfo(connectionID));
    }

    /**
     * Close the connection with the peer, remove the connection details.
     */
    public synchronized void closeConnectionWithPeer(final ControlPoint controlPoint, final Service<?, ?> peerService,
            final ConnectionInfo connectionInfo) throws ActionException {

        // It is important that you synchronize the whole procedure
        logger.debug("Closing connection ID {} with peer: {}", connectionInfo.getConnectionID(), peerService);
        new ConnectionComplete(peerService, controlPoint, connectionInfo.getPeerConnectionID()) {

            @Override
            public void success(ActionInvocation invocation) {
                removeConnection(connectionInfo.getConnectionID());
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                AbstractPeeringConnectionManagerService.this.peerFailure(invocation, operation, defaultMsg);
            }
        }.run(); // Synchronous execution!
    }

    protected abstract ConnectionInfo createConnection(int connectionID, int peerConnectionId,
            ServiceReference peerConnectionManager, ConnectionInfo.Direction direction, ProtocolInfo protocolInfo)
            throws ActionException;

    protected abstract void closeConnection(ConnectionInfo connectionInfo);

    /**
     * Called when connection creation or closing with a peer failed.
     * <p>
     * This is the failure result of an action invocation on the peer's connection
     * management service. The execution of the
     * {@link #createConnectionWithPeer(org.jupnp.model.ServiceReference, org.jupnp.controlpoint.ControlPoint, org.jupnp.model.meta.Service, org.jupnp.support.model.ProtocolInfo , org.jupnp.support.model.ConnectionInfo.Direction)}
     * and
     * {@link #closeConnectionWithPeer(org.jupnp.controlpoint.ControlPoint, org.jupnp.model.meta.Service, org.jupnp.support.model.ConnectionInfo)}
     * methods will block until this method completes handling any failure.
     * </p>
     *
     * @param invocation The underlying action invocation of the remote connection manager service.
     * @param operation The network message response if there was a response, or <code>null</code>.
     * @param defaultFailureMessage A user-friendly error message generated from the invocation exception and response.
     */
    protected abstract void peerFailure(ActionInvocation<?> invocation, UpnpResponse operation,
            String defaultFailureMessage);
}
