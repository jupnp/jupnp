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
package org.jupnp.support.model.dlna.types;

/**
 *
 * @author Mario Franco
 */
public class ScmsFlagType {

    private boolean copyright;
    private boolean original;

    public ScmsFlagType() {
        this.copyright = true;
        this.original = true;
    }

    public ScmsFlagType(boolean copyright, boolean original) {
        this.copyright = copyright;
        this.original = original;
    }

    /**
     * @return the copyright
     */
    public boolean isCopyright() {
        return copyright;
    }

    /**
     * @return the original
     */
    public boolean isOriginal() {
        return original;
    }
}
