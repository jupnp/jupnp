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
package org.jupnp.model.meta;

import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.Validatable;
import org.jupnp.model.ValidationError;
import org.jupnp.util.SpecificationViolationReporter;

/**
 * Integrity rule for a state variable, restricting its values to a range with steps.
 * <p/>
 * TODO: The question here is: Are they crazy enough to use this for !integer (e.g. floating point) numbers?
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public class StateVariableAllowedValueRange implements Validatable {

    private final long minimum;
    private final long maximum;
    private final long step;

    public StateVariableAllowedValueRange(long minimum, long maximum) {
        this(minimum, maximum, 1);
    }

    public StateVariableAllowedValueRange(long minimum, long maximum, long step) {
        if (minimum > maximum) {
            SpecificationViolationReporter.report(
                    "Allowed value range minimum '{}' is greater than maximum '{}', switching values.", minimum,
                    maximum);
            this.minimum = maximum;
            this.maximum = minimum;
        } else {
            this.minimum = minimum;
            this.maximum = maximum;
        }
        this.step = step;
    }

    public long getMinimum() {
        return minimum;
    }

    public long getMaximum() {
        return maximum;
    }

    public long getStep() {
        return step;
    }

    public boolean isInRange(long value) {
        return value >= getMinimum() && value <= getMaximum() && (value % step) == 0;
    }

    @Override
    public List<ValidationError> validate() {
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return "Range Min: " + getMinimum() + " Max: " + getMaximum() + " Step: " + getStep();
    }
}
