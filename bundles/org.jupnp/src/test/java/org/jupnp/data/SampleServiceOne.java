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
package org.jupnp.data;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URL;

import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.Service;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableAllowedValueRange;
import org.jupnp.model.meta.StateVariableEventDetails;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.util.URIUtil;

/**
 * @author Christian Bauer
 */
public class SampleServiceOne extends SampleService {

    public static URL getDescriptorURL() {
        return URIUtil.createAbsoluteURL(SampleDeviceRoot.getDeviceDescriptorURL(), getThisDescriptorURI());
    }

    public static URI getThisDescriptorURI() {
        return URI.create("service/upnp-org/MY-SERVICE-123/desc");
    }

    public static URI getThisControlURI() {
        return URI.create("service/upnp-org/MY-SERVICE-123/control");
    }

    public static URI getThisEventSubscriptionURI() {
        return URI.create("service/upnp-org/MY-SERVICE-123/events");
    }

    public static ServiceId getThisServiceId() {
        return new UDAServiceId("MY-SERVICE-123");
    }

    public static ServiceType getThisServiceType() {
        return new UDAServiceType("MY-SERVICE-TYPE-ONE", 1);
    }

    @Override
    public ServiceType getServiceType() {
        return getThisServiceType();
    }

    @Override
    public ServiceId getServiceId() {
        return getThisServiceId();
    }

    @Override
    public URI getDescriptorURI() {
        return getThisDescriptorURI();
    }

    @Override
    public URI getControlURI() {
        return getThisControlURI();
    }

    @Override
    public URI getEventSubscriptionURI() {
        return getThisEventSubscriptionURI();
    }

    @Override
    public Action[] getActions() {
        return new Action[] {
                new Action("SetTarget",
                        new ActionArgument[] {
                                new ActionArgument("NewTargetValue", "Target", ActionArgument.Direction.IN) }),
                new Action("GetTarget",
                        new ActionArgument[] {
                                new ActionArgument("RetTargetValue", "Target", ActionArgument.Direction.OUT, true) }),
                new Action("GetStatus", new ActionArgument[] {
                        new ActionArgument("ResultStatus", "Status", ActionArgument.Direction.OUT) }) };
    }

    @Override
    public StateVariable[] getStateVariables() {
        return new StateVariable[] {
                new StateVariable("Target", new StateVariableTypeDetails(Datatype.Builtin.BOOLEAN.getDatatype(), "0"),
                        new StateVariableEventDetails(false)),
                new StateVariable("Status", new StateVariableTypeDetails(Datatype.Builtin.BOOLEAN.getDatatype(), "0")),
                new StateVariable("SomeVar",
                        new StateVariableTypeDetails(Datatype.Builtin.STRING.getDatatype(), "foo",
                                new String[] { "foo", "bar" }, null)),
                new StateVariable("AnotherVar",
                        new StateVariableTypeDetails(Datatype.Builtin.UI4.getDatatype(), null, null,
                                new StateVariableAllowedValueRange(0, 10, 2)),
                        new StateVariableEventDetails(false)),
                new StateVariable("ModeratedMaxRateVar",
                        new StateVariableTypeDetails(Datatype.Builtin.STRING.getDatatype()),
                        new StateVariableEventDetails(true, 500, 0)),
                new StateVariable("ModeratedMinDeltaVar",
                        new StateVariableTypeDetails(Datatype.Builtin.I4.getDatatype()),
                        new StateVariableEventDetails(true, 0, 3)), };
    }

    public static void assertMatch(Service a, Service b) {

        assertEquals(a.getActions().length, b.getActions().length);

        assertEquals(a.getAction("SetTarget").getName(), b.getAction("SetTarget").getName());
        assertEquals(a.getAction("SetTarget").getArguments().length, b.getAction("SetTarget").getArguments().length);
        assertEquals(a.getAction("SetTarget").getArguments()[0].getName(),
                a.getAction("SetTarget").getArguments()[0].getName());
        assertEquals(a.getAction("SetTarget").getArguments()[0].getDirection(),
                b.getAction("SetTarget").getArguments()[0].getDirection());
        assertEquals(a.getAction("SetTarget").getArguments()[0].getRelatedStateVariableName(),
                b.getAction("SetTarget").getArguments()[0].getRelatedStateVariableName());

        assertEquals(a.getAction("GetTarget").getArguments()[0].getName(),
                b.getAction("GetTarget").getArguments()[0].getName());
        // TODO: UPNP VIOLATION: WMP12 will discard RenderingControl service if it contains <retval> tags
        // assertEquals(a.getAction("GetTarget").getArguments()[0].isReturnValue(),
        // b.getAction("GetTarget").getArguments()[0].isReturnValue());

        assertEquals(a.getStateVariables().length, b.getStateVariables().length);
        assertNotNull(a.getStateVariable("Target"));
        assertNotNull(b.getStateVariable("Target"));
        assertNotNull(a.getStateVariable("Status"));
        assertNotNull(b.getStateVariable("Status"));
        assertNotNull(a.getStateVariable("SomeVar"));
        assertNotNull(b.getStateVariable("SomeVar"));

        assertEquals(a.getStateVariable("Target").getName(), "Target");
        assertEquals(a.getStateVariable("Target").getEventDetails().isSendEvents(),
                b.getStateVariable("Target").getEventDetails().isSendEvents());

        assertEquals(a.getStateVariable("Status").getName(), "Status");
        assertEquals(a.getStateVariable("Status").getEventDetails().isSendEvents(),
                b.getStateVariable("Status").getEventDetails().isSendEvents());
        assertEquals(a.getStateVariable("Status").getTypeDetails().getDatatype(),
                Datatype.Builtin.BOOLEAN.getDatatype());

        assertEquals(a.getStateVariable("SomeVar").getTypeDetails().getAllowedValues().length,
                b.getStateVariable("SomeVar").getTypeDetails().getAllowedValues().length);
        assertEquals(a.getStateVariable("SomeVar").getTypeDetails().getDefaultValue(),
                b.getStateVariable("SomeVar").getTypeDetails().getDefaultValue());
        assertEquals(a.getStateVariable("SomeVar").getTypeDetails().getAllowedValues()[0],
                b.getStateVariable("SomeVar").getTypeDetails().getAllowedValues()[0]);
        assertEquals(a.getStateVariable("SomeVar").getTypeDetails().getAllowedValues()[1],
                b.getStateVariable("SomeVar").getTypeDetails().getAllowedValues()[1]);
        assertEquals(a.getStateVariable("SomeVar").getEventDetails().isSendEvents(),
                b.getStateVariable("SomeVar").getEventDetails().isSendEvents());

        assertEquals(a.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getMinimum(),
                b.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getMinimum());
        assertEquals(a.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getMaximum(),
                b.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getMaximum());
        assertEquals(a.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getStep(),
                b.getStateVariable("AnotherVar").getTypeDetails().getAllowedValueRange().getStep());
        assertEquals(a.getStateVariable("AnotherVar").getEventDetails().isSendEvents(),
                b.getStateVariable("AnotherVar").getEventDetails().isSendEvents());
    }
}
