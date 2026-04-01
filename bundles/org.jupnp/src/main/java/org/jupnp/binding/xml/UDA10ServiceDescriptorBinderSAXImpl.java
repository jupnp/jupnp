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

import static org.jupnp.binding.xml.Descriptor.Service.ATTRIBUTE;
import static org.jupnp.binding.xml.Descriptor.Service.ELEMENT;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jupnp.binding.staging.MutableAction;
import org.jupnp.binding.staging.MutableActionArgument;
import org.jupnp.binding.staging.MutableAllowedValueRange;
import org.jupnp.binding.staging.MutableService;
import org.jupnp.binding.staging.MutableStateVariable;
import org.jupnp.model.ValidationException;
import org.jupnp.model.meta.ActionArgument;
import org.jupnp.model.meta.Service;
import org.jupnp.model.meta.StateVariableEventDetails;
import org.jupnp.model.types.CustomDatatype;
import org.jupnp.model.types.Datatype;
import org.jupnp.util.SpecificationViolationReporter;
import org.jupnp.xml.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation based on JAXP SAX.
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporterm, make logger final
 */
public class UDA10ServiceDescriptorBinderSAXImpl extends UDA10ServiceDescriptorBinderImpl {

    private final Logger logger = LoggerFactory.getLogger(ServiceDescriptorBinder.class);

    @Override
    public <S extends Service> S describe(S undescribedService, String descriptorXml)
            throws DescriptorBindingException, ValidationException {

        if (descriptorXml == null || descriptorXml.isEmpty()) {
            throw new DescriptorBindingException("Null or empty descriptor");
        }

        try {
            logger.trace("Reading service from XML descriptor");

            SAXParser parser = new SAXParser();

            MutableService descriptor = new MutableService();

            hydrateBasic(descriptor, undescribedService);

            new RootHandler(descriptor, parser);

            parser.parse(new InputSource(
                    // TODO: UPNP VIOLATION: Virgin Media Superhub sends trailing spaces/newlines after last XML
                    // element, need to trim()
                    new StringReader(descriptorXml.trim())));

            // Build the immutable descriptor graph
            return (S) descriptor.build(undescribedService.getDevice());

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new DescriptorBindingException("Could not parse service descriptor", e);
        }
    }

    protected static class RootHandler extends ServiceDescriptorHandler<MutableService> {

        public RootHandler(MutableService instance, SAXParser parser) {
            super(instance, parser);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(ActionListHandler.EL)) {
                List<MutableAction> actions = new ArrayList<>();
                getInstance().actions = actions;
                new ActionListHandler(actions, this);
            }

