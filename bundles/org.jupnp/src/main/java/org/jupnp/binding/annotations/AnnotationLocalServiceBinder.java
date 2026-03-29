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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.jupnp.binding.LocalServiceBinder;
import org.jupnp.binding.LocalServiceBindingException;
import org.jupnp.model.ValidationError;
import org.jupnp.model.ValidationException;
import org.jupnp.model.action.ActionExecutor;
import org.jupnp.model.action.QueryStateVariableExecutor;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.LocalService;
import org.jupnp.model.meta.QueryStateVariableAction;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.state.FieldStateVariableAccessor;
import org.jupnp.model.state.GetterStateVariableAccessor;
import org.jupnp.model.state.StateVariableAccessor;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.model.types.csv.CSV;
import org.jupnp.util.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads {@link org.jupnp.model.meta.LocalService} metadata from annotations.
 *
 * @author Christian Bauer
 */
public class AnnotationLocalServiceBinder implements LocalServiceBinder {

    private final Logger logger = LoggerFactory.getLogger(AnnotationLocalServiceBinder.class);

    @Override
    public LocalService read(Class<?> clazz) throws LocalServiceBindingException {
        logger.trace("Reading and binding annotations of service implementation class: {}", clazz);

        // Read the service ID and service type from the annotation
        if (clazz.isAnnotationPresent(UpnpService.class)) {

            UpnpService annotation = clazz.getAnnotation(UpnpService.class);
            UpnpServiceId idAnnotation = annotation.serviceId();
            UpnpServiceType typeAnnotation = annotation.serviceType();

            ServiceId serviceId = idAnnotation.namespace().equals(UDAServiceId.DEFAULT_NAMESPACE)
                    ? new UDAServiceId(idAnnotation.value())
                    : new ServiceId(idAnnotation.namespace(), idAnnotation.value());

            ServiceType serviceType = typeAnnotation.namespace().equals(UDAServiceType.DEFAULT_NAMESPACE)
                    ? new UDAServiceType(typeAnnotation.value(), typeAnnotation.version())
                    : new ServiceType(typeAnnotation.namespace(), typeAnnotation.value(), typeAnnotation.version());

            boolean supportsQueryStateVariables = annotation.supportsQueryStateVariables();

            Set<Class> stringConvertibleTypes = readStringConvertibleTypes(annotation.stringConvertibleTypes());

            return read(clazz, serviceId, serviceType, supportsQueryStateVariables, stringConvertibleTypes);
        } else {
            throw new LocalServiceBindingException("Given class is not an @UpnpService");
        }
    }

    @Override
    public LocalService read(Class<?> clazz, ServiceId id, ServiceType type, boolean supportsQueryStateVariables,
            Class[] stringConvertibleTypes) throws LocalServiceBindingException {
        return read(clazz, id, type, supportsQueryStateVariables, new HashSet<>(Arrays.asList(stringConvertibleTypes)));
    }

    public LocalService read(Class<?> clazz, ServiceId id, ServiceType type, boolean supportsQueryStateVariables,
            Set<Class> stringConvertibleTypes) throws LocalServiceBindingException {

        Map<StateVariable, StateVariableAccessor> stateVariables = readStateVariables(clazz, stringConvertibleTypes);
        Map<Action, ActionExecutor> actions = readActions(clazz, stateVariables, stringConvertibleTypes);

        // Special treatment of the state variable querying action
        if (supportsQueryStateVariables) {
            actions.put(new QueryStateVariableAction(), new QueryStateVariableExecutor());
        }

        try {
            return new LocalService(type, id, actions, stateVariables, stringConvertibleTypes,
                    supportsQueryStateVariables);

        } catch (ValidationException e) {
            logger.error("Could not validate device model", e);
            for (ValidationError validationError : e.getErrors()) {
                logger.error(validationError.toString());
            }
            throw new LocalServiceBindingException("Validation of model failed, check the log");
        }
    }

    protected Set<Class> readStringConvertibleTypes(Class[] declaredTypes) throws LocalServiceBindingException {

        for (Class stringConvertibleType : declaredTypes) {
            if (!Modifier.isPublic(stringConvertibleType.getModifiers())) {
                throw new LocalServiceBindingException(
                        "Declared string-convertible type must be public: " + stringConvertibleType);
            }
            try {
                stringConvertibleType.getConstructor(String.class);
            } catch (NoSuchMethodException e) {
                throw new LocalServiceBindingException(
                        "Declared string-convertible type needs a public single-argument String constructor: "
                                + stringConvertibleType);
            }
        }
        Set<Class> stringConvertibleTypes = new HashSet<>(Arrays.asList(declaredTypes));

        // Some defaults
        stringConvertibleTypes.add(URI.class);
        stringConvertibleTypes.add(URL.class);
        stringConvertibleTypes.add(CSV.class);

        return stringConvertibleTypes;
    }

