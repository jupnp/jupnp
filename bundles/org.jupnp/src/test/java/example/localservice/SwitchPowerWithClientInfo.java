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

import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.binding.annotations.UpnpStateVariables;
import org.jupnp.model.profile.RemoteClientInfo;

@UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
@UpnpStateVariables({ @UpnpStateVariable(name = "Target", defaultValue = "0", sendEvents = false),
        @UpnpStateVariable(name = "Status", defaultValue = "0") })
public class SwitchPowerWithClientInfo {

    private boolean power;

    @UpnpAction
    public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue,
            RemoteClientInfo clientInfo) {
        power = newTargetValue;
        System.out.println("Switch is: " + power);

        if (clientInfo != null) {
            System.out.println("Client's address is: " + clientInfo.getRemoteAddress());
            System.out.println("Received message on: " + clientInfo.getLocalAddress());
            System.out.println("Client's user agent is: " + clientInfo.getRequestUserAgent());
            System.out.println(
                    "Client's custom header is: " + clientInfo.getRequestHeaders().getFirstHeader("X-MY-HEADER"));

            // Return some extra headers in the response
            clientInfo.getExtraResponseHeaders().add("X-MY-HEADER", "foobar");
        }
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
    public boolean getTarget() {
        return power;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
    public boolean getStatus() {
        return power;
    }
}
