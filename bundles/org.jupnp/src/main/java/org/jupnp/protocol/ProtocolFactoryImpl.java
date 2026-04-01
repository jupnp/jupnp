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
package org.jupnp.protocol;

import java.net.URI;
import java.net.URL;
import java.util.List;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.Namespace;
import org.jupnp.model.NetworkAddress;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.gena.LocalGENASubscription;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.message.IncomingDatagramMessage;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.model.types.NamedServiceType;
import org.jupnp.model.types.NotificationSubtype;
import org.jupnp.model.types.ServiceType;
import org.jupnp.protocol.async.ReceivingNotification;
import org.jupnp.protocol.async.ReceivingSearch;
import org.jupnp.protocol.async.ReceivingSearchResponse;
import org.jupnp.protocol.async.SendingNotificationAlive;
import org.jupnp.protocol.async.SendingNotificationByebye;
import org.jupnp.protocol.async.SendingSearch;
import org.jupnp.protocol.sync.ReceivingAction;
import org.jupnp.protocol.sync.ReceivingEvent;
import org.jupnp.protocol.sync.ReceivingRetrieval;
import org.jupnp.protocol.sync.ReceivingSubscribe;
import org.jupnp.protocol.sync.ReceivingUnsubscribe;
import org.jupnp.protocol.sync.SendingAction;
import org.jupnp.protocol.sync.SendingEvent;
import org.jupnp.protocol.sync.SendingRenewal;
import org.jupnp.protocol.sync.SendingSubscribe;
import org.jupnp.protocol.sync.SendingUnsubscribe;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation, directly instantiates the appropriate protocols.
 * 
 * @author Christian Bauer
 */
public class ProtocolFactoryImpl implements ProtocolFactory {

    private final Logger logger = LoggerFactory.getLogger(ProtocolFactory.class);

    protected final UpnpService upnpService;

    protected ProtocolFactoryImpl() {
        upnpService = null;
    }

    public ProtocolFactoryImpl(UpnpService upnpService) {
        logger.trace("Creating ProtocolFactory: {}", getClass().getName());
        this.upnpService = upnpService;
    }

    @Override
    public UpnpService getUpnpService() {
        return upnpService;
    }

    @Override
    public ReceivingAsync createReceivingAsync(IncomingDatagramMessage message) throws ProtocolCreationException {
        logger.trace("Creating protocol for incoming asynchronous: {}", message);

        if (message.getOperation() instanceof UpnpRequest) {
            switch (((IncomingDatagramMessage<UpnpRequest>) message).getOperation().getMethod()) {
                case NOTIFY:
                    return isByeBye(message) || isSupportedServiceAdvertisement(message)
                            ? createReceivingNotification(message)
                            : null;
                case MSEARCH:
                    return createReceivingSearch(message);
            }

        } else if (message.getOperation() instanceof UpnpResponse) {
            return isSupportedServiceAdvertisement(message) ? createReceivingSearchResponse(message) : null;
        }

        throw new ProtocolCreationException("Protocol for incoming datagram message not found: " + message);
    }

    protected ReceivingAsync createReceivingNotification(IncomingDatagramMessage<UpnpRequest> incomingRequest) {
        return new ReceivingNotification(getUpnpService(), incomingRequest);
    }

    protected ReceivingAsync createReceivingSearch(IncomingDatagramMessage<UpnpRequest> incomingRequest) {
        return new ReceivingSearch(getUpnpService(), incomingRequest);
    }

    protected ReceivingAsync createReceivingSearchResponse(IncomingDatagramMessage<UpnpResponse> incomingResponse) {
        return new ReceivingSearchResponse(getUpnpService(), incomingResponse);
    }

    // DO NOT USE THE PARSED/TYPED MSG HEADERS! THIS WOULD DEFEAT THE PURPOSE OF THIS OPTIMIZATION!

    protected boolean isByeBye(IncomingDatagramMessage message) {
        String ntsHeader = message.getHeaders().getFirstHeader(UpnpHeader.Type.NTS.getHttpName());
        return ntsHeader != null && ntsHeader.equals(NotificationSubtype.BYEBYE.getHeaderString());
    }

    protected boolean isSupportedServiceAdvertisement(IncomingDatagramMessage message) {
        UpnpServiceConfiguration config = upnpService.getConfiguration();
        if (config == null) {
            return false;
        }
        ServiceType[] exclusiveServiceTypes = config.getExclusiveServiceTypes();
        if (exclusiveServiceTypes == null) {
            return false; // Discovery is disabled
        }
        if (exclusiveServiceTypes.length == 0) {
            return true; // Any advertisement is fine
        }

        String usnHeader = message.getHeaders().getFirstHeader(UpnpHeader.Type.USN.getHttpName());
        if (usnHeader == null) {
            return false; // Not a service advertisement, drop it
        }

        try {
            NamedServiceType nst = NamedServiceType.valueOf(usnHeader);
            for (ServiceType exclusiveServiceType : exclusiveServiceTypes) {
                if (nst.getServiceType().implementsVersion(exclusiveServiceType)) {
                    return true;
                }
            }
        } catch (InvalidValueException e) {
            logger.trace("Not a named service type header value: {}", usnHeader);
        }
        logger.trace("Service advertisement not supported, dropping it: {}", usnHeader);
        return false;
    }

