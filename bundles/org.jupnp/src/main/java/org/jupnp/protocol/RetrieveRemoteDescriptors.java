/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.binding.xml.DescriptorBindingException;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.ServiceDescriptorBinder;
import org.jupnp.model.ValidationError;
import org.jupnp.model.ValidationException;
import org.jupnp.model.message.StreamRequestMessage;
import org.jupnp.model.message.StreamResponseMessage;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.UpnpRequest;
import org.jupnp.model.meta.Icon;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.RegistrationException;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Retrieves all remote device XML descriptors, parses them, creates an immutable device and service metadata graph.
 * <p>
 * This implementation encapsulates all steps which are necessary to create a fully usable and populated
 * device metadata graph of a particular UPnP device. It starts with an unhydrated and typically just
 * discovered {@link org.jupnp.model.meta.RemoteDevice}, the only property that has to be available is
 * its {@link org.jupnp.model.meta.RemoteDeviceIdentity}.
 * </p>
 * <p>
 * This protocol implementation will then retrieve the device's XML descriptor, parse it, and retrieve and
 * parse all service descriptors until all device and service metadata has been retrieved. The fully
 * hydrated device is then added to the {@link org.jupnp.registry.Registry}.
 * </p>
 * <p>
 * Any descriptor retrieval, parsing, or validation error of the metadata will abort this protocol
 * with a warning message in the log.
 * </p>
 *
 * @author Christian Bauer
 * @author Kai Kreuzer - fixed service and embedded device processing
 */
