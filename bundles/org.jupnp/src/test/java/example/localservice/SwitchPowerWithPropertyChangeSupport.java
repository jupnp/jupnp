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

import org.jupnp.binding.annotations.*;
import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;

/**
 * @author Jochen Hiller - Changed to use Compact2 compliant Java Beans
 */
@UpnpService(serviceId = @UpnpServiceId("SwitchPower"), serviceType = @UpnpServiceType(value = "SwitchPower", version = 1))
public class SwitchPowerWithPropertyChangeSupport {

    private final PropertyChangeSupport propertyChangeSupport;

    public SwitchPowerWithPropertyChangeSupport() {
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    @UpnpStateVariable(defaultValue = "0", sendEvents = false)
    private boolean target = false;

    @UpnpStateVariable(defaultValue = "0")
    private boolean status = false;

    @UpnpAction
    public void setTarget(@UpnpInputArgument(name = "NewTargetValue") boolean newTargetValue) {

        boolean targetOldValue = target;
        target = newTargetValue;
        boolean statusOldValue = status;
        status = newTargetValue;

        // These have no effect on the UPnP monitoring but it's JavaBean compliant
        getPropertyChangeSupport().firePropertyChange("target", targetOldValue, target);
        getPropertyChangeSupport().firePropertyChange("status", statusOldValue, status);

        // This will send a UPnP event, it's the name of a state variable that triggers events
        getPropertyChangeSupport().firePropertyChange("Status", statusOldValue, status);
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
