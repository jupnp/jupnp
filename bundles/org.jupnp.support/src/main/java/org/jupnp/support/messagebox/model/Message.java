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

import java.util.Random;

import org.jupnp.support.messagebox.parser.MessageDOM;
import org.jupnp.support.messagebox.parser.MessageDOMParser;
import org.jupnp.support.messagebox.parser.MessageElement;
import org.jupnp.xml.ParserException;

/**
 * https://sourceforge.net/apps/mediawiki/samygo/index.php?title=MessageBoxService_request_format
 * 
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class Message implements ElementAppender {

    protected final Random randomGenerator = new Random();

    public enum Category {
        SMS("SMS"),
        INCOMING_CALL("Incoming Call"),
        SCHEDULE_REMINDER("Schedule Reminder");

        public final String text;

        Category(String text) {
            this.text = text;
        }
    }

    public enum DisplayType {

        MINIMUM("Minimum"),
        MAXIMUM("Maximum");

        public final String text;

        DisplayType(String text) {
            this.text = text;
        }
    }

    private final int id;
    private final Category category;
    private DisplayType displayType;

    protected Message(Category category, DisplayType displayType) {
        this(0, category, displayType);
    }

    protected Message(int id, Category category, DisplayType displayType) {
        if (id == 0) {
            id = randomGenerator.nextInt(Integer.MAX_VALUE);
        }
        this.id = id;
        this.category = category;
        this.displayType = displayType;
    }

    public int getId() {
        return id;
    }

    public Category getCategory() {
        return category;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Message message = (Message) o;

        if (id != message.id) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        try {
            MessageDOMParser mp = new MessageDOMParser();
            MessageDOM dom = mp.createDocument();

            MessageElement root = dom.createRoot(mp.createXPath(), "Message");
            root.createChild("Category").setContent(getCategory().text);
            root.createChild("DisplayType").setContent(getDisplayType().text);
            appendMessageElements(root);

            String s = mp.print(dom, 0, false);

            // Cut the root element, what we send to the TV is not really XML, just
            // random element soup which I'm sure the Samsung guys think is XML...
            return s.replaceAll("<Message xmlns=\"urn:samsung-com:messagebox-1-0\">", "").replaceAll("</Message>", "");

        } catch (ParserException e) {
            throw new RuntimeException(e);
        }
    }
}