            if (element.equals(StateVariableListHandler.EL)) {
                List<MutableStateVariable> stateVariables = new ArrayList<>();
                getInstance().stateVariables = stateVariables;
                new StateVariableListHandler(stateVariables, this);
            }
        }
    }

    protected static class ActionListHandler extends ServiceDescriptorHandler<List<MutableAction>> {

        public static final ELEMENT EL = ELEMENT.actionList;

        public ActionListHandler(List<MutableAction> instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(ActionHandler.EL)) {
                MutableAction action = new MutableAction();
                getInstance().add(action);
                new ActionHandler(action, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ActionHandler extends ServiceDescriptorHandler<MutableAction> {

        public static final ELEMENT EL = ELEMENT.action;

        public ActionHandler(MutableAction instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(ActionArgumentListHandler.EL)) {
                List<MutableActionArgument> arguments = new ArrayList<>();
                getInstance().arguments = arguments;
                new ActionArgumentListHandler(arguments, this);
            }
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case name:
                    getInstance().name = getCharacters();
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ActionArgumentListHandler extends ServiceDescriptorHandler<List<MutableActionArgument>> {

        public static final ELEMENT EL = ELEMENT.argumentList;

        public ActionArgumentListHandler(List<MutableActionArgument> instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(ActionArgumentHandler.EL)) {
                MutableActionArgument argument = new MutableActionArgument();
                getInstance().add(argument);
                new ActionArgumentHandler(argument, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ActionArgumentHandler extends ServiceDescriptorHandler<MutableActionArgument> {

        public static final ELEMENT EL = ELEMENT.argument;

        public ActionArgumentHandler(MutableActionArgument instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case name:
                    getInstance().name = getCharacters();
                    break;
                case direction:
                    String directionString = getCharacters();
                    try {
                        getInstance().direction = ActionArgument.Direction
                                .valueOf(directionString.toUpperCase(Locale.ENGLISH));
                    } catch (IllegalArgumentException e) {
                        // TODO: UPNP VIOLATION: Pelco SpectraIV-IP uses illegal value INOUT
                        SpecificationViolationReporter.report("Invalid action argument direction, assuming 'IN': {}",
                                directionString);
                        getInstance().direction = ActionArgument.Direction.IN;
                    }
                    break;
                case relatedStateVariable:
                    getInstance().relatedStateVariable = getCharacters();
                    break;
                case retval:
                    getInstance().retval = true;
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class StateVariableListHandler extends ServiceDescriptorHandler<List<MutableStateVariable>> {

        public static final ELEMENT EL = ELEMENT.serviceStateTable;

        public StateVariableListHandler(List<MutableStateVariable> instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(StateVariableHandler.EL)) {
                MutableStateVariable stateVariable = new MutableStateVariable();

                String sendEventsAttributeValue = attributes.getValue(ATTRIBUTE.sendEvents.toString());
                stateVariable.eventDetails = new StateVariableEventDetails(sendEventsAttributeValue != null
                        && sendEventsAttributeValue.toUpperCase(Locale.ENGLISH).equals("YES"));

                getInstance().add(stateVariable);
                new StateVariableHandler(stateVariable, this);
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class StateVariableHandler extends ServiceDescriptorHandler<MutableStateVariable> {

        public static final ELEMENT EL = ELEMENT.stateVariable;

        public StateVariableHandler(MutableStateVariable instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
            if (element.equals(AllowedValueListHandler.EL)) {
                List<String> allowedValues = new ArrayList<>();
                getInstance().allowedValues = allowedValues;
                new AllowedValueListHandler(allowedValues, this);
            }

            if (element.equals(AllowedValueRangeHandler.EL)) {
                MutableAllowedValueRange allowedValueRange = new MutableAllowedValueRange();
                getInstance().allowedValueRange = allowedValueRange;
                new AllowedValueRangeHandler(allowedValueRange, this);
            }
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case name:
                    getInstance().name = getCharacters();
                    break;
                case dataType:
                    String dtName = getCharacters();
                    Datatype.Builtin builtin = Datatype.Builtin.getByDescriptorName(dtName);
                    getInstance().dataType = builtin != null ? builtin.getDatatype() : new CustomDatatype(dtName);
                    break;
                case defaultValue:
                    getInstance().defaultValue = getCharacters();
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class AllowedValueListHandler extends ServiceDescriptorHandler<List<String>> {

        public static final ELEMENT EL = ELEMENT.allowedValueList;

        public AllowedValueListHandler(List<String> instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            switch (element) {
                case allowedValue:
                    getInstance().add(getCharacters());
                    break;
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class AllowedValueRangeHandler extends ServiceDescriptorHandler<MutableAllowedValueRange> {

        public static final ELEMENT EL = ELEMENT.allowedValueRange;

        public AllowedValueRangeHandler(MutableAllowedValueRange instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        @Override
        public void endElement(ELEMENT element) throws SAXException {
            try {
                switch (element) {
                    case minimum:
                        getInstance().minimum = Long.valueOf(getCharacters());
                        break;
                    case maximum:
                        getInstance().maximum = Long.valueOf(getCharacters());
                        break;
                    case step:
                        getInstance().step = Long.valueOf(getCharacters());
                        break;
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        @Override
        public boolean isLastElement(ELEMENT element) {
            return element.equals(EL);
        }
    }

    protected static class ServiceDescriptorHandler<I> extends SAXParser.Handler<I> {

        public ServiceDescriptorHandler(I instance) {
            super(instance);
        }

        public ServiceDescriptorHandler(I instance, SAXParser parser) {
            super(instance, parser);
        }

        public ServiceDescriptorHandler(I instance, ServiceDescriptorHandler parent) {
            super(instance, parent);
        }

        public ServiceDescriptorHandler(I instance, SAXParser parser, ServiceDescriptorHandler parent) {
            super(instance, parser, parent);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null) {
                return;
            }
            startElement(el, attributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            if (el == null) {
                return;
            }
            endElement(el);
        }

        @Override
        protected boolean isLastElement(String uri, String localName, String qName) {
            ELEMENT el = ELEMENT.valueOrNullOf(localName);
            return el != null && isLastElement(el);
        }

        public void startElement(ELEMENT element, Attributes attributes) throws SAXException {
        }

        public void endElement(ELEMENT element) throws SAXException {
        }

        public boolean isLastElement(ELEMENT element) {
            return false;
        }
    }
}
