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
package org.jupnp.mock;

import java.net.URL;

import org.jupnp.UpnpService;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.protocol.ProtocolCreationException;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.protocol.ReceivingAsync;
import org.jupnp.protocol.ReceivingSync;
import org.jupnp.protocol.async.SendingNotificationAlive;
import org.jupnp.protocol.async.SendingNotificationByebye;
import org.jupnp.protocol.async.SendingSearch;
import org.jupnp.protocol.sync.SendingAction;
import org.jupnp.protocol.sync.SendingEvent;
import org.jupnp.protocol.sync.SendingRenewal;
import org.jupnp.protocol.sync.SendingSubscribe;
import org.jupnp.protocol.sync.SendingUnsubscribe;

/**
 * @author Christian Bauer
 */
public class MockProtocolFactory implements ProtocolFactory {

    @Override
    public UpnpService getUpnpService() {
        return null;
    }

    @Override
    public ReceivingAsync createReceivingAsync(IncomingDatagramMessage message) throws ProtocolCreationException {
        return null;
    }

    @Override
    public ReceivingSync createReceivingSync(StreamRequestMessage requestMessage) throws ProtocolCreationException {
        return null;
    }

    @Override
    public SendingNotificationAlive createSendingNotificationAlive(LocalDevice localDevice) {
        return null;
    }

    @Override
    public SendingNotificationByebye createSendingNotificationByebye(LocalDevice localDevice) {
        return null;
    }

    @Override
    public SendingSearch createSendingSearch(UpnpHeader searchTarget, int mxSeconds) {
        return null;
    }

    @Override
    public SendingAction createSendingAction(ActionInvocation actionInvocation, URL controlURL) {
        return null;
    }

    @Override
    public SendingSubscribe createSendingSubscribe(RemoteGENASubscription subscription) {
        return null;
    }

    @Override
    public SendingRenewal createSendingRenewal(RemoteGENASubscription subscription) {
        return null;
    }

    @Override
    public SendingUnsubscribe createSendingUnsubscribe(RemoteGENASubscription subscription) {
        return null;
    }

    @Override
    public SendingEvent createSendingEvent(LocalGENASubscription subscription) {
        return null;
    }
}
