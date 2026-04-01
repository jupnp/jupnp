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

import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.model.profile.RemoteClientInfo;

@UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
public class SwitchPowerWithInterruption {

    @UpnpStateVariable(sendEvents = false)
    private boolean target = false;

    @UpnpStateVariable
    private boolean status = false;

    @UpnpAction
    public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue,
            RemoteClientInfo remoteClientInfo) throws InterruptedException {
        target = newTargetValue;
        status = newTargetValue;

        boolean interrupted = false;
        while (!interrupted) {
            // Do some long-running work and periodically test if you should continue...

            // ... for local service invocation
            if (Thread.interrupted()) {
                interrupted = true;
            }

            // ... for remote service invocation
            if (remoteClientInfo != null && remoteClientInfo.isRequestCancelled()) {
                interrupted = true;
            }
        }
        throw new InterruptedException("Execution interrupted");
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "RetTargetValue"))
    public boolean getTarget() {
        return target;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "ResultStatus"))
    public boolean getStatus() {
        return status;
    }
}
