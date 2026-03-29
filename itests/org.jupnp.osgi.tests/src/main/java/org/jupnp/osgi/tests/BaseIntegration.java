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
package org.jupnp.osgi.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.jupnp.UpnpService;
import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.common.util.BundleUtil;
import org.jupnp.common.util.DataUtil;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UnsignedVariableInteger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.upnp.UPnPDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseIntegration {

    private final Logger logger = LoggerFactory.getLogger(BaseIntegration.class);

    protected BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
    protected UpnpService upnpService;

    @BeforeEach
    public void setup() {
        waitForAssert(() -> assertNotNull(getService(UpnpServiceConfiguration.class)));
        waitForAssert(() -> assertNotNull(getService(UpnpService.class)));
        waitForAssert(() -> assertNotNull(getService(UPnPDevice.class)));

        upnpService = getService(UpnpService.class);
    }

    public static void waitForAssert(Runnable assertion) {
        int sleepTime = 200;
        int timeout = 10000;

        long waitingTime = 0;
        while (waitingTime < timeout) {
            try {
                assertion.run();
                return;
            } catch (Error error) {
                waitingTime += sleepTime;
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    throw new IllegalStateException("We shouldn't be interrupted while testing");
                }
            }
        }
        assertion.run();
    }

    protected <T> T getService(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        ServiceReference<T> serviceReference = (ServiceReference<T>) bundleContext.getServiceReference(clazz.getName());

        return serviceReference == null ? null : bundleContext.getService(serviceReference);
    }

    public UpnpService getUpnpService() {
        return upnpService;
    }

    public void dumpEnvironment() {
        for (Object key : System.getenv().keySet()) {
            logger.info("{}: {}", key, System.getenv().get(key));
        }

        logger.info("current directory: {}", new File(".").getAbsolutePath());
    }

    public void dumpBundles(Bundle[] bundles) {
        logger.info("This is running inside Equinox.");
        for (Bundle bundle : bundles) {
            logger.info(String.format("%2d|%-12s| %s", bundle.getBundleId(), BundleUtil.getBundleState(bundle),
                    bundle.getSymbolicName()));
        }
    }

    public void dumpRegistry() {
        logger.info("*** UPnP Devices ***");
        for (Device device : upnpService.getRegistry().getDevices()) {
            logger.info("{}: {}", device.getIdentity().getUdn(), device.getType().getType());
        }
    }

    public void dumpUPnPDevice(UPnPDevice device) {
        logger.info("{}", device);
        for (Object key : Collections.list(device.getDescriptions(null).keys())) {
            logger.info("   {}: {}", key, device.getDescriptions(null).get(key));
        }
    }

    public Device getDevice(DeviceType type) {
        Collection<Device> devices = upnpService.getRegistry().getDevices(type);
        return devices.stream().findFirst().orElse(null);
    }

    public Device getDevice(ServiceType type) {
        Collection<Device> devices = upnpService.getRegistry().getDevices(type);
        return devices.stream().findFirst().orElse(null);
    }

    public Service getService(Device device, ServiceType type) {
        for (Service service : device.getServices()) {
            if (service.getServiceType().equals(type)) {
                return service;
            }
        }

        return null;
    }

    public Service getService(ServiceType type) {
        Device device = getDevice(type);
        for (Service service : device.getServices()) {
            if (service.getServiceType().equals(type)) {
                return service;
            }
        }

        return null;
    }

    public Action getAction(Service service, String name) {
        return service.getAction(name);
    }

    public String bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        for (byte b : bytes) {
            sb.append(String.format("0x%x ", b));
        }

        return sb.toString();
    }

    public byte[] toBytes(Byte[] Bytes) {
        byte[] bytes = new byte[Bytes.length];
        for (int i = 0; i < Bytes.length; i++) {
            bytes[i] = Bytes[i].byteValue();
        }

        return bytes;
    }

    public String valueToString(Object value) {
        String string;

        if (value == null) {
            string = "[null]";
        } else if (value instanceof byte[]) {
            string = bytesToString((byte[]) value);
        } else if (value instanceof Byte[]) {
            string = bytesToString(toBytes((Byte[]) value));
        } else {
            string = value.toString();
        }

        return string;
    }

    public boolean validate(String name, String type, Object value, Object desired) {
        boolean matches;

        logger.info("=========================================\n");
        logger.info("data type: {}", type);
        logger.info("    value: {} ({})", valueToString(value), value.getClass().getName());
        logger.info("  desired: {} ({})", valueToString(desired), desired.getClass().getName());

        if (value instanceof UnsignedVariableInteger) {
            value = Integer.valueOf(((UnsignedVariableInteger) value).getValue().intValue());
        } else if (value instanceof Calendar) {
            if (type.equals("time") || type.equals("time.tz")) {
                Calendar calendar = (Calendar) value;
                Date date = calendar.getTime();
                value = date.getTime() + calendar.getTimeZone().getOffset(date.getTime());
            } else {
                value = ((Calendar) value).getTime();
            }
        } else if (value instanceof Byte[]) {
            if (type.equals("bin.base64")) {
                value = Base64.getDecoder().decode(toBytes((Byte[]) value));
            } else {
                value = toBytes((Byte[]) value);
            }
        }

        if (value instanceof byte[]) {
            matches = DataUtil.compareBytes((byte[]) value, (byte[]) desired);
        } else {
            matches = value.equals(desired);

            if (!matches) {
                matches = value.toString().equals(desired.toString());
            }
        }

        logger.info("  matches: {}", matches);

        return matches;
    }
}
