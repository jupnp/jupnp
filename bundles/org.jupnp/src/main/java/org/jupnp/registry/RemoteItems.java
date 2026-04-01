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
package org.jupnp.registry;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jupnp.model.ExpirationDetails;
import org.jupnp.model.gena.CancelReason;
import org.jupnp.model.gena.RemoteGENASubscription;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.types.UDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal class, required by {@link RegistryImpl}.
 *
 * @author Christian Bauer
 */
class RemoteItems extends RegistryItems<RemoteDevice, RemoteGENASubscription> {

    private final Logger logger = LoggerFactory.getLogger(Registry.class);

    RemoteItems(RegistryImpl registry) {
        super(registry);
    }

    /**
     * Adds the given remote device to the registry, or udpates its expiration timestamp.
     * <p>
     * This method first checks if there is a remote device with the same UDN already registered. If so, it
     * updates the expiration timestamp of the remote device without notifying any registry listeners. If the
     * device is truly new, all its resources are tested for conflicts with existing resources in the registry's
     * namespace, then it is added to the registry and listeners are notified that a new fully described remote
     * device is now available.
     * </p>
     *
     * @param device The remote device to be added
     */
    @Override
    void add(final RemoteDevice device) {

        if (update(device.getIdentity())) {
            logger.trace("Ignoring addition, device already registered: {}", device);
            return;
        }

        Resource[] resources = getResources(device);

        for (Resource deviceResource : resources) {
            logger.trace("Validating remote device resource; {}", deviceResource);
            if (registry.getResource(deviceResource.getPathQuery()) != null) {
                throw new RegistrationException(
                        "URI namespace conflict with already registered resource: " + deviceResource);
            }
        }

        for (Resource validatedResource : resources) {
            registry.addResource(validatedResource);
            logger.trace("Added remote device resource: {}", validatedResource);
        }

        // Override the device's maximum age if configured (systems without multicast support)
        Integer maxAgeSeconds = null;
        if (registry.getConfiguration() != null) {
            maxAgeSeconds = registry.getConfiguration().getRemoteDeviceMaxAgeSeconds();
        }
        if (maxAgeSeconds == null) {
            maxAgeSeconds = device.getIdentity().getMaxAgeSeconds();
        }

        RegistryItem item = new RegistryItem(device.getIdentity().getUdn(), device, maxAgeSeconds);
        logger.trace("Adding hydrated remote device to registry with {} seconds expiration: {}",
                item.getExpirationDetails().getMaxAgeSeconds(), device);
        getDeviceItems().add(item);

        if (logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            sb.append("-------------------------- START Registry Namespace -----------------------------------\n");
            for (Resource resource : registry.getResources()) {
                sb.append(resource).append("\n");
            }
            sb.append("-------------------------- END Registry Namespace -----------------------------------");
            logger.trace(sb.toString());
        }

        // Only notify the listeners when the device is fully usable
        logger.trace("Completely hydrated remote device graph available, calling listeners: {}", device);
        for (final RegistryListener listener : registry.getListeners()) {
            registry.getConfiguration().getRemoteListenerExecutor()
                    .execute(() -> listener.remoteDeviceAdded(registry, device));
        }
    }

    boolean update(RemoteDeviceIdentity rdIdentity) {

        for (LocalDevice localDevice : registry.getLocalDevices()) {
            if (localDevice.findDevice(rdIdentity.getUdn()) != null) {
                logger.trace("Ignoring update, a local device graph contains UDN");
                return true;
            }
        }

        RemoteDevice registeredRemoteDevice = get(rdIdentity.getUdn(), false);

        if (registeredRemoteDevice != null) {

            // check for IP address change
            RemoteDeviceIdentity remoteDeviceIdentity = registeredRemoteDevice.getIdentity();
            if (remoteDeviceIdentity != null) {
                URL descriptorUrl = rdIdentity.getDescriptorURL();
                URL remoteDescriptorUrl = remoteDeviceIdentity.getDescriptorURL();
                if (descriptorUrl != null & remoteDescriptorUrl != null
                        && !descriptorUrl.getHost().equals(remoteDescriptorUrl.getHost())) {
                    logger.trace("IP adress has changed - removing the registered device");
                    remove(registeredRemoteDevice);
                    return false;
                }
            }

            if (!registeredRemoteDevice.isRoot()) {
                logger.trace("Updating root device of embedded: {}", registeredRemoteDevice);
                registeredRemoteDevice = registeredRemoteDevice.getRoot();
            }

            // Override the device's maximum age if configured (systems without multicast support)
            final RegistryItem<UDN, RemoteDevice> item = new RegistryItem<>(
                    registeredRemoteDevice.getIdentity().getUdn(), registeredRemoteDevice,
                    registry.getConfiguration().getRemoteDeviceMaxAgeSeconds() != null
                            ? registry.getConfiguration().getRemoteDeviceMaxAgeSeconds()
                            : rdIdentity.getMaxAgeSeconds());

            logger.trace("Updating expiration of: {}", registeredRemoteDevice);
            getDeviceItems().remove(item);
            getDeviceItems().add(item);

            logger.trace("Remote device updated, calling listeners: {}", registeredRemoteDevice);
            for (final RegistryListener listener : registry.getListeners()) {
                registry.getConfiguration().getRemoteListenerExecutor()
                        .execute(() -> listener.remoteDeviceUpdated(registry, item.getItem()));
            }

            return true;

        }
        return false;
    }

