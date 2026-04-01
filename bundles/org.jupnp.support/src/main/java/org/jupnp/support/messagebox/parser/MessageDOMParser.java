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

import org.jupnp.xml.DOMParser;
import org.jupnp.xml.NamespaceContextMap;
import org.w3c.dom.Document;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class MessageDOMParser extends DOMParser<MessageDOM> {

    @Override
    protected MessageDOM createDOM(Document document) {
        return new MessageDOM(document);
    }

    public NamespaceContextMap createDefaultNamespaceContext(String... optionalPrefixes) {
        NamespaceContextMap ctx = new NamespaceContextMap() {
            private static final long serialVersionUID = -5868179503803332877L;

            @Override
            protected String getDefaultNamespaceURI() {
                return MessageDOM.NAMESPACE_URI;
            }
        };
        for (String optionalPrefix : optionalPrefixes) {
            ctx.put(optionalPrefix, MessageDOM.NAMESPACE_URI);
        }
        return ctx;
    }

    public XPath createXPath() {
        return super.createXPath(createDefaultNamespaceContext(MessageElement.XPATH_PREFIX));
    }
}
