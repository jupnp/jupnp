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
package org.jupnp.xml;

import org.xml.sax.SAXParseException;

/**
 * Unified exception thrown by the <tt>DOMParser</tt> and <tt>SAXParser</tt>.
 *
 * @author Christian Bauer
 */
public class ParserException extends Exception {

    public ParserException() {
    }

    public ParserException(String s) {
        super(s);
    }

    public ParserException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public ParserException(Throwable throwable) {
        super(throwable);
    }

    public ParserException(SAXParseException e) {
        super("(Line/Column: " + e.getLineNumber() + ":" + e.getColumnNumber() + ") " + e.getMessage());
    }
}