    /**
     * Removes the given device from the registry and notifies registry listeners.
     *
     * @param remoteDevice The device to remove from the registry.
     * @return <tt>true</tt> if the given device was found and removed from the registry, false if it wasn't registered.
     */
    @Override
    boolean remove(final RemoteDevice remoteDevice) {
        return remove(remoteDevice, false);
    }

    boolean remove(final RemoteDevice remoteDevice, boolean shuttingDown) throws RegistrationException {
        final RemoteDevice registeredDevice = get(remoteDevice.getIdentity().getUdn(), true);
        if (registeredDevice != null) {

            logger.trace("Removing remote device from registry: {}", remoteDevice);

            // Resources
            for (Resource deviceResource : getResources(registeredDevice)) {
                if (registry.removeResource(deviceResource)) {
                    logger.trace("Unregistered resource: {}", deviceResource);
                }
            }

            // Active subscriptions
            Iterator<RegistryItem<String, RemoteGENASubscription>> it = getSubscriptionItems().iterator();
            while (it.hasNext()) {
                final RegistryItem<String, RemoteGENASubscription> outgoingSubscription = it.next();

                UDN subscriptionForUDN = outgoingSubscription.getItem().getService().getDevice().getIdentity().getUdn();

                if (subscriptionForUDN.equals(registeredDevice.getIdentity().getUdn())) {
                    logger.trace("Removing outgoing subscription: {}", outgoingSubscription.getKey());
                    it.remove();
                    if (!shuttingDown) {
                        registry.getConfiguration().getRemoteListenerExecutor().execute(
                                () -> outgoingSubscription.getItem().end(CancelReason.DEVICE_WAS_REMOVED, null));
                    }
                }
            }

            // Only notify listeners if we are NOT in the process of shutting down the registry
            if (!shuttingDown) {
                for (final RegistryListener listener : registry.getListeners()) {
                    registry.getConfiguration().getRemoteListenerExecutor()
                            .execute(() -> listener.remoteDeviceRemoved(registry, registeredDevice));
                }
            }

            // Finally, remove the device from the registry
            getDeviceItems().remove(new RegistryItem<>(registeredDevice.getIdentity().getUdn()));

            return true;
        }

        return false;
    }

    @Override
    void removeAll() {
        removeAll(false);
    }

    void removeAll(boolean shuttingDown) {
        RemoteDevice[] allDevices = get().toArray(new RemoteDevice[get().size()]);
        for (RemoteDevice device : allDevices) {
            remove(device, shuttingDown);
        }
    }

    /* ############################################################################################################ */

    void start() {
        // Noop
    }

    @Override
    void maintain() {

        if (getDeviceItems().isEmpty()) {
            return;
        }

        // Remove expired remote devices
        Map<UDN, RemoteDevice> expiredRemoteDevices = new HashMap<>();
        for (RegistryItem<UDN, RemoteDevice> remoteItem : getDeviceItems()) {
            logger.trace("Device '{}' expires in seconds: {}", remoteItem.getItem(),
                    remoteItem.getExpirationDetails().getSecondsUntilExpiration());
            if (remoteItem.getExpirationDetails().hasExpired(false)) {
                expiredRemoteDevices.put(remoteItem.getKey(), remoteItem.getItem());
            }
        }
        for (RemoteDevice remoteDevice : expiredRemoteDevices.values()) {
            logger.trace("Removing expired: {}", remoteDevice);
            remove(remoteDevice);
        }

        // Renew outgoing subscriptions
        Set<RemoteGENASubscription> expiredOutgoingSubscriptions = new HashSet<>();
        for (RegistryItem<String, RemoteGENASubscription> item : getSubscriptionItems()) {
            ExpirationDetails expirationDetails = item.getExpirationDetails();
            if (expirationDetails.getRenewAttempts() < 1 && expirationDetails.hasExpired(true)) {
                expiredOutgoingSubscriptions.add(item.getItem());
                expirationDetails.renewAttempted();
            }
        }
        for (RemoteGENASubscription subscription : expiredOutgoingSubscriptions) {
            logger.trace("Renewing outgoing subscription: {}", subscription);
            renewOutgoingSubscription(subscription);
        }
    }

    public void resume() {
        logger.trace("Updating remote device expiration timestamps on resume");
        List<RemoteDeviceIdentity> toUpdate = new ArrayList<>();
        for (RegistryItem<UDN, RemoteDevice> remoteItem : getDeviceItems()) {
            toUpdate.add(remoteItem.getItem().getIdentity());
        }
        for (RemoteDeviceIdentity identity : toUpdate) {
            update(identity);
        }
    }

    @Override
    void shutdown() {
        logger.trace("Cancelling all outgoing subscriptions to remote devices during shutdown");
        List<RemoteGENASubscription> remoteSubscriptions = new ArrayList<>();
        for (RegistryItem<String, RemoteGENASubscription> item : getSubscriptionItems()) {
            remoteSubscriptions.add(item.getItem());
        }
        for (RemoteGENASubscription remoteSubscription : remoteSubscriptions) {
            // This will remove the active subscription from the registry!
            registry.getProtocolFactory().createSendingUnsubscribe(remoteSubscription).run();
        }

        logger.trace("Removing all remote devices from registry during shutdown");
        removeAll(true);
    }

    /* ############################################################################################################ */

    protected void renewOutgoingSubscription(final RemoteGENASubscription subscription) {
        registry.executeAsyncProtocol(registry.getProtocolFactory().createSendingRenewal(subscription));
    }
}
