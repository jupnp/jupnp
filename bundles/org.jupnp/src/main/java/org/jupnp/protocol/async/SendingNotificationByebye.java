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
 * Sending <em>BYEBYE</em> notification messages for a registered local device.
 *
 * @author Christian Bauer
 */
public class SendingNotificationByebye extends SendingNotification {

    private final Logger logger = LoggerFactory.getLogger(SendingNotification.class);

    public SendingNotificationByebye(UpnpService upnpService, LocalDevice device) {
        super(upnpService, device);
    }

    // The UDA 1.0 spec says "a message corresponding to /each/ of the ssd:alive messages" but
    // it's not clear if that means the "required" messages according to the tables only or if
    // it includes the triple (or whatever) repeated messages that have been sent to protect
    // against networking problems. It also says, a little later, that "each of the messages should
    // be send more than once". So we are also sending them three times - hell, why not pollute the
    // network with useless stuff, that is going to make this more reliable for sure...

    // In other words: The superclass method is fine even for byebye.

    @Override
    protected void execute() throws RouterException {
        logger.trace("Sending byebye messages ({} times) for: {}", getBulkRepeat(), getDevice());
        super.execute();
    }

    @Override
    protected NotificationSubtype getNotificationSubtype() {
        return NotificationSubtype.BYEBYE;
    }
}
