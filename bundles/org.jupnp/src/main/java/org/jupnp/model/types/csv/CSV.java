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
package org.jupnp.model.types.csv;

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.ModelUtil;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.util.Reflections;

/**
 * Transforms a state variable value from/to strings of comma-separated elements.
 * <p>
 * A concrete implementation of this interface knows how to transform values of the
 * declared type into a string of comma-separated list of elements, and how to read
 * such strings back into individual values.
 * </p>
 * <p>
 * Your action method returns a <code>CSV&lt;...&gt;</code> instance as an output argument. It can
 * also accept a concrete subclass of this type as an input argument, e.g. <code>CSVString</code>.
 * This type extends a regular <code>List</code>, so within your action method you can
 * handle the elements as usual.
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class CSV<T> extends ArrayList<T> {

    protected final Datatype.Builtin datatype;

    protected CSV() {
        datatype = getBuiltinDatatype();
    }

    protected CSV(String s) throws InvalidValueException {
        datatype = getBuiltinDatatype();
        addAll(parseString(s));
    }

    protected List parseString(String s) throws InvalidValueException {
        String[] strings = ModelUtil.fromCommaSeparatedList(s);
        List values = new ArrayList();
        for (String string : strings) {
            values.add(datatype.getDatatype().valueOf(string));
        }
        return values;
    }

    protected Datatype.Builtin getBuiltinDatatype() throws InvalidValueException {
        Class csvType = Reflections.getTypeArguments(ArrayList.class, getClass()).get(0);
        Datatype.Default defaultType = Datatype.Default.getByJavaType(csvType);
        if (defaultType == null) {
            throw new InvalidValueException("No built-in UPnP datatype for Java type of CSV: " + csvType);
        }
        return defaultType.getBuiltinType();
    }

    @Override
    public String toString() {
        List<String> stringValues = new ArrayList<>();
        for (T t : this) {
            stringValues.add(datatype.getDatatype().getString(t));
        }
        return ModelUtil.toCommaSeparatedList(stringValues.toArray(new Object[stringValues.size()]));
    }
}
