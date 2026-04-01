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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wraps a W3C element.
 * <p>
 * Using XPath for simple queries like "get all children with attribute value X" is actually slower than
 * iterating with the W3C API. We still use XPath because there is a small chance that the authors of the
 * XML implementation in the JDK will not get forced by Larry to fix their ugly code and APIs. Yes, it'
 * s unlikely but one can dream...
 * </p>
 *
 * @author Christian Bauer
 */
public abstract class DOMElement<CHILD extends DOMElement, PARENT extends DOMElement> {

    public final Builder<PARENT> PARENT_BUILDER;
    public final ArrayBuilder<CHILD> CHILD_BUILDER;

    private final XPath xpath;
    private Element element;

    protected DOMElement(XPath xpath, Element element) {
        this.xpath = xpath;
        this.element = element;
        this.PARENT_BUILDER = createParentBuilder(this);
        this.CHILD_BUILDER = createChildBuilder(this);
    }

    public Element getW3CElement() {
        return element;
    }

    public String getElementName() {
        return getW3CElement().getNodeName();
    }

    public String getContent() {
        return getW3CElement().getTextContent();
    }

    public DOMElement<CHILD, PARENT> setContent(String content) {
        getW3CElement().setTextContent(content);
        return this;
    }

    public String getAttribute(String attribute) {
        String v = getW3CElement().getAttribute(attribute);
        return !v.isEmpty() ? v : null;
    }

    public DOMElement setAttribute(String attribute, String value) {
        getW3CElement().setAttribute(attribute, value);
        return this;
    }

    public PARENT getParent() {
        return PARENT_BUILDER.build((Element) getW3CElement().getParentNode());
    }

