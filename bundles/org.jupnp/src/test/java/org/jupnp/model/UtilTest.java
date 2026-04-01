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
package org.jupnp.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

class UtilTest {

    protected final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    protected final DocumentBuilder documentBuilder;

    public UtilTest() {
        try {
            this.documentBuilderFactory.setNamespaceAware(true);
            this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void validUDAName() {
        assertFalse(ModelUtil.isValidUDAName("in-valid"));

        assertTrue(ModelUtil.isValidUDAName("a_valid"));
        assertTrue(ModelUtil.isValidUDAName("A_valid"));
        assertTrue(ModelUtil.isValidUDAName("1_valid"));
        assertTrue(ModelUtil.isValidUDAName("_valid"));

        assertTrue(ModelUtil.isValidUDAName("Some_Valid.Name"));
        assertFalse(ModelUtil.isValidUDAName("XML_invalid"));
        assertFalse(ModelUtil.isValidUDAName("xml_invalid"));
    }

    @Test
    void csvToString() {
        Object[] plainStrings = new Object[] { "foo", "bar", "baz" };
        assertEquals("foo,bar,baz", ModelUtil.toCommaSeparatedList(plainStrings));

        Object[] commaStrings = new Object[] { "foo,", "bar", "b,az" };
        assertEquals("foo\\,,bar,b\\,az", ModelUtil.toCommaSeparatedList(commaStrings));

        Object[] backslashStrings = new Object[] { "f\\oo", "b,ar", "b\\az" };
        assertEquals("f\\\\oo,b\\,ar,b\\\\az", ModelUtil.toCommaSeparatedList(backslashStrings));
    }

    @Test
    void stringToCsv() {
        String[] plainStrings = new String[] { "foo", "bar", "baz" };
        assertArrayEquals(ModelUtil.fromCommaSeparatedList("foo,bar,baz"), plainStrings);

        String[] commaStrings = new String[] { "foo,", "bar", "b,az" };
        assertArrayEquals(ModelUtil.fromCommaSeparatedList("foo\\,,bar,b\\,az"), commaStrings);

        String[] backslashStrings = new String[] { "f\\oo", "b,ar", "b\\az" };
        assertArrayEquals(ModelUtil.fromCommaSeparatedList("f\\\\oo,b\\,ar,b\\\\az"), backslashStrings);
    }

    @Test
    void printDOM1() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        dom.appendChild(fooEl);

        Element barEl = dom.createElement("bar");
        barEl.setAttribute("baz", "123");
        fooEl.appendChild(barEl);

        barEl.setTextContent("abc");

        String xml = XMLUtil.documentToString(dom);

        assertEquals(documentToString(dom), xml);
    }

    @Test
    void printDOM2() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElementNS("urn:foo-bar:baz", "foo");
        dom.appendChild(fooEl);

        Element barEl = dom.createElement("bar");
        barEl.setAttribute("baz", "123");
        fooEl.appendChild(barEl);

        barEl.setTextContent("abc");

        String xml = XMLUtil.documentToString(dom);

        assertEquals(documentToString(dom), xml);
    }

    @Test
    void printDOM3() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElementNS("urn:foo-bar:baz", "foo");
        dom.appendChild(fooEl);

        Element barEl = dom.createElementNS("urn:foo-bar:abc", "bar");
        barEl.setAttribute("baz", "123");
        fooEl.appendChild(barEl);

        barEl.setTextContent("abc");

        String xml = XMLUtil.documentToString(dom);

        assertEquals(documentToString(dom), xml);
    }

    @Test
    void printDOM4() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        dom.appendChild(fooEl);

        Element barEl = dom.createElementNS("urn:foo-bar:baz", "bar");
        barEl.setAttribute("baz", "123");
        fooEl.appendChild(barEl);

        barEl.setTextContent("abc");

        String xml = XMLUtil.documentToString(dom);

        assertEquals(documentToString(dom), xml);
    }

    @Test
    void printDOM5() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        dom.appendChild(fooEl);

        Document dom2 = documentBuilder.newDocument();
        dom2.setXmlStandalone(true);

        Element barEl = dom2.createElementNS("urn:foo-bar:baz", "bar");
        barEl.setAttribute("baz", "123");
        dom2.appendChild(barEl);

        Element bazEl = dom2.createElement("baz");
        bazEl.setTextContent("baz");
        barEl.appendChild(bazEl);

        String dom2XML = XMLUtil.documentToString(dom2);
        Document dom2Reparsed = documentBuilder.parse(new InputSource(new StringReader(dom2XML)));
        fooEl.appendChild(dom.importNode(dom2Reparsed.getDocumentElement(), true));

        String xml = XMLUtil.documentToString(dom);

        // We can't really test that, the order of attributes is different
        assertEquals(documentToString(dom).length(), xml.length());
    }

    @Test
    void printDOM6() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        dom.appendChild(fooEl);

        Element barOneEl = dom.createElementNS("urn:same:space", "same:bar");
        barOneEl.setTextContent("One");
        fooEl.appendChild(barOneEl);

        Element barTwoEl = dom.createElementNS("urn:same:space", "same:bar");
        barTwoEl.setTextContent("Two");
        fooEl.appendChild(barTwoEl);

        String xml = XMLUtil.documentToString(dom);

        assertEquals(documentToString(dom), xml);
    }

    @Test
    void printDOM7() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElement("foo");
        fooEl.setAttribute("bar", "baz");
        dom.appendChild(fooEl);

        String xml = XMLUtil.documentToString(dom);

        assertEquals(documentToString(dom), xml);
    }

    @Test
    @Disabled("This is where I give up on Android 2.1")
    void printDOM8() throws Exception {
        Document dom = documentBuilder.newDocument();
        dom.setXmlStandalone(true); // ROTFL

        Element fooEl = dom.createElementNS("urn:foo", "abc");
        dom.appendChild(fooEl);

        fooEl.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:bar", "urn:bar");

        Element barEl = dom.createElementNS("urn:bar", "bar:def");
        fooEl.appendChild(barEl);

        Element bar2El = dom.createElementNS("urn:bar", "bar:def2");
        fooEl.appendChild(bar2El);

        String xml = XMLUtil.documentToString(dom);
        System.out.println(xml);

        assertEquals(documentToString(dom), xml);
    }

    public static String documentToString(Document document) throws Exception {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        document.setXmlStandalone(true);
        transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(out));
        return out.toString();
    }

    @Test
    void parseTimeStrings() {
        assertEquals(11, ModelUtil.fromTimeString("00:00:11.123"));
        assertEquals(11, ModelUtil.fromTimeString("00:00:11"));
        assertEquals(71, ModelUtil.fromTimeString("00:01:11"));
        assertEquals(3671, ModelUtil.fromTimeString("01:01:11"));
    }

    @Test
    void parseInvalidTimeString() {
        assertThrows(IllegalArgumentException.class, () -> ModelUtil.fromTimeString("00-00:11.123"));
    }
}
