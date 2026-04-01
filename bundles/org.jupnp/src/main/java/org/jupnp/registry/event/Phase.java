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
package org.jupnp.registry.event;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @author Christian Bauer
 */

public interface Phase {

    @Target({ FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface Alive {

    }

    @Target({ FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface Complete {

    }

    @Target({ FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface Byebye {

    }

    @Target({ FIELD, PARAMETER })
    @Retention(RUNTIME)
    @interface Updated {

    }
}