    @Override
    public ReceivingSync createReceivingSync(StreamRequestMessage message) throws ProtocolCreationException {
        logger.trace("Creating protocol for incoming synchronous: {}", message);

        if (message.getOperation().getMethod().equals(UpnpRequest.Method.GET)) {

            return createReceivingRetrieval(message);

        } else if (getUpnpService().getConfiguration().getNamespace().isControlPath(message.getUri())) {

            if (message.getOperation().getMethod().equals(UpnpRequest.Method.POST))
                return createReceivingAction(message);

        } else if (getUpnpService().getConfiguration().getNamespace().isEventSubscriptionPath(message.getUri())) {

            if (message.getOperation().getMethod().equals(UpnpRequest.Method.SUBSCRIBE)) {
                return createReceivingSubscribe(message);
            } else if (message.getOperation().getMethod().equals(UpnpRequest.Method.UNSUBSCRIBE)) {
                return createReceivingUnsubscribe(message);
            }

        } else if (getUpnpService().getConfiguration().getNamespace().isEventCallbackPath(message.getUri())) {

            if (message.getOperation().getMethod().equals(UpnpRequest.Method.NOTIFY))
                return createReceivingEvent(message);

        } else {

            // TODO: UPNP VIOLATION: Onkyo devices send event messages with trailing garbage characters
            // /dev/9bb022aa-e922-aab9-682b-aa09e9b9e059/svc/upnp-org/RenderingControl/event/cb192%2e168%2e10%2e38
            // TODO: UPNP VIOLATION: Yamaha does the same
            // /dev/9ab0c000-f668-11de-9976-00a0de870fd4/svc/upnp-org/RenderingControl/event/cb><http://10.189.150.197:42082/dev/9ab0c000-f668-11de-9976-00a0de870fd4/svc/upnp-org/RenderingControl/event/cb
            if (message.getUri().getPath().contains(Namespace.EVENTS + Namespace.CALLBACK_FILE)) {
                logger.warn("Fixing trailing garbage in event message path: {}", message.getUri().getPath());
                String invalid = message.getUri().toString();
                message.setUri(URI.create(invalid.substring(0,
                        invalid.indexOf(Namespace.CALLBACK_FILE) + Namespace.CALLBACK_FILE.length())));
                if (getUpnpService().getConfiguration().getNamespace().isEventCallbackPath(message.getUri())
                        && message.getOperation().getMethod().equals(UpnpRequest.Method.NOTIFY))
                    return createReceivingEvent(message);
            }

        }

        throw new ProtocolCreationException("Protocol for message type not found: " + message);
    }

    @Override
    public SendingNotificationAlive createSendingNotificationAlive(LocalDevice localDevice) {
        return new SendingNotificationAlive(getUpnpService(), localDevice);
    }

    @Override
    public SendingNotificationByebye createSendingNotificationByebye(LocalDevice localDevice) {
        return new SendingNotificationByebye(getUpnpService(), localDevice);
    }

    @Override
    public SendingSearch createSendingSearch(UpnpHeader searchTarget, int mxSeconds) {
        return new SendingSearch(getUpnpService(), searchTarget, mxSeconds);
    }

    @Override
    public SendingAction createSendingAction(ActionInvocation actionInvocation, URL controlURL) {
        return new SendingAction(getUpnpService(), actionInvocation, controlURL);
    }

    @Override
    public SendingSubscribe createSendingSubscribe(RemoteGENASubscription subscription)
            throws ProtocolCreationException {
        try {
            List<NetworkAddress> activeStreamServers = getUpnpService().getRouter().getActiveStreamServers(
                    subscription.getService().getDevice().getIdentity().getDiscoveredOnLocalAddress());
            return new SendingSubscribe(getUpnpService(), subscription, activeStreamServers);
        } catch (RouterException e) {
            throw new ProtocolCreationException(
                    "Failed to obtain local stream servers (for event callback URL creation) from router", e);
        }
    }

    @Override
    public SendingRenewal createSendingRenewal(RemoteGENASubscription subscription) {
        return new SendingRenewal(getUpnpService(), subscription);
    }

    @Override
    public SendingUnsubscribe createSendingUnsubscribe(RemoteGENASubscription subscription) {
        return new SendingUnsubscribe(getUpnpService(), subscription);
    }

    @Override
    public SendingEvent createSendingEvent(LocalGENASubscription subscription) {
        return new SendingEvent(getUpnpService(), subscription);
    }

    protected ReceivingRetrieval createReceivingRetrieval(StreamRequestMessage message) {
        return new ReceivingRetrieval(getUpnpService(), message);
    }

    protected ReceivingAction createReceivingAction(StreamRequestMessage message) {
        return new ReceivingAction(getUpnpService(), message);
    }

    protected ReceivingSubscribe createReceivingSubscribe(StreamRequestMessage message) {
        return new ReceivingSubscribe(getUpnpService(), message);
    }

    protected ReceivingUnsubscribe createReceivingUnsubscribe(StreamRequestMessage message) {
        return new ReceivingUnsubscribe(getUpnpService(), message);
    }

    protected ReceivingEvent createReceivingEvent(StreamRequestMessage message) {
        return new ReceivingEvent(getUpnpService(), message);
    }
}
