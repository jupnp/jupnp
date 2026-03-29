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
public class MessageIncomingCall extends Message {

    private final DateTime callTime;
    private final NumberName callee;
    private final NumberName caller;

    public MessageIncomingCall(NumberName callee, NumberName caller) {
        this(new DateTime(), callee, caller);
    }

    public MessageIncomingCall(DateTime callTime, NumberName callee, NumberName caller) {
        this(DisplayType.MAXIMUM, callTime, callee, caller);
    }

    public MessageIncomingCall(DisplayType displayType, DateTime callTime, NumberName callee, NumberName caller) {
        super(Category.INCOMING_CALL, displayType);
        this.callTime = callTime;
        this.callee = callee;
        this.caller = caller;
    }

    public DateTime getCallTime() {
        return callTime;
    }

    public NumberName getCallee() {
        return callee;
    }

    public NumberName getCaller() {
        return caller;
    }

    @Override
    public void appendMessageElements(MessageElement parent) {
        getCallTime().appendMessageElements(parent.createChild("CallTime"));
        getCallee().appendMessageElements(parent.createChild("Callee"));
        getCaller().appendMessageElements(parent.createChild("Caller"));
    }
}
