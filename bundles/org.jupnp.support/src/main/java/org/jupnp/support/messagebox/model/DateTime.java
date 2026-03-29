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
package org.jupnp.support.messagebox.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jupnp.support.messagebox.parser.MessageElement;

/**
 * @author Christian Bauer
 */
public class DateTime implements ElementAppender {

    private final String date;
    private final String time;

    public DateTime() {
        this(getCurrentDate(), getCurrentTime());
    }

    public DateTime(String date, String time) {
        this.date = date;
        this.time = time;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    @Override
    public void appendMessageElements(MessageElement parent) {
        parent.createChild("Date").setContent(getDate());
        parent.createChild("Time").setContent(getTime());
    }

    public static String getCurrentDate() {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        return fmt.format(new Date());
    }

    public static String getCurrentTime() {
        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm:ss");
        return fmt.format(new Date());
    }
}