public class RetrieveRemoteDescriptors implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(RetrieveRemoteDescriptors.class);

    private final UpnpService upnpService;
    private RemoteDevice rd;

    private static final ConcurrentHashMap<URL, Boolean> activeRetrievals = new ConcurrentHashMap<>();
    protected List<UDN> errorsAlreadyLogged = new ArrayList<>();

    public RetrieveRemoteDescriptors(UpnpService upnpService, RemoteDevice rd) {
        this.upnpService = upnpService;
        this.rd = rd;
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    @Override
    public void run() {

        URL deviceURL = rd.getIdentity().getDescriptorURL();

        // Exit if it has been discovered already, most likely because we have been waiting in the executor queue too
        // long
        if (getUpnpService().getRegistry().getRemoteDevice(rd.getIdentity().getUdn(), true) != null) {
            logger.trace("Exiting early, already discovered: {}", deviceURL);
            return;
        }

        // Performance optimization, try to avoid concurrent GET requests for device descriptor,
        // if we retrieve it once, we have the hydrated device. There is no different outcome
        // processing this several times concurrently.

        if (activeRetrievals.putIfAbsent(deviceURL, Boolean.TRUE) != null) {
            logger.trace("Exiting early, active retrieval for URL already in progress: {}", deviceURL);
            return;
        }

        try {
            describe();
        } catch (RouterException e) {
            logger.warn("Descriptor retrieval failed: {}", deviceURL, e);
        } finally {
            activeRetrievals.remove(deviceURL);
        }
    }

    protected void describe() throws RouterException {

        // All of the following is a very expensive and time consuming procedure, thanks to the
        // braindead design of UPnP. Several GET requests, several descriptors, several XML parsing
        // steps - all of this could be done with one and it wouldn't make a difference. So every
        // call of this method has to be really necessary and rare.

        if (getUpnpService().getRouter() == null) {
            logger.warn("Router not yet initialized");
            return;
        }

        StreamRequestMessage deviceDescRetrievalMsg;
        StreamResponseMessage deviceDescMsg;

        try {

            deviceDescRetrievalMsg = new StreamRequestMessage(UpnpRequest.Method.GET,
                    rd.getIdentity().getDescriptorURL());

            // Extra headers
            UpnpServiceConfiguration config = getUpnpService().getConfiguration();
            if (config != null) {
                UpnpHeaders headers = config.getDescriptorRetrievalHeaders(rd.getIdentity());
                if (headers != null) {
                    deviceDescRetrievalMsg.getHeaders().putAll(headers);
                }
            } else {
                logger.debug("Configuration not available, skipping descriptor retrieval headers");
            }

            logger.debug("Sending device descriptor retrieval message: {}", deviceDescRetrievalMsg);
            deviceDescMsg = getUpnpService().getRouter().send(deviceDescRetrievalMsg);

        } catch (IllegalArgumentException e) {
            // UpnpRequest constructor can throw IllegalArgumentException on invalid URI
            // IllegalArgumentException can also be thrown by Apache HttpClient on blank URI in send()
            logger.warn("Device descriptor retrieval failed: {}, possibly invalid URL",
                    rd.getIdentity().getDescriptorURL(), e);
            return;
        }

        if (deviceDescMsg == null) {
            logger.warn("Device descriptor retrieval failed, no response: {}", rd.getIdentity().getDescriptorURL());
            return;
        }

        if (deviceDescMsg.getOperation().isFailed()) {
            logger.warn("Device descriptor retrieval failed: {}, {}", rd.getIdentity().getDescriptorURL(),
                    deviceDescMsg.getOperation().getResponseDetails());
            return;
        }

        if (!deviceDescMsg.isContentTypeTextUDA()) {
            logger.debug("Received device descriptor without or with invalid Content-Type: {}",
                    rd.getIdentity().getDescriptorURL());
            // We continue despite the invalid UPnP message because we can still hope to convert the content
        }

        String descriptorContent = deviceDescMsg.getBodyString();
        if (descriptorContent == null || descriptorContent.isEmpty()) {
            logger.warn("Received empty device descriptor: {}", rd.getIdentity().getDescriptorURL());
            return;
        }

        logger.debug("Received root device descriptor: {}", deviceDescMsg);
        describe(descriptorContent);
    }

    protected void describe(String descriptorXML) throws RouterException {

        boolean notifiedStart = false;
        RemoteDevice describedDevice = null;
        try {

            UpnpServiceConfiguration config = getUpnpService().getConfiguration();
            if (config == null) {
                logger.warn("Configuration not available, cannot parse device descriptor: {}",
                        rd.getIdentity().getDescriptorURL());
                return;
            }

            DeviceDescriptorBinder deviceDescriptorBinder = config.getDeviceDescriptorBinderUDA10();

            describedDevice = deviceDescriptorBinder.describe(rd, descriptorXML);

            logger.debug("Remote device described (without services) notifying listeners: {}", describedDevice);
            notifiedStart = getUpnpService().getRegistry().notifyDiscoveryStart(describedDevice);

            logger.debug("Hydrating described device's services: {}", describedDevice);
            RemoteDevice hydratedDevice = describeServices(describedDevice);
            if (hydratedDevice == null) {
                if (!errorsAlreadyLogged.contains(rd.getIdentity().getUdn())) {
                    errorsAlreadyLogged.add(rd.getIdentity().getUdn());
                    logger.warn("Device service description failed: {}", rd);
                }
                if (notifiedStart) {
                    getUpnpService().getRegistry().notifyDiscoveryFailure(describedDevice,
                            new DescriptorBindingException("Device service description failed: " + rd));

                    // Even though not all services could be described, we want to have the device in the registry.
                    logger.debug("Adding described remote device to registry: {}", describedDevice);
                    getUpnpService().getRegistry().addDevice(describedDevice);
                    return;
                }
            } else {
                logger.debug("Adding fully hydrated remote device to registry: {}", hydratedDevice);
                // The registry will do the right thing: A new root device is going to be added, if it's
                // already present or we just received the descriptor again (because we got an embedded
                // devices' notification), it will simply update the expiration timestamp of the root
                // device.
                getUpnpService().getRegistry().addDevice(hydratedDevice);
            }
        } catch (ValidationException e) {
            // Avoid error log spam each time device is discovered, errors are logged once per device.
            if (!errorsAlreadyLogged.contains(rd.getIdentity().getUdn())) {
                errorsAlreadyLogged.add(rd.getIdentity().getUdn());
                logger.warn("Could not validate device model: {}", rd);
                for (ValidationError validationError : e.getErrors()) {
                    logger.warn(validationError.toString());
                }
                if (describedDevice != null && notifiedStart) {
                    getUpnpService().getRegistry().notifyDiscoveryFailure(describedDevice, e);
                }
            }

        } catch (DescriptorBindingException e) {
            logger.warn("Could not hydrate device or its services from descriptor: {}", rd, e);
            if (describedDevice != null && notifiedStart) {
                getUpnpService().getRegistry().notifyDiscoveryFailure(describedDevice, e);
            }

        } catch (RegistrationException e) {
            logger.warn("Adding hydrated device to registry failed: {}", rd, e);
            if (describedDevice != null && notifiedStart) {
                getUpnpService().getRegistry().notifyDiscoveryFailure(describedDevice, e);
            }
        }
    }

    protected RemoteDevice describeServices(RemoteDevice currentDevice)
            throws RouterException, DescriptorBindingException, ValidationException {

        List<RemoteService> describedServices = new ArrayList<>();
        if (currentDevice.hasServices()) {
            List<RemoteService> filteredServices = filterExclusiveServices(currentDevice.getServices());
            for (RemoteService service : filteredServices) {
                RemoteService svc = describeService(service);
                if (svc != null) {
                    describedServices.add(svc);
                }
            }
        }

        List<RemoteDevice> describedEmbeddedDevices = new ArrayList<>();
        if (currentDevice.hasEmbeddedDevices()) {
            for (RemoteDevice embeddedDevice : currentDevice.getEmbeddedDevices()) {
                if (embeddedDevice == null) {
                    continue;
                }
                RemoteDevice describedEmbeddedDevice = describeServices(embeddedDevice);
                if (describedEmbeddedDevice != null) {
                    describedEmbeddedDevices.add(describedEmbeddedDevice);
                }
            }
        }

        if ((currentDevice.hasServices() && describedServices.isEmpty())
                || (currentDevice.hasEmbeddedDevices() && describedEmbeddedDevices.isEmpty())) {
            // we cannot return a fully hydrated device, so we return null instead
            return null;
        }

        Icon[] iconDupes = new Icon[currentDevice.getIcons().length];
        for (int i = 0; i < currentDevice.getIcons().length; i++) {
            Icon icon = currentDevice.getIcons()[i];
            iconDupes[i] = icon.deepCopy();
        }

        // Yes, we create a completely new immutable graph here
        return currentDevice.newInstance(currentDevice.getIdentity().getUdn(), currentDevice.getVersion(),
                currentDevice.getType(), currentDevice.getDetails(), iconDupes,
                currentDevice.toServiceArray(describedServices), describedEmbeddedDevices);
    }

    protected RemoteService describeService(RemoteService service)
            throws RouterException, DescriptorBindingException, ValidationException {

        URL descriptorURL;
        try {
            descriptorURL = service.getDevice().normalizeURI(service.getDescriptorURI());
        } catch (IllegalArgumentException e) {
            logger.warn("Could not normalize service descriptor URL: {}", service.getDescriptorURI());
            return null;
        }

        UpnpServiceConfiguration config = getUpnpService().getConfiguration();
        if (config == null) {
            logger.warn("Configuration not available, cannot retrieve service descriptor: {}", descriptorURL);
            return null;
        }

        StreamRequestMessage serviceDescRetrievalMsg = new StreamRequestMessage(UpnpRequest.Method.GET, descriptorURL);

        // Extra headers
        UpnpHeaders headers = config.getDescriptorRetrievalHeaders(service.getDevice().getIdentity());
        if (headers != null) {
            serviceDescRetrievalMsg.getHeaders().putAll(headers);
        }

        logger.debug("Sending service descriptor retrieval message: {}", serviceDescRetrievalMsg);
        StreamResponseMessage serviceDescMsg = getUpnpService().getRouter().send(serviceDescRetrievalMsg);

        if (serviceDescMsg == null) {
            logger.warn("Could not retrieve service descriptor, no response: {}", service);
            return null;
        }

        if (serviceDescMsg.getOperation().isFailed()) {
            logger.warn("Service descriptor retrieval failed: {}, {}", descriptorURL,
                    serviceDescMsg.getOperation().getResponseDetails());
            return null;
        }

        if (!serviceDescMsg.isContentTypeTextUDA()) {
            logger.debug("Received service descriptor without or with invalid Content-Type: {}", descriptorURL);
            // We continue despite the invalid UPnP message because we can still hope to convert the content
        }

        String descriptorContent = serviceDescMsg.getBodyString();
        if (descriptorContent == null || descriptorContent.isEmpty()) {
            logger.warn("Received empty service descriptor: {}", descriptorURL);
            return null;
        }

        logger.debug("Received service descriptor, hydrating service model: {}", serviceDescMsg);
        ServiceDescriptorBinder serviceDescriptorBinder = config.getServiceDescriptorBinderUDA10();

        return serviceDescriptorBinder.describe(service, descriptorContent);
    }

    protected List<RemoteService> filterExclusiveServices(RemoteService[] services) {
        UpnpServiceConfiguration config = getUpnpService().getConfiguration();
        if (config == null) {
            logger.debug("Configuration not available, including all services");
            return Arrays.asList(services);
        }

        ServiceType[] exclusiveTypes = config.getExclusiveServiceTypes();

        if (exclusiveTypes == null || exclusiveTypes.length == 0) {
            return Arrays.asList(services);
        }

        List<RemoteService> exclusiveServices = new ArrayList<>();
        for (RemoteService discoveredService : services) {
            for (ServiceType exclusiveType : exclusiveTypes) {
                if (discoveredService.getServiceType().implementsVersion(exclusiveType)) {
                    logger.debug("Including exclusive service: {}", discoveredService);
                    exclusiveServices.add(discoveredService);
                } else {
                    logger.debug("Excluding unwanted service: {}", exclusiveType);
                }
            }
        }
        return exclusiveServices;
    }

    /**
     * Check if the descriptor that is going to be retrieved by this task is already being retrieved by another task.
     * Can be used for optimization to prevent submitting tasks that are not going to retrieve a new descriptor.
     * 
     * @param rd The remote device to check
     * @return <code>true</code> the descriptor is currently being retrieved, otherwise <code>false</code>
     * 
     * @throws IllegalArgumentException if the remote device is {@code null}
     */
    public static boolean isRetrievalInProgress(RemoteDevice rd) {
        if (rd == null) {
            throw new IllegalArgumentException("RemoteDevice must not be null!");
        }
        return activeRetrievals.containsKey(rd.getIdentity().getDescriptorURL());
    }
}
