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
package org.jupnp.protocol.async;

import org.jupnp.UpnpService;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sending <em>ALIVE</em> notification messages for a registered local device.
 *
 * @author Christian Bauer
 */
public class SendingNotificationAlive extends SendingNotification {

    private final Logger logger = LoggerFactory.getLogger(SendingNotification.class);

    public SendingNotificationAlive(UpnpService upnpService, LocalDevice device) {
        super(upnpService, device);
    }

    @Override
    protected void execute() throws RouterException {
        logger.trace("Sending alive messages ({} times) for: {}", getBulkRepeat(), getDevice());
        super.execute();
    }

    @Override
    protected NotificationSubtype getNotificationSubtype() {
        return NotificationSubtype.ALIVE;
    }
}
