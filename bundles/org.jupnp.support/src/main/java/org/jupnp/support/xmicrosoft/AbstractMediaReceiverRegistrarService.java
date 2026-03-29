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
package org.jupnp.support.xmicrosoft;

import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.binding.annotations.UpnpStateVariables;
import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.types.UnsignedIntegerFourBytes;

/**
 * Basic implementation of service required by MSFT devices such as XBox 360.
 *
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
@UpnpService(serviceId = @UpnpServiceId(namespace = "microsoft.com", value = "X_MS_MediaReceiverRegistrar"), serviceType = @UpnpServiceType(namespace = "microsoft.com", value = "X_MS_MediaReceiverRegistrar", version = 1))
@UpnpStateVariables({ @UpnpStateVariable(name = "A_ARG_TYPE_DeviceID", sendEvents = false, datatype = "string"),
        @UpnpStateVariable(name = "A_ARG_TYPE_Result", sendEvents = false, datatype = "int"),
        @UpnpStateVariable(name = "A_ARG_TYPE_RegistrationReqMsg", sendEvents = false, datatype = "bin.base64"),
        @UpnpStateVariable(name = "A_ARG_TYPE_RegistrationRespMsg", sendEvents = false, datatype = "bin.base64") })
public abstract class AbstractMediaReceiverRegistrarService {

    protected final PropertyChangeSupport propertyChangeSupport;

    @UpnpStateVariable(eventMinimumDelta = 1)
    private UnsignedIntegerFourBytes authorizationGrantedUpdateID = new UnsignedIntegerFourBytes(0);

    @UpnpStateVariable(eventMinimumDelta = 1)
    private UnsignedIntegerFourBytes authorizationDeniedUpdateID = new UnsignedIntegerFourBytes(0);

    @UpnpStateVariable
    private UnsignedIntegerFourBytes validationSucceededUpdateID = new UnsignedIntegerFourBytes(0);

    @UpnpStateVariable
    private UnsignedIntegerFourBytes validationRevokedUpdateID = new UnsignedIntegerFourBytes(0);

    protected AbstractMediaReceiverRegistrarService() {
        this(null);
    }

    protected AbstractMediaReceiverRegistrarService(PropertyChangeSupport propertyChangeSupport) {
        this.propertyChangeSupport = propertyChangeSupport != null ? propertyChangeSupport
                : new PropertyChangeSupport(this);
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "AuthorizationGrantedUpdateID"))
    public UnsignedIntegerFourBytes getAuthorizationGrantedUpdateID() {
        return authorizationGrantedUpdateID;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "AuthorizationDeniedUpdateID"))
    public UnsignedIntegerFourBytes getAuthorizationDeniedUpdateID() {
        return authorizationDeniedUpdateID;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "ValidationSucceededUpdateID"))
    public UnsignedIntegerFourBytes getValidationSucceededUpdateID() {
        return validationSucceededUpdateID;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "ValidationRevokedUpdateID"))
    public UnsignedIntegerFourBytes getValidationRevokedUpdateID() {
        return validationRevokedUpdateID;
    }

    @UpnpAction(out = { @UpnpOutputArgument(name = "Result", stateVariable = "A_ARG_TYPE_Result") })
    public int isAuthorized(
            @UpnpInputArgument(name = "DeviceID", stateVariable = "A_ARG_TYPE_DeviceID") String deviceID) {
        return 1;
    }

    @UpnpAction(out = { @UpnpOutputArgument(name = "Result", stateVariable = "A_ARG_TYPE_Result") })
    public int isValidated(
            @UpnpInputArgument(name = "DeviceID", stateVariable = "A_ARG_TYPE_DeviceID") String deviceID) {
        return 1;
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "RegistrationRespMsg", stateVariable = "A_ARG_TYPE_RegistrationRespMsg") })
    public byte[] registerDevice(
            @UpnpInputArgument(name = "RegistrationReqMsg", stateVariable = "A_ARG_TYPE_RegistrationReqMsg") byte[] registrationReqMsg) {
        return new byte[] {};
    }
}
