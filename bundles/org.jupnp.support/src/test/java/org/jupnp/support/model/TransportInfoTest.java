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
package org.jupnp.support.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.jupnp.binding.annotations.AnnotationLocalServiceBinder;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.LocalService;
import org.jupnp.support.avtransport.impl.AVTransportService;

class TransportInfoTest {

    private final LocalService<?> avTransport = new AnnotationLocalServiceBinder().read(AVTransportService.class);

    @Test
    void vendorDefinedValuesAreNotSharedBetweenResponses() {
        final TransportInfo first = response("X_FIRST_STATE", "X_FIRST_STATUS");
        final TransportInfo second = response("X_SECOND_STATE", "X_SECOND_STATUS");

        assertSame(TransportState.CUSTOM, first.getCurrentTransportState());
        assertSame(TransportStatus.CUSTOM, first.getCurrentTransportStatus());
        assertEquals("X_FIRST_STATE", first.getCurrentTransportStateValue());
        assertEquals("X_FIRST_STATUS", first.getCurrentTransportStatusValue());

        assertSame(TransportState.CUSTOM, second.getCurrentTransportState());
        assertSame(TransportStatus.CUSTOM, second.getCurrentTransportStatus());
        assertEquals("X_SECOND_STATE", second.getCurrentTransportStateValue());
        assertEquals("X_SECOND_STATUS", second.getCurrentTransportStatusValue());
    }

    @Test
    void vendorDefinedValuesDoNotModifyTheEnumConstants() {
        response("X_STATE", "X_STATUS");

        assertEquals("CUSTOM", TransportState.CUSTOM.getValue());
        assertEquals("CUSTOM", TransportStatus.CUSTOM.getValue());
    }

    @Test
    void standardValuesAreReportedByName() {
        final TransportInfo fromResponse = response("PAUSED_PLAYBACK", "ERROR_OCCURRED");

        assertSame(TransportState.PAUSED_PLAYBACK, fromResponse.getCurrentTransportState());
        assertSame(TransportStatus.ERROR_OCCURRED, fromResponse.getCurrentTransportStatus());
        assertEquals("PAUSED_PLAYBACK", fromResponse.getCurrentTransportStateValue());
        assertEquals("ERROR_OCCURRED", fromResponse.getCurrentTransportStatusValue());

        final TransportInfo created = new TransportInfo(TransportState.PLAYING);

        assertEquals("PLAYING", created.getCurrentTransportStateValue());
        assertEquals("OK", created.getCurrentTransportStatusValue());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private TransportInfo response(final String state, final String status) {
        final ActionInvocation invocation = new ActionInvocation(avTransport.getAction("GetTransportInfo"));
        invocation.setOutput("CurrentTransportState", state);
        invocation.setOutput("CurrentTransportStatus", status);
        invocation.setOutput("CurrentSpeed", "1");
        return new TransportInfo(invocation.getOutputMap());
    }
}
