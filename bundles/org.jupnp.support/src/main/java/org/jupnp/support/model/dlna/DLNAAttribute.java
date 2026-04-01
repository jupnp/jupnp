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
package org.jupnp.support.model.dlna;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transforms known and standardized DLNA attributes from/to string representation.
 * <p>
 * The {@link #newInstance(org.jupnp.support.model.dlna.DLNAAttribute.Type, String, String)}
 * method attempts to instantiate the best header subtype for a given header (name) and string value.
 * </p>
 *
 * @author Christian Bauer
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class DLNAAttribute<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DLNAAttribute.class);

    /**
     * Maps a standardized DLNA attribute to potential attribute subtypes.
     */
    public enum Type {

        /**
         * Order is important for DLNAProtocolInfo
         */
        DLNA_ORG_PN("DLNA.ORG_PN", DLNAProfileAttribute.class),
        DLNA_ORG_OP("DLNA.ORG_OP", DLNAOperationsAttribute.class),
        DLNA_ORG_PS("DLNA.ORG_PS", DLNAPlaySpeedAttribute.class),
        DLNA_ORG_CI("DLNA.ORG_CI", DLNAConversionIndicatorAttribute.class),
        DLNA_ORG_FLAGS("DLNA.ORG_FLAGS", DLNAFlagsAttribute.class);

        private static final Map<String, Type> byName = new HashMap<>() {
            private static final long serialVersionUID = -4611773458029624524L;

            {
                for (Type t : Type.values()) {
                    put(t.getAttributeName().toUpperCase(Locale.ROOT), t);
                }
            }
        };

        private final String attributeName;
        private final Class<? extends DLNAAttribute<?>>[] attributeTypes;

        @SafeVarargs
        Type(String attributeName, Class<? extends DLNAAttribute<?>>... attributeClass) {
            this.attributeName = attributeName;
            this.attributeTypes = attributeClass;
        }

        public String getAttributeName() {
            return attributeName;
        }

        public Class<? extends DLNAAttribute<?>>[] getAttributeTypes() {
            return attributeTypes;
        }

        public static Type valueOfAttributeName(String attributeName) {
            if (attributeName == null) {
                return null;
            }
            return byName.get(attributeName.toUpperCase(Locale.ROOT));
        }
    }

    private T value;

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    /**
     * @param s This attribute's value as a string representation.
     * @param cf This attribute's mime type as a string representation, optional.
     * @throws InvalidDLNAProtocolAttributeException
     *             If the value is invalid for this DLNA attribute.
     */
    public abstract void setString(String s, String cf);

    /**
     * @return A string representing this attribute's value.
     */
    public abstract String getString();

    /**
     * Create a new instance of a {@link DLNAAttribute} subtype that matches the given type and value.
     * <p>
     * This method iterates through all potential attribute subtype classes as declared in {@link Type}.
     * It creates a new instance of the subtype class and calls its {@link #setString(String, String)} method.
     * If no {@link org.jupnp.support.model.dlna.InvalidDLNAProtocolAttributeException} is thrown,
     * the subtype instance is returned.
     * </p>
     *
     * @param type The type of the attribute.
     * @param attributeValue The value of the attribute.
     * @param contentFormat The DLNA mime type of the attribute, optional.
     * @return The best matching attribute subtype instance, or <code>null</code> if no subtype can be found.
     */
    public static DLNAAttribute<?> newInstance(DLNAAttribute.Type type, String attributeValue, String contentFormat) {

        DLNAAttribute<?> attr = null;
        for (int i = 0; i < type.getAttributeTypes().length && attr == null; i++) {
            Class<? extends DLNAAttribute<?>> attributeClass = type.getAttributeTypes()[i];
            try {
                LOGGER.trace("Trying to parse DLNA '{}' with class: {}", type, attributeClass.getSimpleName());
                attr = attributeClass.getDeclaredConstructor().newInstance();
                if (attributeValue != null) {
                    attr.setString(attributeValue, contentFormat);
                }
            } catch (InvalidDLNAProtocolAttributeException e) {
                LOGGER.trace("Invalid DLNA attribute value for tested type: {} - {}", attributeClass.getSimpleName(),
                        e.getMessage());
                attr = null;
            } catch (Exception e) {
                LOGGER.error("Error instantiating DLNA attribute of type '{}' with value: {}", type, attributeValue, e);
            }
        }
        return attr;
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") '" + getValue() + "'";
    }
}
