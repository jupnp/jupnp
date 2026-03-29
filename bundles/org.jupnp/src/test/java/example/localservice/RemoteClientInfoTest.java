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
package example.localservice;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.data.SampleData;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.action.RemoteActionInvocation;
import org.jupnp.model.message.Connection;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.message.header.UserAgentHeader;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.profile.RemoteClientInfo;
import org.jupnp.model.types.UDADeviceType;

class RemoteClientInfoTest {

    static LocalDevice createTestDevice(Class serviceClass) throws Exception {

        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(serviceClass);
        svc.setManager(new DefaultServiceManager(svc, serviceClass));

        return new LocalDevice(SampleData.createLocalDeviceIdentity(), new UDADeviceType("BinaryLight", 1),
                new DeviceDetails("Example Binary Light"), svc);
    }

    static Object[][] getDevices() throws Exception {
        return new LocalDevice[][] { { createTestDevice(SwitchPowerWithClientInfo.class) } };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) {
        LocalService svc = device.getServices()[0];

        UpnpHeaders requestHeaders = new UpnpHeaders();
        requestHeaders.add(UpnpHeader.Type.USER_AGENT, new UserAgentHeader("foo/bar"));
        requestHeaders.add("X-MY-HEADER", "foo");

        RemoteClientInfo clientInfo = new RemoteClientInfo(new Connection() {
            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public InetAddress getRemoteAddress() {
                try {
                    return InetAddress.getByName("10.0.0.1");
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public InetAddress getLocalAddress() {
                try {
                    return InetAddress.getByName("10.0.0.2");
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            }
        }, requestHeaders);

        ActionInvocation setTargetInvocation = new RemoteActionInvocation(svc.getAction("SetTarget"), clientInfo);

        setTargetInvocation.setInput("NewTargetValue", true);
        svc.getExecutor(setTargetInvocation.getAction()).execute(setTargetInvocation);
        assertNull(setTargetInvocation.getFailure());
        assertEquals(0, setTargetInvocation.getOutput().length);

        assertEquals("foobar", clientInfo.getExtraResponseHeaders().getFirstHeader("X-MY-HEADER"));
    }
}
