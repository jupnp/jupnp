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
package org.jupnp.support.messagebox.parser;

import javax.xml.xpath.XPath;

import org.jupnp.xml.DOMElement;
import org.w3c.dom.Element;

/**
 * @author Christian Bauer
 */
public class MessageElement extends DOMElement<MessageElement, MessageElement> {

    public static final String XPATH_PREFIX = "m";

    public MessageElement(XPath xpath, Element element) {
        super(xpath, element);
    }

    @Override
    protected String prefix(String localName) {
        return XPATH_PREFIX + ":" + localName;
    }

    @Override
    protected Builder<MessageElement> createParentBuilder(DOMElement el) {
        return new Builder<>(el) {
            @Override
            public MessageElement build(Element element) {
                return new MessageElement(getXpath(), element);
            }
        };
    }

    @Override
    protected ArrayBuilder<MessageElement> createChildBuilder(DOMElement el) {
        return new ArrayBuilder<>(el) {
            @Override
            public MessageElement[] newChildrenArray(int length) {
                return new MessageElement[length];
            }

            @Override
            public MessageElement build(Element element) {
                return new MessageElement(getXpath(), element);
            }
        };
    }
}
