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
package org.jupnp.transport.spi;

import java.net.DatagramPacket;
import java.net.InetAddress;

import org.jupnp.model.UnsupportedDataException;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.OutgoingDatagramMessage;

/**
 * Reads and creates UDP datagrams from and into UPnP messages.
 * <p>
 * An implementation of this interface has to be thread-safe.
 * </p>
 *
 * @author Christian Bauer
 */
public interface DatagramProcessor {

    /**
     * Reads the datagram and instantiates a message.
     * <p>
     * The message is either a {@link org.jupnp.model.message.UpnpRequest} or
     * a {@link org.jupnp.model.message.UpnpResponse} operation type.
     * </p>
     *
     * @param receivedOnAddress The address of the socket on which this datagram was received.
     * @param datagram The received UDP datagram.
     * @return The populated instance.
     * @throws org.jupnp.model.UnsupportedDataException If the datagram could not be read, or didn't contain required
     *             data.
     */
    IncomingDatagramMessage read(InetAddress receivedOnAddress, DatagramPacket datagram)
            throws UnsupportedDataException;

    /**
     * Creates a UDP datagram with the content of a message.
     * <p>
     * The outgoing message might be a {@link org.jupnp.model.message.UpnpRequest} or a
     * {@link org.jupnp.model.message.UpnpResponse}.
     * </p>
     *
     * @param message The outgoing datagram message.
     * @return An actual UDP datagram.
     * @throws UnsupportedDataException If the datagram could not be created.
     */
    DatagramPacket write(OutgoingDatagramMessage message) throws UnsupportedDataException;
}
