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
package example.controlpoint;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Future;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.DefaultServiceManager;
import org.jupnp.model.action.ActionCancelledException;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.types.UDAServiceId;

import example.binarylight.BinaryLightSampleData;

class ActionCancellationTest {

    static LocalService bindService(Class<?> clazz) throws Exception {
        LocalServiceBinder binder = new AnnotationLocalServiceBinder();
        LocalService svc = binder.read(clazz);
        svc.setManager(new DefaultServiceManager(svc, clazz));
        return svc;
    }

    static Object[][] getDevices() throws Exception {
        return new LocalDevice[][] {
                { BinaryLightSampleData.createDevice(bindService(SwitchPowerWithInterruption.class)) }, };
    }

    @ParameterizedTest
    @MethodSource("getDevices")
    void invokeActions(LocalDevice device) throws Exception {
        final boolean[] tests = new boolean[1];

        MockUpnpService upnpService = new MockUpnpService(false, false, true);
        upnpService.startup();
        LocalService service = device.findService(new UDAServiceId("SwitchPower"));
        Action action = service.getAction("SetTarget");

        ActionInvocation setTargetInvocation = new ActionInvocation(action);
        setTargetInvocation.setInput("NewTargetValue", true);

        ActionCallback setTargetCallback = new ActionCallback(setTargetInvocation) {
            @Override
            public void success(ActionInvocation invocation) {
                // Will not be called if invocation has been cancelled
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                if (invocation.getFailure() instanceof ActionCancelledException) {
                    // Handle the cancellation here...
                    tests[0] = true;
                }
            }
        };

        Future future = upnpService.getControlPoint().execute(setTargetCallback);
        Thread.sleep(500);
        future.cancel(true);

        Thread.sleep(500);
        for (boolean test : tests) {
            assertTrue(test);
        }
    }
}
