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

import org.jupnp.support.messagebox.parser.MessageElement;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class MessageScheduleReminder extends Message {

    private final DateTime startTime;
    private final NumberName owner;
    private final String subject;
    private final DateTime endTime;
    private final String location;
    private final String body;

    public MessageScheduleReminder(DateTime startTime, NumberName owner, String subject, DateTime endTime,
            String location, String body) {
        this(DisplayType.MAXIMUM, startTime, owner, subject, endTime, location, body);
    }

    public MessageScheduleReminder(DisplayType displayType, DateTime startTime, NumberName owner, String subject,
            DateTime endTime, String location, String body) {
        super(Category.SCHEDULE_REMINDER, displayType);
        this.startTime = startTime;
        this.owner = owner;
        this.subject = subject;
        this.endTime = endTime;
        this.location = location;
        this.body = body;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public NumberName getOwner() {
        return owner;
    }

    public String getSubject() {
        return subject;
    }

    public DateTime getEndTime() {
        return endTime;
    }

    public String getLocation() {
        return location;
    }

    public String getBody() {
        return body;
    }

    @Override
    public void appendMessageElements(MessageElement parent) {
        getStartTime().appendMessageElements(parent.createChild("StartTime"));
        getOwner().appendMessageElements(parent.createChild("Owner"));
        parent.createChild("Subject").setContent(getSubject());
        getEndTime().appendMessageElements(parent.createChild("EndTime"));
        parent.createChild("Location").setContent(getLocation());
        parent.createChild("Body").setContent(getBody());
    }
}
