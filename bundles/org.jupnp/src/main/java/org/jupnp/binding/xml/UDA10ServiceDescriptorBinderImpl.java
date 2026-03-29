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
package org.jupnp.binding.xml;

import static org.jupnp.model.XMLUtil.appendNewElement;
import static org.jupnp.model.XMLUtil.appendNewElementIfNotNull;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jupnp.binding.staging.MutableAction;
import org.jupnp.binding.staging.MutableActionArgument;
import org.jupnp.binding.staging.MutableAllowedValueRange;
import org.jupnp.binding.staging.MutableService;
import org.jupnp.binding.staging.MutableStateVariable;
import org.jupnp.binding.xml.Descriptor.Service.ATTRIBUTE;
import org.jupnp.binding.xml.Descriptor.Service.ELEMENT;
import org.jupnp.model.ValidationException;
import org.jupnp.model.XMLUtil;
import org.jupnp.model.meta.Action;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.QueryStateVariableAction;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.Service;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableEventDetails;
import org.jupnp.model.types.CustomDatatype;
import org.jupnp.model.types.Datatype;
import org.jupnp.util.SpecificationViolationReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Implementation based on JAXP DOM.
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter, make logger final
 */
public class UDA10ServiceDescriptorBinderImpl implements ServiceDescriptorBinder, ErrorHandler {

    private final Logger logger = LoggerFactory.getLogger(ServiceDescriptorBinder.class);

    @Override
    public <S extends Service> S describe(S undescribedService, String descriptorXml)
            throws DescriptorBindingException, ValidationException {
        if (descriptorXml == null || descriptorXml.isEmpty()) {
            throw new DescriptorBindingException("Null or empty descriptor");
        }

        try {
            logger.trace("Populating service from XML descriptor: {}", undescribedService);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            documentBuilder.setErrorHandler(this);

            Document d = documentBuilder.parse(new InputSource(
                    // TODO: UPNP VIOLATION: Virgin Media Superhub sends trailing spaces/newlines after last XML
                    // element, need to trim()
                    new StringReader(descriptorXml.trim())));

            return describe(undescribedService, d);

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new DescriptorBindingException("Could not parse service descriptor", e);
        }
    }

    @Override
    public <S extends Service> S describe(S undescribedService, Document dom)
            throws DescriptorBindingException, ValidationException {
        try {
            logger.trace("Populating service from DOM: {}", undescribedService);

            // Read the XML into a mutable descriptor graph
            MutableService descriptor = new MutableService();

            hydrateBasic(descriptor, undescribedService);

            Element rootElement = dom.getDocumentElement();
            hydrateRoot(descriptor, rootElement);

            // Build the immutable descriptor graph
            return buildInstance(undescribedService, descriptor);

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new DescriptorBindingException("Could not parse service DOM", e);
        }
    }

    protected <S extends Service> S buildInstance(S undescribedService, MutableService descriptor)
            throws ValidationException {
        return (S) descriptor.build(undescribedService.getDevice());
    }

    protected void hydrateBasic(MutableService descriptor, Service undescribedService) {
        descriptor.serviceId = undescribedService.getServiceId();
        descriptor.serviceType = undescribedService.getServiceType();
        if (undescribedService instanceof RemoteService) {
            RemoteService rs = (RemoteService) undescribedService;
            descriptor.controlURI = rs.getControlURI();
            descriptor.eventSubscriptionURI = rs.getEventSubscriptionURI();
            descriptor.descriptorURI = rs.getDescriptorURI();
        }
    }