    public CHILD[] getChildren() {
        NodeList nodes = getW3CElement().getChildNodes();
        List<CHILD> children = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                children.add(CHILD_BUILDER.build((Element) node));
            }
        }
        return children.toArray(CHILD_BUILDER.newChildrenArray(children.size()));
    }

    public CHILD[] getChildren(String name) {
        Collection<CHILD> list = getXPathChildElements(CHILD_BUILDER, prefix(name));
        return list.toArray(CHILD_BUILDER.newChildrenArray(list.size()));
    }

    public CHILD getRequiredChild(String name) throws ParserException {
        CHILD[] children = getChildren(name);
        if (children.length != 1) {
            throw new ParserException("Required single child element of '" + getElementName() + "' not found: " + name);
        }
        return children[0];
    }

    public CHILD[] findChildren(String name) {
        Collection<CHILD> list = getXPathChildElements(CHILD_BUILDER, "descendant::" + prefix(name));
        return list.toArray(CHILD_BUILDER.newChildrenArray(list.size()));
    }

    public CHILD findChildWithIdentifier(final String id) {
        Collection<CHILD> list = getXPathChildElements(CHILD_BUILDER,
                "descendant::" + prefix("*") + "[@id=\"" + id + "\"]");
        if (list.size() == 1) {
            return list.iterator().next();
        }
        return null;
    }

    public CHILD getFirstChild(String name) {
        return getXPathChildElement(CHILD_BUILDER, prefix(name) + "[1]");
    }

    public CHILD createChild(String name) {
        return createChild(name, null);
    }

    public CHILD createChild(String name, String namespaceURI) {
        CHILD child = CHILD_BUILDER.build(namespaceURI == null ? getW3CElement().getOwnerDocument().createElement(name)
                : getW3CElement().getOwnerDocument().createElementNS(namespaceURI, name));
        getW3CElement().appendChild(child.getW3CElement());
        return child;
    }

    public CHILD appendChild(CHILD el, boolean copy) {
        el = adoptOrImport(getW3CElement().getOwnerDocument(), el, copy);
        getW3CElement().appendChild(el.getW3CElement());
        return el;
    }

    public CHILD replaceChild(CHILD original, CHILD replacement, boolean copy) {
        replacement = adoptOrImport(getW3CElement().getOwnerDocument(), replacement, copy);
        getW3CElement().replaceChild(replacement.getW3CElement(), original.getW3CElement());
        return replacement;
    }

    public void replaceEqualChild(DOMElement source, String identifier) {
        DOMElement original = findChildWithIdentifier(identifier);
        DOMElement replacement = source.findChildWithIdentifier(identifier);
        original.getParent().replaceChild(original, replacement, true);
    }

    public void removeChild(CHILD el) {
        getW3CElement().removeChild(el.getW3CElement());
    }

    public void removeChildren() {
        NodeList children = getW3CElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            getW3CElement().removeChild(child);
        }
    }

    protected CHILD adoptOrImport(Document document, CHILD child, boolean copy) {
        if (document != null) {
            if (copy) {
                child = CHILD_BUILDER.build((Element) document.importNode(child.getW3CElement(), true));
            } else {
                child = CHILD_BUILDER.build((Element) document.adoptNode(child.getW3CElement()));
            }
        }
        return child;
    }

    protected abstract Builder<PARENT> createParentBuilder(DOMElement el);

    protected abstract ArrayBuilder<CHILD> createChildBuilder(DOMElement el);

    public String toSimpleXMLString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(getElementName());
        NamedNodeMap map = getW3CElement().getAttributes();
        for (int i = 0; i < map.getLength(); i++) {
            Node attr = map.item(i);
            sb.append(" ").append(attr.getNodeName()).append("=\"").append(attr.getTextContent()).append("\"");
        }
        if (!getContent().isEmpty()) {
            sb.append(">").append(getContent()).append("</").append(getElementName()).append(">");
        } else {
            sb.append("/>");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "(" + getClass().getSimpleName() + ") " + (getW3CElement() == null ? "UNBOUND" : getElementName());
    }

    public XPath getXpath() {
        return xpath;
    }

    protected String prefix(String localName) {
        return localName;
    }

    public Collection<PARENT> getXPathParentElements(Builder<CHILD> builder, String expr) {
        return getXPathElements(builder, expr);
    }

    public Collection<CHILD> getXPathChildElements(Builder<CHILD> builder, String expr) {
        return getXPathElements(builder, expr);
    }

    public PARENT getXPathParentElement(Builder<PARENT> builder, String expr) {
        Node node = (Node) getXPathResult(getW3CElement(), expr, XPathConstants.NODE);
        return node != null && node.getNodeType() == Node.ELEMENT_NODE ? builder.build((Element) node) : null;
    }

    public CHILD getXPathChildElement(Builder<CHILD> builder, String expr) {
        Node node = (Node) getXPathResult(getW3CElement(), expr, XPathConstants.NODE);
        return node != null && node.getNodeType() == Node.ELEMENT_NODE ? builder.build((Element) node) : null;
    }

    public Collection getXPathElements(Builder builder, String expr) {
        Collection col = new ArrayList();
        NodeList result = (NodeList) getXPathResult(getW3CElement(), expr, XPathConstants.NODESET);
        for (int i = 0; i < result.getLength(); i++) {
            DOMElement e = builder.build((Element) result.item(i));
            col.add(e);
        }
        return col;
    }

    public String getXPathString(XPath xpath, String expr) {
        return getXPathResult(getW3CElement(), expr, null).toString();
    }

    public Object getXPathResult(String expr, QName result) {
        return getXPathResult(getW3CElement(), expr, result);
    }

    public Object getXPathResult(Node context, String expr, QName result) {
        try {
            // System.out.println("#### XPATH: " + expr + " CONTEXT: " + context.getAttributes().getNamedItem("id") + "
            // EXPECTING: " + result);
            if (result == null) {
                return xpath.evaluate(expr, context);
            }
            return xpath.evaluate(expr, context, result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract static class Builder<T extends DOMElement> {
        public DOMElement element;

        protected Builder(DOMElement element) {
            this.element = element;
        }

        public abstract T build(Element element);

        public T firstChildOrNull(String elementName) {
            DOMElement el = element.getFirstChild(elementName);
            return el != null ? build(el.getW3CElement()) : null;
        }
    }

    public abstract static class ArrayBuilder<T extends DOMElement> extends Builder<T> {

        protected ArrayBuilder(DOMElement element) {
            super(element);
        }

        public abstract T[] newChildrenArray(int length);

        public T[] getChildElements() {
            return buildArray(element.getChildren());
        }

        public T[] getChildElements(String elementName) {
            return buildArray(element.getChildren(elementName));
        }

        protected T[] buildArray(DOMElement[] list) {
            T[] children = newChildrenArray(list.length);
            for (int i = 0; i < children.length; i++) {
                children[i] = build(list[i].getW3CElement());
            }
            return children;
        }
    }
}
