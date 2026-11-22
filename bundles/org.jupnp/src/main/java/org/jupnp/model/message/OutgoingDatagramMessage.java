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
package org.jupnp.model.message;

import java.net.InetAddress;

/**
 * A UDP datagram request or response message for sending, with destination address and port.
 * <p>
 * Optionally holds the local address the message should be sent from, e.g. a search response
 * is sent from the address on which the search request was received.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class OutgoingDatagramMessage<O extends UpnpOperation> extends UpnpMessage<O> {

    private InetAddress destinationAddress;
    private int destinationPort;
    private InetAddress localAddress;
    // For performance reasons, headers of this message are not normalized
    private UpnpHeaders headers = new UpnpHeaders(false);

    protected OutgoingDatagramMessage(O operation, InetAddress destinationAddress, int destinationPort) {
        super(operation);
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
    }

    protected OutgoingDatagramMessage(O operation, InetAddress destinationAddress, int destinationPort,
            InetAddress localAddress) {
        this(operation, destinationAddress, destinationPort);
        this.localAddress = localAddress;
    }

    protected OutgoingDatagramMessage(O operation, BodyType bodyType, Object body, InetAddress destinationAddress,
            int destinationPort) {
        super(operation, bodyType, body);
        this.destinationAddress = destinationAddress;
        this.destinationPort = destinationPort;
    }

    public InetAddress getDestinationAddress() {
        return destinationAddress;
    }

    public int getDestinationPort() {
        return destinationPort;
    }

    /**
     * @return The local address this message should be sent from, or <code>null</code> if there is no preference.
     */
    public InetAddress getLocalAddress() {
        return localAddress;
    }

    @Override
    public UpnpHeaders getHeaders() {
        return this.headers;
    }
}
