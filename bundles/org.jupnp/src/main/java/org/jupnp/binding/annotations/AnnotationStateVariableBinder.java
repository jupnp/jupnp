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
package org.jupnp.binding.annotations;

import java.util.Set;

import org.jupnp.binding.AllowedValueProvider;
import org.jupnp.binding.AllowedValueRangeProvider;
import org.jupnp.binding.LocalServiceBindingException;
import org.jupnp.model.ModelUtil;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableAllowedValueRange;
import org.jupnp.model.meta.StateVariableEventDetails;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.Datatype;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer
 */
public class AnnotationStateVariableBinder {

    private final Logger logger = LoggerFactory.getLogger(AnnotationLocalServiceBinder.class);

    protected UpnpStateVariable annotation;
    protected String name;
    protected StateVariableAccessor accessor;
    protected Set<Class> stringConvertibleTypes;

    public AnnotationStateVariableBinder(UpnpStateVariable annotation, String name, StateVariableAccessor accessor,
            Set<Class> stringConvertibleTypes) {
        this.annotation = annotation;
        this.name = name;
        this.accessor = accessor;
        this.stringConvertibleTypes = stringConvertibleTypes;
    }

    public UpnpStateVariable getAnnotation() {
        return annotation;
    }

    public String getName() {
        return name;
    }

    public StateVariableAccessor getAccessor() {
        return accessor;
    }

    public Set<Class> getStringConvertibleTypes() {
        return stringConvertibleTypes;
    }

    protected StateVariable createStateVariable() throws LocalServiceBindingException {

        logger.trace("Creating state variable '{}' with accessor: {}", getName(), getAccessor());

        // Datatype
        Datatype datatype = createDatatype();

        // Default value
        String defaultValue = createDefaultValue(datatype);

        // Allowed values
        String[] allowedValues = null;
        if (Datatype.Builtin.STRING.equals(datatype.getBuiltin())) {

            if (getAnnotation().allowedValueProvider() != void.class) {
                allowedValues = getAllowedValuesFromProvider();
            } else if (getAnnotation().allowedValues().length > 0) {
                allowedValues = getAnnotation().allowedValues();
            } else if (getAnnotation().allowedValuesEnum() != void.class) {
                allowedValues = getAllowedValues(getAnnotation().allowedValuesEnum());
            } else if (getAccessor() != null && getAccessor().getReturnType().isEnum()) {
                allowedValues = getAllowedValues(getAccessor().getReturnType());
            } else {
                logger.trace("Not restricting allowed values (of string typed state var): {}", getName());
            }

            if (allowedValues != null && defaultValue != null) {

                // Check if the default value is an allowed value
                boolean foundValue = false;
                for (String s : allowedValues) {
                    if (s.equals(defaultValue)) {
                        foundValue = true;
                        break;
                    }
                }
                if (!foundValue) {
                    throw new LocalServiceBindingException(
                            "Default value '" + defaultValue + "' is not in allowed values of: " + getName());
                }
            }
        }

        // Allowed value range
        StateVariableAllowedValueRange allowedValueRange = null;
        if (Datatype.Builtin.isNumeric(datatype.getBuiltin())) {

            if (getAnnotation().allowedValueRangeProvider() != void.class) {
                allowedValueRange = getAllowedRangeFromProvider();
            } else if (getAnnotation().allowedValueMinimum() > 0 || getAnnotation().allowedValueMaximum() > 0) {
                allowedValueRange = getAllowedValueRange(getAnnotation().allowedValueMinimum(),
                        getAnnotation().allowedValueMaximum(), getAnnotation().allowedValueStep());
            } else {
                logger.trace("Not restricting allowed value range (of numeric typed state var): {}", getName());
            }

            // Check if the default value is an allowed value
            if (defaultValue != null && allowedValueRange != null) {

                long v;
                try {
                    v = Long.parseLong(defaultValue);
                } catch (Exception e) {
                    throw new LocalServiceBindingException("Default value '" + defaultValue
                            + "' is not numeric (for range checking) of: " + getName());
                }

                if (!allowedValueRange.isInRange(v)) {
                    throw new LocalServiceBindingException(
                            "Default value '" + defaultValue + "' is not in allowed range of: " + getName());
                }
            }
        }

        // Event details
        boolean sendEvents = getAnnotation().sendEvents();
        if (sendEvents && getAccessor() == null) {
            throw new LocalServiceBindingException(
                    "State variable sends events but has no accessor for field or getter: " + getName());
        }

        int eventMaximumRateMillis = 0;
        int eventMinimumDelta = 0;
        if (sendEvents) {
            if (getAnnotation().eventMaximumRateMilliseconds() > 0) {
                logger.trace("Moderating state variable events using maximum rate (milliseconds): {}",
                        getAnnotation().eventMaximumRateMilliseconds());
                eventMaximumRateMillis = getAnnotation().eventMaximumRateMilliseconds();
            }

            if (getAnnotation().eventMinimumDelta() > 0 && Datatype.Builtin.isNumeric(datatype.getBuiltin())) {
                // TODO: Doesn't consider floating point types!
                logger.trace("Moderating state variable events using minimum delta: {}",
                        getAnnotation().eventMinimumDelta());
                eventMinimumDelta = getAnnotation().eventMinimumDelta();
            }
        }

        StateVariableTypeDetails typeDetails = new StateVariableTypeDetails(datatype, defaultValue, allowedValues,
                allowedValueRange);

        StateVariableEventDetails eventDetails = new StateVariableEventDetails(sendEvents, eventMaximumRateMillis,
                eventMinimumDelta);

        return new StateVariable(getName(), typeDetails, eventDetails);
    }

