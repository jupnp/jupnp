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
package org.jupnp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.data.SampleData;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests for the specification violations reported by {@link Action#validate()}.
 */
class ActionValidationTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(SpecificationViolationReporter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger.addAppender(appender);
        SpecificationViolationReporter.enableReporting();
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void reportsNothingForSingleReturnValueAsFirstOutArgument() throws Exception {
        createRemoteDevice(new ActionArgument<>("In", "Foo", ActionArgument.Direction.IN),
                new ActionArgument<>("Out0", "Foo", ActionArgument.Direction.OUT, true),
                new ActionArgument<>("Out1", "Foo", ActionArgument.Direction.OUT));

        assertEquals(List.of(), reports());
    }

    @Test
    void reportsReturnValueBehindOtherOutArgumentsOncePerAction() throws Exception {
        RemoteDevice device = createRemoteDevice(new ActionArgument<>("Out0", "Foo", ActionArgument.Direction.OUT),
                new ActionArgument<>("Out1", "Foo", ActionArgument.Direction.OUT),
                new ActionArgument<>("Out2", "Foo", ActionArgument.Direction.OUT),
                new ActionArgument<>("Out3", "Foo", ActionArgument.Direction.OUT, true));

        assertEquals(
                List.of("UPnP specification violation of device '" + device
                        + "': Argument 'Out3' of action 'GetFoo' is <retval/> but not the first OUT argument"),
                reports());
    }

    @Test
    void reportsMultipleReturnValuesOncePerAction() throws Exception {
        ActionArgument<RemoteService>[] arguments = new ActionArgument[50];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = new ActionArgument<>("Out" + i, "Foo", ActionArgument.Direction.OUT, true);
        }
        RemoteDevice device = createRemoteDevice(arguments);

        assertEquals(
                List.of("UPnP specification violation of device '" + device
                        + "': Only one argument of action 'GetFoo' can be <retval/>",
                        "UPnP specification violation of device '" + device
                                + "': Argument 'Out49' of action 'GetFoo' is <retval/> but not the first OUT argument"),
                reports());
        assertEquals(1, device.getServices()[0].getActions().length);
    }

    @SafeVarargs
    private static RemoteDevice createRemoteDevice(ActionArgument<RemoteService>... arguments) throws Exception {
        RemoteService service = new RemoteService(new UDAServiceType("Foo", 1), new UDAServiceId("Foo"),
                URI.create("/scpd.xml"), URI.create("/control"), URI.create("/events"),
                new Action[] { new Action<>("GetFoo", arguments) }, new StateVariable[] { new StateVariable<>("Foo",
                        new StateVariableTypeDetails(Datatype.Builtin.STRING.getDatatype())) });
        return new RemoteDevice(SampleData.createRemoteDeviceIdentity(), new UDADeviceType("Foo", 1),
                new DeviceDetails("Foo"), service);
    }

    private List<String> reports() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }
}
