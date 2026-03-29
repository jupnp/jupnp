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
package org.jupnp.resources;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.UpnpService;
import org.jupnp.binding.xml.DescriptorBindingException;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.RecoveringUDA10DeviceDescriptorBinderImpl;
import org.jupnp.binding.xml.UDA10DeviceDescriptorBinderSAXImpl;
import org.jupnp.data.SampleData;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.mock.MockUpnpServiceConfiguration;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.util.io.IO;

/**
 * @author Christian Bauer
 */
class InvalidUDA10DeviceDescriptorParsingTest {

    static String[][] getStrict() {
        return new String[][] { { "/invalidxml/device/atb_miviewtv.xml" }, { "/invalidxml/device/doubletwist.xml" },
                { "/invalidxml/device/eyetv_netstream_sat.xml" }, { "/invalidxml/device/makemkv.xml" },
                { "/invalidxml/device/tpg.xml" }, { "/invalidxml/device/ceton_infinitv.xml" },
                { "/invalidxml/device/zyxel_miviewtv.xml" }, { "/invalidxml/device/perfectwave.xml" },
                { "/invalidxml/device/escient.xml" }, { "/invalidxml/device/eyecon.xml" },
                { "/invalidxml/device/kodak.xml" }, { "/invalidxml/device/plutinosoft.xml" },
                { "/invalidxml/device/samsung.xml" }, { "/invalidxml/device/philips_hue.xml" }, };
    }

    static String[][] getRecoverable() {
        return new String[][] { { "/invalidxml/device/missing_namespaces.xml" }, { "/invalidxml/device/ushare.xml" },
                { "/invalidxml/device/lg.xml" },
                // TODO: This test is currently failing!
                // {"/invalidxml/device/readydlna.xml"},
        };
    }

    static String[][] getUnrecoverable() {
        return new String[][] { { "/invalidxml/device/unrecoverable/pms.xml" },
                { "/invalidxml/device/unrecoverable/awox.xml" }, { "/invalidxml/device/philips.xml" },
                { "/invalidxml/device/simplecenter.xml" }, { "/invalidxml/device/ums.xml" }, };
    }

    /* ############################## TEST FAILURE ############################ */

    @ParameterizedTest
    @MethodSource("getRecoverable")
    void readFailure(String recoverable) {
        assertThrows(DescriptorBindingException.class, () -> readDevice(recoverable, new MockUpnpService()));
    }

    @ParameterizedTest
    @MethodSource("getUnrecoverable")
    void readRecoveringFailure(String unrecoverable) {
        assertThrows(Exception.class,
                () -> readDevice(unrecoverable, new MockUpnpService(new MockUpnpServiceConfiguration() {
                    @Override
                    public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
                        return new RecoveringUDA10DeviceDescriptorBinderImpl();
                    }
                })));
    }

    /* ############################## TEST SUCCESS ############################ */

    @ParameterizedTest
    @MethodSource("getStrict")
    void readDefault(String strict) throws Exception {
        readDevice(strict, new MockUpnpService());
    }

    @ParameterizedTest
    @MethodSource("getStrict")
    void readSAX(String strict) throws Exception {
        readDevice(strict, new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
                return new UDA10DeviceDescriptorBinderSAXImpl();
            }
        }));
    }

    @ParameterizedTest
    @MethodSource("getStrict")
    void readRecoveringStrict(String strict) throws Exception {
        readDevice(strict, new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
                return new RecoveringUDA10DeviceDescriptorBinderImpl();
            }
        }));
    }

    @ParameterizedTest
    @MethodSource("getRecoverable")
    void readRecovering(String recoverable) throws Exception {
        readDevice(recoverable, new MockUpnpService(new MockUpnpServiceConfiguration() {
            @Override
            public DeviceDescriptorBinder getDeviceDescriptorBinderUDA10() {
                return new RecoveringUDA10DeviceDescriptorBinderImpl();
            }
        }));
    }

    protected void readDevice(String invalidXMLFile, UpnpService upnpService) throws Exception {
        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        upnpService.getConfiguration().getDeviceDescriptorBinderUDA10().describe(device,
                IO.readLines(getClass().getResourceAsStream(invalidXMLFile)));
    }
}
