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
package org.jupnp.model.meta;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.jupnp.model.Namespace;
import org.jupnp.model.Validatable;
import org.jupnp.model.ValidationError;
import org.jupnp.model.ValidationException;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDN;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes either a root or embedded device.
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public abstract class Device<DI extends DeviceIdentity, D extends Device, S extends Service> implements Validatable {

    private final Logger logger = LoggerFactory.getLogger(Device.class);

    private final DI identity;

    private final UDAVersion version;
    private final DeviceType type;
    private final DeviceDetails details;
    private final Icon[] icons;
    protected final S[] services;
    protected final D[] embeddedDevices;

    // Package mutable state
    private D parentDevice;

    protected Device(DI identity) throws ValidationException {
        this(identity, null, null, null, null, null);
    }

    protected Device(DI identity, DeviceType type, DeviceDetails details, Icon[] icons, S[] services)
            throws ValidationException {
        this(identity, null, type, details, icons, services, null);
    }

    protected Device(DI identity, DeviceType type, DeviceDetails details, Icon[] icons, S[] services,
            D[] embeddedDevices) throws ValidationException {
        this(identity, null, type, details, icons, services, embeddedDevices);
    }

    protected Device(DI identity, UDAVersion version, DeviceType type, DeviceDetails details, Icon[] icons,
            S[] services, D[] embeddedDevices) throws ValidationException {

        this.identity = identity;
        this.version = version == null ? new UDAVersion() : version;
        this.type = type;
        this.details = details;

        // We don't fail device validation if icons were invalid, only log a warning. To
        // comply with mutability rules (can't set icons field in validate() method), we
        // validate the icons here before we set the field value
        List<Icon> validIcons = new ArrayList<>();
        if (icons != null) {
            for (Icon icon : icons) {
                if (icon != null) {
                    icon.setDevice(this); // Set before validate()!
                    List<ValidationError> iconErrors = icon.validate();
                    if (iconErrors.isEmpty()) {
                        validIcons.add(icon);
                    } else {
                        SpecificationViolationReporter.report("Discarding invalid '" + icon + "': " + iconErrors);
                    }
                }
            }
        }
        this.icons = validIcons.toArray(new Icon[validIcons.size()]);

        boolean allNullServices = true;
        if (services != null) {
            for (S service : services) {
                if (service != null) {
                    allNullServices = false;
                    service.setDevice(this);
                }
            }
        }
        this.services = services == null || allNullServices ? null : services;

        boolean allNullEmbedded = true;
        if (embeddedDevices != null) {
            for (D embeddedDevice : embeddedDevices) {
                if (embeddedDevice != null) {
                    allNullEmbedded = false;
                    embeddedDevice.setParentDevice(this);
                }
            }
        }
        this.embeddedDevices = embeddedDevices == null || allNullEmbedded ? null : embeddedDevices;

        List<ValidationError> errors = validate();
        if (!errors.isEmpty()) {
            if (logger.isTraceEnabled()) {
                for (ValidationError error : errors) {
                    logger.trace(error.toString());
                }
            }
            throw new ValidationException("Validation of device graph failed, call getErrors() on exception", errors);
        }
    }

    public DI getIdentity() {
        return identity;
    }

    public UDAVersion getVersion() {
        return version;
    }

    public DeviceType getType() {
        return type;
    }

    public DeviceDetails getDetails() {
        return details;
    }

    public DeviceDetails getDetails(RemoteClientInfo info) {
        return this.getDetails();
    }

    public Icon[] getIcons() {
        return icons;
    }

    public boolean hasIcons() {
        return getIcons() != null && getIcons().length > 0;
    }

    public boolean hasServices() {
        return getServices() != null && getServices().length > 0;
    }

    public boolean hasEmbeddedDevices() {
        return getEmbeddedDevices() != null && getEmbeddedDevices().length > 0;
    }

    public D getParentDevice() {
        return parentDevice;
    }

    void setParentDevice(D parentDevice) {
        if (this.parentDevice != null)
            throw new IllegalStateException("Final value has been set already, model is immutable");
        this.parentDevice = parentDevice;
    }

    public boolean isRoot() {
        return getParentDevice() == null;
    }

    public abstract S[] getServices();

    public abstract D[] getEmbeddedDevices();

    public abstract D getRoot();

    public abstract D findDevice(UDN udn);

    public D[] findEmbeddedDevices() {
        return toDeviceArray(findEmbeddedDevices((D) this));
    }

    public D[] findDevices(DeviceType deviceType) {
        return toDeviceArray(find(deviceType, (D) this));
    }

    public D[] findDevices(ServiceType serviceType) {
        return toDeviceArray(find(serviceType, (D) this));
    }

    public Icon[] findIcons() {
        List<Icon> icons = new ArrayList<>();
        if (hasIcons()) {
            icons.addAll(Arrays.asList(getIcons()));
        }
        D[] embeddedDevices = findEmbeddedDevices();
        for (D embeddedDevice : embeddedDevices) {
            if (embeddedDevice.hasIcons()) {
                icons.addAll(Arrays.asList(embeddedDevice.getIcons()));
            }
        }
        return icons.toArray(new Icon[icons.size()]);
    }

    public S[] findServices() {
        return toServiceArray(findServices(null, null, (D) this));
    }

    public S[] findServices(ServiceType serviceType) {
        return toServiceArray(findServices(serviceType, null, (D) this));
    }

    protected D find(UDN udn, D current) {
        if (current.getIdentity().getUdn().equals(udn)) {
            return current;
        }
        if (current.hasEmbeddedDevices()) {
            for (D embeddedDevice : (D[]) current.getEmbeddedDevices()) {
                D match;
                if ((match = find(udn, embeddedDevice)) != null) {
                    return match;
                }
            }
        }
        return null;
    }

    protected Collection<D> findEmbeddedDevices(D current) {
        Collection<D> devices = new HashSet<>();
        if (!current.isRoot()) {
            devices.add(current);
        }
        if (current.hasEmbeddedDevices()) {
            for (D embeddedDevice : (D[]) current.getEmbeddedDevices()) {
                devices.addAll(findEmbeddedDevices(embeddedDevice));
            }
        }
        return devices;
    }

    protected Collection<D> find(DeviceType deviceType, D current) {
        Collection<D> devices = new HashSet<>();
        // Type might be null if we just discovered the device and it hasn't yet been hydrated
        if (current.getType() != null && current.getType().implementsVersion(deviceType)) {
            devices.add(current);
        }
        if (current.hasEmbeddedDevices()) {
            for (D embeddedDevice : (D[]) current.getEmbeddedDevices()) {
                devices.addAll(find(deviceType, embeddedDevice));
            }
        }
        return devices;
    }

    protected Collection<D> find(ServiceType serviceType, D current) {
        Collection<S> services = findServices(serviceType, null, current);
        Collection<D> devices = new HashSet<>();
        for (Service service : services) {
            devices.add((D) service.getDevice());
        }
        return devices;
    }

    protected Collection<S> findServices(ServiceType serviceType, ServiceId serviceId, D current) {
        Collection services = new HashSet<>();
        if (current.hasServices()) {
            for (Service service : current.getServices()) {
                if (isMatch(service, serviceType, serviceId))
                    services.add(service);
            }
        }
        Collection<D> embeddedDevices = findEmbeddedDevices(current);
        if (embeddedDevices != null) {
            for (D embeddedDevice : embeddedDevices) {
                if (embeddedDevice.hasServices()) {
                    for (Service service : embeddedDevice.getServices()) {
                        if (isMatch(service, serviceType, serviceId))
                            services.add(service);
                    }
                }
            }
        }
        return services;
    }

    public S findService(ServiceId serviceId) {
        Collection<S> services = findServices(null, serviceId, (D) this);
        return services.size() == 1 ? services.iterator().next() : null;
    }

    public S findService(ServiceType serviceType) {
        Collection<S> services = findServices(serviceType, null, (D) this);
        return !services.isEmpty() ? services.iterator().next() : null;
    }

    public ServiceType[] findServiceTypes() {
        Collection<S> services = findServices(null, null, (D) this);
        Collection<ServiceType> col = new HashSet<>();
        for (S service : services) {
            col.add(service.getServiceType());
        }
        return col.toArray(new ServiceType[col.size()]);
    }

    private boolean isMatch(Service s, ServiceType serviceType, ServiceId serviceId) {
        boolean matchesType = serviceType == null || s.getServiceType().implementsVersion(serviceType);
        boolean matchesId = serviceId == null || s.getServiceId().equals(serviceId);
        return matchesType && matchesId;
    }

    public boolean isFullyHydrated() {
        S[] services = findServices();
        for (S service : services) {
            if (service.hasStateVariables()) {
                return true;
            }
        }
        return false;
    }

    public String getDisplayString() {

        // The goal is to have a clean string with "<manufacturer> <model name> <model#>"

        String cleanModelName = null;
        String cleanModelNumber = null;

        if (getDetails() != null && getDetails().getModelDetails() != null) {

            // Some vendors end the model name with the model number, let's remove that
            ModelDetails modelDetails = getDetails().getModelDetails();
            if (modelDetails.getModelName() != null) {
                cleanModelName = modelDetails.getModelNumber() != null
                        && modelDetails.getModelName().endsWith(modelDetails.getModelNumber())
                                ? modelDetails.getModelName().substring(0,
                                        modelDetails.getModelName().length() - modelDetails.getModelNumber().length())
                                : modelDetails.getModelName();
            }

            // Some vendors repeat the model name as the model number, no good
            if (cleanModelName != null) {
                cleanModelNumber = modelDetails.getModelNumber() != null
                        && !cleanModelName.startsWith(modelDetails.getModelNumber()) ? modelDetails.getModelNumber()
                                : "";
            } else {
                cleanModelNumber = modelDetails.getModelNumber();
            }
        }

        StringBuilder sb = new StringBuilder();

        if (getDetails() != null && getDetails().getManufacturerDetails() != null) {

            // Some vendors repeat the manufacturer in model name, let's remove that too
            if (cleanModelName != null && getDetails().getManufacturerDetails().getManufacturer() != null) {
                cleanModelName = cleanModelName.startsWith(getDetails().getManufacturerDetails().getManufacturer())
                        ? cleanModelName.substring(getDetails().getManufacturerDetails().getManufacturer().length())
                                .trim()
                        : cleanModelName.trim();
            }

            if (getDetails().getManufacturerDetails().getManufacturer() != null) {
                sb.append(getDetails().getManufacturerDetails().getManufacturer());
            }
        }

        sb.append(cleanModelName != null && !cleanModelName.isEmpty() ? " " + cleanModelName : "");
        sb.append(cleanModelNumber != null && !cleanModelNumber.isEmpty() ? " " + cleanModelNumber.trim() : "");
        return sb.toString();
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (getType() != null) {

            // Only validate the graph if we have a device type - that means we validate only if there
            // actually is a fully hydrated graph, not just a discovered device of which we haven't even
            // retrieved the descriptor yet. This assumes that the descriptor will ALWAYS contain a device
            // type. Now that is a risky assumption...

            errors.addAll(getVersion().validate());

            if (getIdentity() != null) {
                errors.addAll(getIdentity().validate());
            }

            if (getDetails() != null) {
                errors.addAll(getDetails().validate());
            }

            if (hasServices()) {
                for (Service service : getServices()) {
                    if (service != null)
                        errors.addAll(service.validate());
                }
            }

            if (hasEmbeddedDevices()) {
                for (Device embeddedDevice : getEmbeddedDevices()) {
                    if (embeddedDevice != null)
                        errors.addAll(embeddedDevice.validate());
                }
            }
        }

        return errors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Device device = (Device) o;

        if (!identity.equals(device.identity)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return identity.hashCode();
    }

    public abstract D newInstance(UDN udn, UDAVersion version, DeviceType type, DeviceDetails details, Icon[] icons,
            S[] services, List<D> embeddedDevices) throws ValidationException;

    public abstract S newInstance(ServiceType serviceType, ServiceId serviceId, URI descriptorURI, URI controlURI,
            URI eventSubscriptionURI, Action<S>[] actions, StateVariable<S>[] stateVariables)
            throws ValidationException;

    public abstract D[] toDeviceArray(Collection<D> col);

    public abstract S[] newServiceArray(int size);

    public abstract S[] toServiceArray(Collection<S> col);

    public abstract Resource[] discoverResources(Namespace namespace);

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") Identity: " + getIdentity().toString() + ", Root: " + isRoot();
    }
}