    protected void hydrateRoot(MutableService descriptor, Element rootElement) throws DescriptorBindingException {

        // We don't check the XMLNS, nobody bothers anyway...

        if (!ELEMENT.scpd.equals(rootElement)) {
            throw new DescriptorBindingException("Root element name is not <scpd>: " + rootElement.getNodeName());
        }

        NodeList rootChildren = rootElement.getChildNodes();

        for (int i = 0; i < rootChildren.getLength(); i++) {
            Node rootChild = rootChildren.item(i);

            if (rootChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (ELEMENT.specVersion.equals(rootChild)) {
                logger.trace("Ignoring UDA major/minor specVersion");
            } else if (ELEMENT.actionList.equals(rootChild)) {
                hydrateActionList(descriptor, rootChild);
            } else if (ELEMENT.serviceStateTable.equals(rootChild)) {
                hydrateServiceStateTableList(descriptor, rootChild);
            } else {
                logger.trace("Ignoring unknown element: {}", rootChild.getNodeName());
            }
        }
    }

    public void hydrateActionList(MutableService descriptor, Node actionListNode) throws DescriptorBindingException {

        NodeList actionListChildren = actionListNode.getChildNodes();
        for (int i = 0; i < actionListChildren.getLength(); i++) {
            Node actionListChild = actionListChildren.item(i);

            if (actionListChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (ELEMENT.action.equals(actionListChild)) {
                MutableAction action = new MutableAction();
                hydrateAction(action, actionListChild);
                descriptor.actions.add(action);
            }
        }
    }

    public void hydrateAction(MutableAction action, Node actionNode) {

        NodeList actionNodeChildren = actionNode.getChildNodes();
        for (int i = 0; i < actionNodeChildren.getLength(); i++) {
            Node actionNodeChild = actionNodeChildren.item(i);

            if (actionNodeChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (ELEMENT.name.equals(actionNodeChild)) {
                action.name = XMLUtil.getTextContent(actionNodeChild);
            } else if (ELEMENT.argumentList.equals(actionNodeChild)) {

                NodeList argumentChildren = actionNodeChild.getChildNodes();
                for (int j = 0; j < argumentChildren.getLength(); j++) {
                    Node argumentChild = argumentChildren.item(j);

                    if (argumentChild.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    MutableActionArgument actionArgument = new MutableActionArgument();
                    hydrateActionArgument(actionArgument, argumentChild);
                    action.arguments.add(actionArgument);
                }
            }
        }
    }

    public void hydrateActionArgument(MutableActionArgument actionArgument, Node actionArgumentNode) {

        NodeList argumentNodeChildren = actionArgumentNode.getChildNodes();
        for (int i = 0; i < argumentNodeChildren.getLength(); i++) {
            Node argumentNodeChild = argumentNodeChildren.item(i);

            if (argumentNodeChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (ELEMENT.name.equals(argumentNodeChild)) {
                actionArgument.name = XMLUtil.getTextContent(argumentNodeChild);
            } else if (ELEMENT.direction.equals(argumentNodeChild)) {
                String directionString = XMLUtil.getTextContent(argumentNodeChild);
                try {
                    actionArgument.direction = ActionArgument.Direction
                            .valueOf(directionString.toUpperCase(Locale.ENGLISH));
                } catch (IllegalArgumentException e) {
                    // TODO: UPNP VIOLATION: Pelco SpectraIV-IP uses illegal value INOUT
                    SpecificationViolationReporter.report("Invalid action argument direction, assuming 'IN': {}",
                            directionString);
                    actionArgument.direction = ActionArgument.Direction.IN;
                }
            } else if (ELEMENT.relatedStateVariable.equals(argumentNodeChild)) {
                actionArgument.relatedStateVariable = XMLUtil.getTextContent(argumentNodeChild);
            } else if (ELEMENT.retval.equals(argumentNodeChild)) {
                actionArgument.retval = true;
            }
        }
    }

    public void hydrateServiceStateTableList(MutableService descriptor, Node serviceStateTableNode) {

        NodeList serviceStateTableChildren = serviceStateTableNode.getChildNodes();
        for (int i = 0; i < serviceStateTableChildren.getLength(); i++) {
            Node serviceStateTableChild = serviceStateTableChildren.item(i);

            if (serviceStateTableChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (ELEMENT.stateVariable.equals(serviceStateTableChild)) {
                MutableStateVariable stateVariable = new MutableStateVariable();
                hydrateStateVariable(stateVariable, (Element) serviceStateTableChild);
                descriptor.stateVariables.add(stateVariable);
            }
        }
    }

    public void hydrateStateVariable(MutableStateVariable stateVariable, Element stateVariableElement) {

        stateVariable.eventDetails = new StateVariableEventDetails(
                stateVariableElement.getAttribute("sendEvents") != null && stateVariableElement
                        .getAttribute(ATTRIBUTE.sendEvents.toString()).toUpperCase(Locale.ENGLISH).equals("YES"));

        NodeList stateVariableChildren = stateVariableElement.getChildNodes();
        for (int i = 0; i < stateVariableChildren.getLength(); i++) {
            Node stateVariableChild = stateVariableChildren.item(i);

            if (stateVariableChild.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (ELEMENT.name.equals(stateVariableChild)) {
                stateVariable.name = XMLUtil.getTextContent(stateVariableChild);
            } else if (ELEMENT.dataType.equals(stateVariableChild)) {
                String dtName = XMLUtil.getTextContent(stateVariableChild);
                Datatype.Builtin builtin = Datatype.Builtin.getByDescriptorName(dtName);
                stateVariable.dataType = builtin != null ? builtin.getDatatype() : new CustomDatatype(dtName);
            } else if (ELEMENT.defaultValue.equals(stateVariableChild)) {
                stateVariable.defaultValue = XMLUtil.getTextContent(stateVariableChild);
            } else if (ELEMENT.allowedValueList.equals(stateVariableChild)) {

                List<String> allowedValues = new ArrayList<>();

                NodeList allowedValueListChildren = stateVariableChild.getChildNodes();
                for (int j = 0; j < allowedValueListChildren.getLength(); j++) {
                    Node allowedValueListChild = allowedValueListChildren.item(j);

                    if (allowedValueListChild.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    if (ELEMENT.allowedValue.equals(allowedValueListChild)) {
                        allowedValues.add(XMLUtil.getTextContent(allowedValueListChild));
                    }
                }

                stateVariable.allowedValues = allowedValues;

            } else if (ELEMENT.allowedValueRange.equals(stateVariableChild)) {

                MutableAllowedValueRange range = new MutableAllowedValueRange();

                NodeList allowedValueRangeChildren = stateVariableChild.getChildNodes();
                for (int j = 0; j < allowedValueRangeChildren.getLength(); j++) {
                    Node allowedValueRangeChild = allowedValueRangeChildren.item(j);

                    if (allowedValueRangeChild.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    if (ELEMENT.minimum.equals(allowedValueRangeChild)) {
                        try {
                            range.minimum = Long.valueOf(XMLUtil.getTextContent(allowedValueRangeChild));
                        } catch (Exception e) {
                        }
                    } else if (ELEMENT.maximum.equals(allowedValueRangeChild)) {
                        try {
                            range.maximum = Long.valueOf(XMLUtil.getTextContent(allowedValueRangeChild));
                        } catch (Exception e) {
                        }
                    } else if (ELEMENT.step.equals(allowedValueRangeChild)) {
                        try {
                            range.step = Long.valueOf(XMLUtil.getTextContent(allowedValueRangeChild));
                        } catch (Exception e) {
                        }
                    }
                }

                stateVariable.allowedValueRange = range;
            }
        }
    }

    @Override
    public String generate(Service service) throws DescriptorBindingException {
        try {
            logger.trace("Generating XML descriptor from service model: {}", service);

            return XMLUtil.documentToString(buildDOM(service));

        } catch (Exception e) {
            throw new DescriptorBindingException("Could not build DOM: " + e.getMessage(), e);
        }
    }

    @Override
    public Document buildDOM(Service service) throws DescriptorBindingException {

        try {
            logger.trace("Generting XML descriptor from service model: {}", service);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);

            Document d = factory.newDocumentBuilder().newDocument();
            generateScpd(service, d);

            return d;

        } catch (Exception e) {
            throw new DescriptorBindingException("Could not generate service descriptor: " + e.getMessage(), e);
        }
    }

    private void generateScpd(Service serviceModel, Document descriptor) {

        Element scpdElement = descriptor.createElementNS(Descriptor.Service.NAMESPACE_URI, ELEMENT.scpd.toString());
        descriptor.appendChild(scpdElement);

        generateSpecVersion(serviceModel, descriptor, scpdElement);
        if (serviceModel.hasActions()) {
            generateActionList(serviceModel, descriptor, scpdElement);
        }
        generateServiceStateTable(serviceModel, descriptor, scpdElement);
    }

    private void generateSpecVersion(Service serviceModel, Document descriptor, Element rootElement) {
        Element specVersionElement = appendNewElement(descriptor, rootElement, ELEMENT.specVersion);
        appendNewElementIfNotNull(descriptor, specVersionElement, ELEMENT.major,
                serviceModel.getDevice().getVersion().getMajor());
        appendNewElementIfNotNull(descriptor, specVersionElement, ELEMENT.minor,
                serviceModel.getDevice().getVersion().getMinor());
    }

    private void generateActionList(Service serviceModel, Document descriptor, Element scpdElement) {

        Element actionListElement = appendNewElement(descriptor, scpdElement, ELEMENT.actionList);

        for (Action action : serviceModel.getActions()) {
            if (!action.getName().equals(QueryStateVariableAction.ACTION_NAME)) {
                generateAction(action, descriptor, actionListElement);
            }
        }
    }

    private void generateAction(Action action, Document descriptor, Element actionListElement) {

        Element actionElement = appendNewElement(descriptor, actionListElement, ELEMENT.action);

        appendNewElementIfNotNull(descriptor, actionElement, ELEMENT.name, action.getName());

        if (action.hasArguments()) {
            Element argumentListElement = appendNewElement(descriptor, actionElement, ELEMENT.argumentList);
            for (ActionArgument actionArgument : action.getArguments()) {
                generateActionArgument(actionArgument, descriptor, argumentListElement);
            }
        }
    }

    private void generateActionArgument(ActionArgument actionArgument, Document descriptor, Element actionElement) {

        Element actionArgumentElement = appendNewElement(descriptor, actionElement, ELEMENT.argument);

        appendNewElementIfNotNull(descriptor, actionArgumentElement, ELEMENT.name, actionArgument.getName());
        appendNewElementIfNotNull(descriptor, actionArgumentElement, ELEMENT.direction,
                actionArgument.getDirection().toString().toLowerCase(Locale.ENGLISH));
        if (actionArgument.isReturnValue()) {
            // TODO: UPNP VIOLATION: WMP12 will discard RenderingControl service if it contains <retval> tags
            SpecificationViolationReporter.report("Not producing <retval> element to be compatible with WMP12: {}",
                    actionArgument);
            // appendNewElement(descriptor, actionArgumentElement, ELEMENT.retval);
        }
        appendNewElementIfNotNull(descriptor, actionArgumentElement, ELEMENT.relatedStateVariable,
                actionArgument.getRelatedStateVariableName());
    }

    private void generateServiceStateTable(Service serviceModel, Document descriptor, Element scpdElement) {

        Element serviceStateTableElement = appendNewElement(descriptor, scpdElement, ELEMENT.serviceStateTable);

        for (StateVariable stateVariable : serviceModel.getStateVariables()) {
            generateStateVariable(stateVariable, descriptor, serviceStateTableElement);
        }
    }

    private void generateStateVariable(StateVariable stateVariable, Document descriptor,
            Element serviveStateTableElement) {

        Element stateVariableElement = appendNewElement(descriptor, serviveStateTableElement, ELEMENT.stateVariable);

        appendNewElementIfNotNull(descriptor, stateVariableElement, ELEMENT.name, stateVariable.getName());

        if (stateVariable.getTypeDetails().getDatatype() instanceof CustomDatatype) {
            appendNewElementIfNotNull(descriptor, stateVariableElement, ELEMENT.dataType,
                    ((CustomDatatype) stateVariable.getTypeDetails().getDatatype()).getName());
        } else {
            appendNewElementIfNotNull(descriptor, stateVariableElement, ELEMENT.dataType,
                    stateVariable.getTypeDetails().getDatatype().getBuiltin().getDescriptorName());
        }

        appendNewElementIfNotNull(descriptor, stateVariableElement, ELEMENT.defaultValue,
                stateVariable.getTypeDetails().getDefaultValue());

        // The default is 'yes' but we generate it anyway just to be sure
        if (stateVariable.getEventDetails().isSendEvents()) {
            stateVariableElement.setAttribute(ATTRIBUTE.sendEvents.toString(), "yes");
        } else {
            stateVariableElement.setAttribute(ATTRIBUTE.sendEvents.toString(), "no");
        }

        if (stateVariable.getTypeDetails().getAllowedValues() != null) {
            Element allowedValueListElement = appendNewElement(descriptor, stateVariableElement,
                    ELEMENT.allowedValueList);
            for (String allowedValue : stateVariable.getTypeDetails().getAllowedValues()) {
                appendNewElementIfNotNull(descriptor, allowedValueListElement, ELEMENT.allowedValue, allowedValue);
            }
        }

        if (stateVariable.getTypeDetails().getAllowedValueRange() != null) {
            Element allowedValueRangeElement = appendNewElement(descriptor, stateVariableElement,
                    ELEMENT.allowedValueRange);
            appendNewElementIfNotNull(descriptor, allowedValueRangeElement, ELEMENT.minimum,
                    stateVariable.getTypeDetails().getAllowedValueRange().getMinimum());
            appendNewElementIfNotNull(descriptor, allowedValueRangeElement, ELEMENT.maximum,
                    stateVariable.getTypeDetails().getAllowedValueRange().getMaximum());
            if (stateVariable.getTypeDetails().getAllowedValueRange().getStep() >= 1L) {
                appendNewElementIfNotNull(descriptor, allowedValueRangeElement, ELEMENT.step,
                        stateVariable.getTypeDetails().getAllowedValueRange().getStep());
            }
        }
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        logger.warn(e.toString());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }
}
