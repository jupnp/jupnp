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
 * Sender and body will only be displayed if display type is set to "Maximum".
 *
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class MessageSMS extends Message {

    private final DateTime receiveTime;
    private final NumberName receiver;
    private final NumberName sender;
    private final String body;

    public MessageSMS(NumberName receiver, NumberName sender, String body) {
        this(new DateTime(), receiver, sender, body);
    }

    public MessageSMS(DateTime receiveTime, NumberName receiver, NumberName sender, String body) {
        this(Message.DisplayType.MAXIMUM, receiveTime, receiver, sender, body);
    }

    public MessageSMS(DisplayType displayType, DateTime receiveTime, NumberName receiver, NumberName sender,
            String body) {
        super(Message.Category.SMS, displayType);
        this.receiveTime = receiveTime;
        this.receiver = receiver;
        this.sender = sender;
        this.body = body;
    }

    public DateTime getReceiveTime() {
        return receiveTime;
    }

    public NumberName getReceiver() {
        return receiver;
    }

    public NumberName getSender() {
        return sender;
    }

    public String getBody() {
        return body;
    }

    @Override
    public void appendMessageElements(MessageElement parent) {
        getReceiveTime().appendMessageElements(parent.createChild("ReceiveTime"));
        getReceiver().appendMessageElements(parent.createChild("Receiver"));
        getSender().appendMessageElements(parent.createChild("Sender"));
        parent.createChild("Body").setContent(getBody());
    }
}