    protected Map<StateVariable, StateVariableAccessor> readStateVariables(Class<?> clazz,
            Set<Class> stringConvertibleTypes) throws LocalServiceBindingException {

        Map<StateVariable, StateVariableAccessor> map = new HashMap<>();

        // State variables declared on the class
        if (clazz.isAnnotationPresent(UpnpStateVariables.class)) {
            UpnpStateVariables variables = clazz.getAnnotation(UpnpStateVariables.class);
            for (UpnpStateVariable v : variables.value()) {

                if (v.name().isEmpty()) {
                    throw new LocalServiceBindingException(
                            "Class-level @UpnpStateVariable name attribute value required");
                }

                String javaPropertyName = toJavaStateVariableName(v.name());

                Method getter = Reflections.getGetterMethod(clazz, javaPropertyName);
                Field field = Reflections.getField(clazz, javaPropertyName);

                StateVariableAccessor accessor = null;
                if (getter != null && field != null) {
                    accessor = variables.preferFields() ? new FieldStateVariableAccessor(field)
                            : new GetterStateVariableAccessor(getter);
                } else if (field != null) {
                    accessor = new FieldStateVariableAccessor(field);
                } else if (getter != null) {
                    accessor = new GetterStateVariableAccessor(getter);
                } else {
                    logger.trace("No field or getter found for state variable, skipping accessor: {}", v.name());
                }

                StateVariable stateVar = new AnnotationStateVariableBinder(v, v.name(), accessor,
                        stringConvertibleTypes).createStateVariable();

                map.put(stateVar, accessor);
            }
        }

        // State variables declared on fields
        for (Field field : Reflections.getFields(clazz, UpnpStateVariable.class)) {

            UpnpStateVariable svAnnotation = field.getAnnotation(UpnpStateVariable.class);

            StateVariableAccessor accessor = new FieldStateVariableAccessor(field);

            StateVariable stateVar = new AnnotationStateVariableBinder(svAnnotation,
                    svAnnotation.name().isEmpty() ? toUpnpStateVariableName(field.getName()) : svAnnotation.name(),
                    accessor, stringConvertibleTypes).createStateVariable();

            map.put(stateVar, accessor);
        }

        // State variables declared on getters
        for (Method getter : Reflections.getMethods(clazz, UpnpStateVariable.class)) {

            String propertyName = Reflections.getMethodPropertyName(getter.getName());
            if (propertyName == null) {
                throw new LocalServiceBindingException("Annotated method is not a getter method (: " + getter);
            }

            if (getter.getParameterTypes().length > 0) {
                throw new LocalServiceBindingException(
                        "Getter method defined as @UpnpStateVariable can not have parameters: " + getter);
            }

            UpnpStateVariable svAnnotation = getter.getAnnotation(UpnpStateVariable.class);

            StateVariableAccessor accessor = new GetterStateVariableAccessor(getter);

            StateVariable stateVar = new AnnotationStateVariableBinder(svAnnotation,
                    svAnnotation.name().isEmpty() ? toUpnpStateVariableName(propertyName) : svAnnotation.name(),
                    accessor, stringConvertibleTypes).createStateVariable();

            map.put(stateVar, accessor);
        }

        return map;
    }

    protected Map<Action, ActionExecutor> readActions(Class<?> clazz,
            Map<StateVariable, StateVariableAccessor> stateVariables, Set<Class> stringConvertibleTypes)
            throws LocalServiceBindingException {

        Map<Action, ActionExecutor> map = new HashMap<>();

        for (Method method : Reflections.getMethods(clazz, UpnpAction.class)) {
            AnnotationActionBinder actionBinder = new AnnotationActionBinder(method, stateVariables,
                    stringConvertibleTypes);
            Action action = actionBinder.appendAction(map);
            if (isActionExcluded(action)) {
                map.remove(action);
            }
        }

        return map;
    }

    /**
     * Override this method to exclude action/methods after they have been discovered.
     */
    protected boolean isActionExcluded(Action action) {
        return false;
    }

    // TODO: I don't like the exceptions much, user has no idea what to do

    static String toUpnpStateVariableName(String javaName) {
        if (javaName.isEmpty()) {
            throw new IllegalArgumentException("Variable name must be at least 1 character long");
        }
        return javaName.substring(0, 1).toUpperCase(Locale.ENGLISH) + javaName.substring(1);
    }

    static String toJavaStateVariableName(String upnpName) {
        if (upnpName.isEmpty()) {
            throw new IllegalArgumentException("Variable name must be at least 1 character long");
        }
        return upnpName.substring(0, 1).toLowerCase(Locale.ENGLISH) + upnpName.substring(1);
    }

    static String toUpnpActionName(String javaName) {
        if (javaName.isEmpty()) {
            throw new IllegalArgumentException("Action name must be at least 1 character long");
        }
        return javaName.substring(0, 1).toUpperCase(Locale.ENGLISH) + javaName.substring(1);
    }

    static String toJavaActionName(String upnpName) {
        if (upnpName.isEmpty()) {
            throw new IllegalArgumentException("Variable name must be at least 1 character long");
        }
        return upnpName.substring(0, 1).toLowerCase(Locale.ENGLISH) + upnpName.substring(1);
    }
}
