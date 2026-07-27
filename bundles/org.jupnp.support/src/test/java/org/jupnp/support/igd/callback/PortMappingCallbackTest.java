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
package org.jupnp.support.igd.callback;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.message.UpnpResponse;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.model.PortMapping;

class PortMappingCallbackTest {

    @Test
    void addUsesPortDatatypeAdvertisedByTheService() throws Exception {
        final PortMappingAdd callback = new PortMappingAdd(portMappingService(Datatype.Builtin.UI4.getDatatype(), true),
                mapping()) {
            @Override
            public void success(final ActionInvocation invocation) {
            }

            @Override
            public void failure(final ActionInvocation invocation, final UpnpResponse operation,
                    final String defaultMsg) {
            }
        };
        final ActionInvocation<?> invocation = callback.getActionInvocation();

        assertInstanceOf(UnsignedIntegerFourBytes.class, invocation.getInput("NewExternalPort").getValue());
        assertInstanceOf(UnsignedIntegerFourBytes.class, invocation.getInput("NewInternalPort").getValue());
    }

    @Test
    void deleteUsesPortDatatypeAdvertisedByTheService() throws Exception {
        final PortMappingDelete callback = new PortMappingDelete(
                portMappingService(Datatype.Builtin.UI4.getDatatype(), false), mapping()) {
            @Override
            public void success(final ActionInvocation invocation) {
            }

            @Override
            public void failure(final ActionInvocation invocation, final UpnpResponse operation,
                    final String defaultMsg) {
            }
        };
        final ActionInvocation<?> invocation = callback.getActionInvocation();

        assertInstanceOf(UnsignedIntegerFourBytes.class, invocation.getInput("NewExternalPort").getValue());
    }

    @Test
    void addAllowsUnsetPorts() throws Exception {
        final PortMappingAdd callback = new PortMappingAdd(portMappingService(Datatype.Builtin.UI4.getDatatype(), true),
                new PortMapping()) {
            @Override
            public void success(final ActionInvocation invocation) {
            }

            @Override
            public void failure(final ActionInvocation invocation, final UpnpResponse operation,
                    final String defaultMsg) {
            }
        };
        final ActionInvocation<?> invocation = callback.getActionInvocation();

        assertNull(invocation.getInput("NewExternalPort").getValue());
        assertNull(invocation.getInput("NewInternalPort").getValue());
    }

    private PortMapping mapping() {
        return new PortMapping(36743, "192.168.1.99", PortMapping.Protocol.TCP, "jUPnP test");
    }

    private RemoteService portMappingService(final Datatype<?> portDatatype, final boolean add) throws Exception {
        final StateVariable[] variables = new StateVariable[] { variable("ExternalPort", portDatatype),
                variable("InternalPort", portDatatype), variable("Protocol", Datatype.Builtin.STRING.getDatatype()),
                variable("InternalClient", Datatype.Builtin.STRING.getDatatype()),
                variable("LeaseDuration", Datatype.Builtin.UI4.getDatatype()),
                variable("Enabled", Datatype.Builtin.BOOLEAN.getDatatype()),
                variable("Description", Datatype.Builtin.STRING.getDatatype()) };
        final Action action = add
                ? new Action("AddPortMapping",
                        new ActionArgument[] { input("NewExternalPort", "ExternalPort"),
                                input("NewProtocol", "Protocol"), input("NewInternalClient", "InternalClient"),
                                input("NewInternalPort", "InternalPort"), input("NewLeaseDuration", "LeaseDuration"),
                                input("NewEnabled", "Enabled"), input("NewPortMappingDescription", "Description") })
                : new Action("DeletePortMapping", new ActionArgument[] { input("NewExternalPort", "ExternalPort"),
                        input("NewProtocol", "Protocol") });

        return new RemoteService(new UDAServiceType("WANIPConnection", 1), new UDAServiceId("WANIPConnection"),
                URI.create("service.xml"), URI.create("control"), URI.create("events"), new Action[] { action },
                variables);
    }

    private StateVariable variable(final String name, final Datatype<?> datatype) {
        return new StateVariable(name, new StateVariableTypeDetails(datatype));
    }

    private ActionArgument input(final String name, final String stateVariable) {
        return new ActionArgument(name, stateVariable, ActionArgument.Direction.IN);
    }
}