    protected Datatype createDatatype() throws LocalServiceBindingException {

        String declaredDatatype = getAnnotation().datatype();

        if (declaredDatatype.isEmpty() && getAccessor() != null) {
            Class<?> returnType = getAccessor().getReturnType();
            logger.trace("Using accessor return type as state variable type: {}", returnType);

            if (ModelUtil.isStringConvertibleType(getStringConvertibleTypes(), returnType)) {
                // Enums and toString() convertible types are always state variables with type STRING
                logger.trace("Return type is string-convertible, using string datatype");
                return Datatype.Default.STRING.getBuiltinType().getDatatype();
            } else {
                Datatype.Default defaultDatatype = Datatype.Default.getByJavaType(returnType);
                if (defaultDatatype != null) {
                    logger.trace("Return type has default UPnP datatype: {}", defaultDatatype);
                    return defaultDatatype.getBuiltinType().getDatatype();
                }
            }
        }

        // We can also guess that if the allowed values are set then it's a string
        if ((declaredDatatype == null || declaredDatatype.isEmpty())
                && (getAnnotation().allowedValues().length > 0 || getAnnotation().allowedValuesEnum() != void.class)) {
            logger.trace("State variable has restricted allowed values, hence using 'string' datatype");
            declaredDatatype = "string";
        }

        // If we still don't have it, there is nothing more we can do
        if (declaredDatatype == null || declaredDatatype.isEmpty()) {
            throw new LocalServiceBindingException("Could not detect datatype of state variable: " + getName());
        }

        logger.trace("Trying to find built-in UPnP datatype for detected name: {}", declaredDatatype);

        // Now try to find the actual UPnP datatype by mapping the Default to Builtin
        Datatype.Builtin builtin = Datatype.Builtin.getByDescriptorName(declaredDatatype);
        if (builtin != null) {
            logger.trace("Found built-in UPnP datatype: {}", builtin);
            return builtin.getDatatype();
        } else {
            // TODO
            throw new LocalServiceBindingException(
                    "No built-in UPnP datatype found, using CustomDataType (TODO: NOT IMPLEMENTED)");
        }
    }

    protected String createDefaultValue(Datatype datatype) throws LocalServiceBindingException {

        // Next, the default value of the state variable, first the declared one
        if (!getAnnotation().defaultValue().isEmpty()) {
            // The declared default value needs to match the datatype
            try {
                datatype.valueOf(getAnnotation().defaultValue());
                logger.trace("Found state variable default value: {}", getAnnotation().defaultValue());
                return getAnnotation().defaultValue();
            } catch (Exception e) {
                throw new LocalServiceBindingException("Default value doesn't match datatype of state variable '"
                        + getName() + "': " + e.getMessage());
            }
        }

        return null;
    }

    protected String[] getAllowedValues(Class enumType) throws LocalServiceBindingException {

        if (!enumType.isEnum()) {
            throw new LocalServiceBindingException("Allowed values type is not an Enum: " + enumType);
        }

        logger.trace("Restricting allowed values of state variable to Enum: {}", getName());
        String[] allowedValueStrings = new String[enumType.getEnumConstants().length];
        for (int i = 0; i < enumType.getEnumConstants().length; i++) {
            Object o = enumType.getEnumConstants()[i];
            if (o.toString().length() > 32) {
                throw new LocalServiceBindingException(
                        "Allowed value string (that is, Enum constant name) is longer than 32 characters: " + o);
            }
            logger.trace("Adding allowed value (converted to string): {}", o);
            allowedValueStrings[i] = o.toString();
        }

        return allowedValueStrings;
    }

    protected StateVariableAllowedValueRange getAllowedValueRange(long min, long max, long step)
            throws LocalServiceBindingException {
        if (max < min) {
            throw new LocalServiceBindingException("Allowed value range maximum is smaller than minimum: " + getName());
        }

        return new StateVariableAllowedValueRange(min, max, step);
    }

    protected String[] getAllowedValuesFromProvider() throws LocalServiceBindingException {
        Class provider = getAnnotation().allowedValueProvider();
        if (!AllowedValueProvider.class.isAssignableFrom(provider)) {
            throw new LocalServiceBindingException(
                    "Allowed value provider is not of type " + AllowedValueProvider.class + ": " + getName());
        }
        try {
            return ((Class<? extends AllowedValueProvider>) provider).getDeclaredConstructor().newInstance()
                    .getValues();
        } catch (Exception e) {
            throw new LocalServiceBindingException("Allowed value provider can't be instantiated: " + getName(), e);
        }
    }

    protected StateVariableAllowedValueRange getAllowedRangeFromProvider() throws LocalServiceBindingException {
        Class provider = getAnnotation().allowedValueRangeProvider();
        if (!AllowedValueRangeProvider.class.isAssignableFrom(provider)) {
            throw new LocalServiceBindingException("Allowed value range provider is not of type "
                    + AllowedValueRangeProvider.class + ": " + getName());
        }
        try {
            AllowedValueRangeProvider providerInstance = ((Class<? extends AllowedValueRangeProvider>) provider)
                    .getDeclaredConstructor().newInstance();
            return getAllowedValueRange(providerInstance.getMinimum(), providerInstance.getMaximum(),
                    providerInstance.getStep());
        } catch (Exception e) {
            throw new LocalServiceBindingException("Allowed value range provider can't be instantiated: " + getName(),
                    e);
        }
    }
}
