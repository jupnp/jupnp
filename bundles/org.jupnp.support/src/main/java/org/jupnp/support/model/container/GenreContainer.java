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
package org.jupnp.support.model.container;

/**
 * @author Christian Bauer
 */
public class GenreContainer extends Container {

    public static final Class CLASS = new Class("object.container.genre");

    public GenreContainer() {
        setClazz(CLASS);
    }

    public GenreContainer(Container other) {
        super(other);
    }

    public GenreContainer(String id, Container parent, String title, String creator, Integer childCount) {
        this(id, parent.getId(), title, creator, childCount);
    }

    public GenreContainer(String id, String parentID, String title, String creator, Integer childCount) {
        super(id, parentID, title, creator, CLASS, childCount);
    }
}
