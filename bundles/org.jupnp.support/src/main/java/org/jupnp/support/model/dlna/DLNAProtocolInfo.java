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
package org.jupnp.support.model.dlna;

import java.util.EnumMap;
import java.util.Map;

import org.jupnp.support.model.Protocol;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.util.MimeType;

/**
 * Encaspulates a MIME type (content format) and transport, protocol, additional information.
 * <p/>
 * Parses DLNA attributes in the additional information.
 *
 * @author Mario Franco
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class DLNAProtocolInfo extends ProtocolInfo {

    protected final Map<DLNAAttribute.Type, DLNAAttribute<?>> attributes = new EnumMap<>(DLNAAttribute.Type.class);

    public DLNAProtocolInfo(String s) {
        super(s);
        parseAdditionalInfo();
    }

    public DLNAProtocolInfo(MimeType contentFormatMimeType) {
        super(contentFormatMimeType);
    }

    public DLNAProtocolInfo(DLNAProfiles profile) {
        super(MimeType.valueOf(profile.getContentFormat()));
        this.attributes.put(DLNAAttribute.Type.DLNA_ORG_PN, new DLNAProfileAttribute(profile));
        this.additionalInfo = this.getAttributesString();
    }

    public DLNAProtocolInfo(DLNAProfiles profile, Map<DLNAAttribute.Type, DLNAAttribute<?>> attributes) {
        super(MimeType.valueOf(profile.getContentFormat()));
        this.attributes.putAll(attributes);
        this.attributes.put(DLNAAttribute.Type.DLNA_ORG_PN, new DLNAProfileAttribute(profile));
        this.additionalInfo = this.getAttributesString();
    }

    public DLNAProtocolInfo(Protocol protocol, String network, String contentFormat, String additionalInfo) {
        super(protocol, network, contentFormat, additionalInfo);
        parseAdditionalInfo();
    }

    public DLNAProtocolInfo(Protocol protocol, String network, String contentFormat,
            Map<DLNAAttribute.Type, DLNAAttribute<?>> attributes) {
        super(protocol, network, contentFormat, "");
        this.attributes.putAll(attributes);
        this.additionalInfo = this.getAttributesString();
    }

    public DLNAProtocolInfo(ProtocolInfo template) {
        this(template.getProtocol(), template.getNetwork(), template.getContentFormat(), template.getAdditionalInfo());
    }

    public boolean contains(DLNAAttribute.Type type) {
        return attributes.containsKey(type);
    }

    public DLNAAttribute<?> getAttribute(DLNAAttribute.Type type) {
        return attributes.get(type);
    }

    public Map<DLNAAttribute.Type, DLNAAttribute<?>> getAttributes() {
        return attributes;
    }

    protected String getAttributesString() {
        StringBuilder sb = new StringBuilder();
        for (DLNAAttribute.Type type : DLNAAttribute.Type.values()) {
            String value = attributes.containsKey(type) ? attributes.get(type).getString() : null;
            if (value != null && !value.isEmpty()) {
                sb.append(sb.length() == 0 ? "" : ";");
                sb.append(type.getAttributeName());
                sb.append("=");
                sb.append(value);
            }
        }
        return sb.toString();
    }

    protected void parseAdditionalInfo() {
        if (additionalInfo != null) {
            String[] atts = additionalInfo.split(";");
            for (String att : atts) {
                String[] attNameValue = att.split("=");
                if (attNameValue.length == 2) {
                    DLNAAttribute.Type type = DLNAAttribute.Type.valueOfAttributeName(attNameValue[0]);
                    if (type != null) {
                        DLNAAttribute<?> dlnaAttrinute = DLNAAttribute.newInstance(type, attNameValue[1],
                                this.getContentFormat());
                        attributes.put(type, dlnaAttrinute);
                    }
                }
            }
        }
    }
}
