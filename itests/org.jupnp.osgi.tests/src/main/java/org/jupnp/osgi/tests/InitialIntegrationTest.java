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

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.jupnp.common.data.TestData;
import org.jupnp.common.data.TestDataFactory;
import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionArgumentValue;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.model.types.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InitialIntegrationTest extends BaseIntegration {

    static final String INITIAL_TEST_DATA_ID = "initial";
    static final String SET_TEST_DATA_ID = "set";

    static final Logger LOGGER = LoggerFactory.getLogger(InitialIntegrationTest.class);

    static final String DEVICE_TYPE = "urn:schemas-4thline-com:device:simple-test:1";
    static final String SERVICE_TYPE = "urn:schemas-4thline-com:service:SimpleTest:1";

    static class GetTargetActionInvocation extends ActionInvocation {
        GetTargetActionInvocation(Service service, String name) {
            super(service.getAction(name));
        }
    }

    void doSimpleDeviceGetAction(final String name, String testDataId) {
        waitForAssert(() -> {
            Device device = getDevice(ServiceType.valueOf(SERVICE_TYPE));
            assertNotNull(device);
            Service service = getService(device, ServiceType.valueOf(SERVICE_TYPE));
            assertNotNull(service);
            Action action = getAction(service, name);
            assertNotNull(action);
        });

        Device device = getDevice(ServiceType.valueOf(SERVICE_TYPE));
        Service service = getService(device, ServiceType.valueOf(SERVICE_TYPE));

        TestData data = TestDataFactory.getInstance().getTestData(testDataId);
        assertNotNull(data);

        AtomicBoolean successful = new AtomicBoolean(false);

        ActionInvocation setTargetInvocation = new GetTargetActionInvocation(service, name);
        getUpnpService().getControlPoint().execute(new ActionCallback(setTargetInvocation) {

            @Override
            public void success(ActionInvocation invocation) {
                LOGGER.info("Successfully called action '{}'", name);
                ActionArgumentValue[] outputs = invocation.getOutput();
                for (ActionArgumentValue output : outputs) {
                    ActionArgument argument = output.getArgument();
                    String name = argument.getName();
                    String type = name;
                    Object value = output.getValue();
                    Object desired = data.getOSGiUPnPValue(name, type);

                    assertTrue(validate(name, type, value, desired));
                }
                successful.set(true);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                fail(String.format("Failed calling action '%s': %s", name, defaultMsg));
            }
        });

        waitForAssert(() -> assertTrue(successful.get()));
    }

    static class SetTargetActionInvocation extends ActionInvocation {
        SetTargetActionInvocation(Service service, String name, TestData data) {
            super(service.getAction(name));
            LOGGER.debug("@@@ name: {}  inputs: {}", name, getAction().getInputArguments().length);
            ActionArgument lastArgument = null;
            String argumentType = "";
            Object argumentObject = "";
            try {
                for (ActionArgument argument : getAction().getInputArguments()) {
                    lastArgument = argument;

                    LOGGER.debug("@@@ argument: {}", argument);
                    argumentType = argument.getDatatype().getBuiltin().getDescriptorName();
                    LOGGER.debug("@@@ type: {}", argumentType);

                    argumentObject = data.getOSGiUPnPValue(argument.getName(), argumentType);
                    LOGGER.debug("@@@ object: {}", argumentObject);
                    argumentObject = data.getjUPnPUPnPValue(argumentType, argumentObject);
                    LOGGER.debug("@@@ type: {}  value: {} ({})", argumentType, argumentObject,
                            argumentObject.getClass().getName());
                    setInput(argument.getName(), argumentObject);
                }
            } catch (InvalidValueException e) {
                InvalidValueException exception = LOGGER.isDebugEnabled() ? e : null;
                LOGGER.warn("Invalid value while setting '{}' argument '{}' of type '{}' to: {}", name, lastArgument,
                        argumentType, argumentObject, exception);
            }
        }
    }

    void doSimpleDeviceSetAction(final String name, String testDataId) {
        waitForAssert(() -> {
            Device device = getDevice(ServiceType.valueOf(SERVICE_TYPE));
            assertNotNull(device);
            Service service = getService(device, ServiceType.valueOf(SERVICE_TYPE));
            assertNotNull(service);
            Action action = getAction(service, name);
            assertNotNull(action);
        });

        Device device = getDevice(ServiceType.valueOf(SERVICE_TYPE));
        Service service = getService(device, ServiceType.valueOf(SERVICE_TYPE));

        TestData data = TestDataFactory.getInstance().getTestData(testDataId);
        assertNotNull(data);

        AtomicBoolean successful = new AtomicBoolean(false);

        ActionInvocation setTargetInvocation = new SetTargetActionInvocation(service, name, data);
        getUpnpService().getControlPoint().execute(new ActionCallback(setTargetInvocation) {

            @Override
            public void success(ActionInvocation invocation) {
                LOGGER.info("Successfully called action '{}'", name);
                successful.set(true);
            }

            @Override
            public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                fail(String.format("Failed calling action '%s': %s", name, defaultMsg));
            }
        });

        waitForAssert(() -> assertTrue(successful.get()));
    }

    @Test
    @Order(0)
    void testSimpleDeviceGetAllVariablesAction() {
        doSimpleDeviceGetAction("GetAllVariables", INITIAL_TEST_DATA_ID);
    }

    @Test
    @Order(1)
    void testSimpleDeviceSetAllVariablesAction() {
        doSimpleDeviceSetAction("SetAllVariables", SET_TEST_DATA_ID);
        doSimpleDeviceGetAction("GetAllVariables", SET_TEST_DATA_ID);
    }
}
