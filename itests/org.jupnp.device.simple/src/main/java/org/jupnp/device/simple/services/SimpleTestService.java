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
package org.jupnp.device.simple.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jupnp.common.data.TestData;
import org.jupnp.common.data.TestDataFactory;
import org.jupnp.device.simple.actions.GetAction;
import org.jupnp.device.simple.actions.SetAction;
import org.jupnp.device.simple.model.Simple;
import org.jupnp.device.simple.model.TestVariable;
import org.jupnp.device.simple.variables.TestStateVariable;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPLocalStateVariable;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

public class SimpleTestService implements UPnPService {
    private static final String SERVICE_ID = "urn:4thline-com:serviceId:SimpleTest";
    private static final String SERVICE_TYPE = "urn:schemas-4thline-com:service:SimpleTest:1";
    private static final int FIELD_UPNP_TYPE = 0;
    private static final int FIELD_JAVA_TYPE = 1;
    private static final int FIELD_DEFAULT_VALUE = 2;
    private static final int FIELD_VALUE = 3;
    private static final String TEST_DATA_ID = "initial";

    private static final long time = System.currentTimeMillis();

    private static Object[][] records = {
            // Integer ui1, ui2, i1, i2, i4, int
            { UPnPLocalStateVariable.TYPE_UI1, Integer.class, 0, 1 },
            { UPnPLocalStateVariable.TYPE_UI2, Integer.class, 0, 2 },
            { UPnPLocalStateVariable.TYPE_I1, Integer.class, 0, 3 },
            { UPnPLocalStateVariable.TYPE_I2, Integer.class, 0, 4 },
            { UPnPLocalStateVariable.TYPE_I4, Integer.class, 0, 5 },
            { UPnPLocalStateVariable.TYPE_INT, Integer.class, 0, 6 },
            // Long ui4, time, time.tz
            // time Time in a subset of ISO 8601 format with no date and no time zone.
            // time.tz Time in a subset of ISO 8601 format with optional time zone but no date.
            { UPnPLocalStateVariable.TYPE_UI4, Long.class, 0L, 7L },
            { UPnPLocalStateVariable.TYPE_TIME, Long.class, 0L, time },
            { UPnPLocalStateVariable.TYPE_TIME_TZ, Long.class, 0L, time },
            // Float r4, float
            { UPnPLocalStateVariable.TYPE_R4, Float.class, (float) 0, 10.09F },
            { UPnPLocalStateVariable.TYPE_FLOAT, Float.class, (float) 0, 11.2F },
            // Double r8, number, fixed.14.4
            { UPnPLocalStateVariable.TYPE_R8, Double.class, (double) 0, 12.3 },
            { UPnPLocalStateVariable.TYPE_NUMBER, Double.class, (double) 0, 13.3 },
            { UPnPLocalStateVariable.TYPE_FIXED_14_4, Double.class, (double) 0, 14.4 },
            // Character char
            { UPnPLocalStateVariable.TYPE_CHAR, Character.class, 'A', 'A' },
            // String string, uri, uuid
            { UPnPLocalStateVariable.TYPE_STRING, String.class, "", "string" },
            { UPnPLocalStateVariable.TYPE_URI, String.class, "", "uri" },
            { UPnPLocalStateVariable.TYPE_UUID, String.class, "", "uuid" },
            // Date date, dateTime, dateTime.tz
            // date Date in a subset of ISO 8601 format without time data.
            // dateTime Date in ISO 8601 format with optional time but no time zone.
            // dateTime.tz Date in ISO 8601 format with optional time and optional time zone.
            { UPnPLocalStateVariable.TYPE_DATE, Date.class, new Date(), new Date(time) },
            { UPnPLocalStateVariable.TYPE_DATETIME, Date.class, new Date(), new Date(time) },
            { UPnPLocalStateVariable.TYPE_DATETIME_TZ, Date.class, new Date(), new Date(time) },
            // Boolean
            { UPnPLocalStateVariable.TYPE_BOOLEAN, Boolean.class, Boolean.FALSE, Boolean.TRUE },
            // byte[] bin.base64, bin.hex
            { UPnPLocalStateVariable.TYPE_BIN_BASE64, byte[].class, new byte[] {},
                    new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03 } },
            { UPnPLocalStateVariable.TYPE_BIN_HEX, byte[].class, new byte[] {},
                    new byte[] { (byte) 0x10, (byte) 0x01, (byte) 0xF0, (byte) 0x0F } }, };

    private static TestData data = TestDataFactory.getInstance().getTestData(TEST_DATA_ID);

    private TestStateVariable[] variables;
    private Map<String, UPnPStateVariable> variablesIndex = new HashMap<>();
    private UPnPAction[] actions;
    private Map<String, UPnPAction> actionsIndex = new HashMap<>();

    public SimpleTestService(UPnPDevice device, Simple simple) {
        List<UPnPStateVariable> variableList = new ArrayList<>();

        for (Object[] record : records) {
            Object value;

            if (data == null) {
                value = record[FIELD_VALUE];
            } else {
                String name = (String) record[FIELD_UPNP_TYPE];
                String type = (String) record[FIELD_UPNP_TYPE];
                value = data.getOSGiUPnPValue(name, type, record[FIELD_VALUE]);
            }

            TestStateVariable variable = new TestStateVariable(device, this, (Class<?>) record[FIELD_JAVA_TYPE],
                    (String) record[FIELD_UPNP_TYPE], record[FIELD_DEFAULT_VALUE], true, new TestVariable(value));
            variableList.add(variable);
        }

        List<UPnPAction> actionList = new ArrayList<>();
        variables = variableList.toArray(new TestStateVariable[variableList.size()]);
        for (TestStateVariable variable : variables) {
            variablesIndex.put(variable.getName(), variable);

            actionList.add(new GetAction(variable));
            actionList.add(new SetAction(variable));
        }

        actionList.add(new GetAction("GetAllVariables", variables));
        actionList.add(new SetAction("SetAllVariables", variables));

        actions = actionList.toArray(new UPnPAction[actionList.size()]);
        for (UPnPAction action : actions) {
            actionsIndex.put(action.getName(), action);
        }
    }

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public String getType() {
        return SERVICE_TYPE;
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public UPnPAction getAction(String name) {
        return actionsIndex.get(name);
    }

    @Override
    public UPnPAction[] getActions() {
        return actions;
    }

    @Override
    public UPnPStateVariable[] getStateVariables() {
        return variables;
    }

    @Override
    public UPnPStateVariable getStateVariable(String name) {
        return variablesIndex.get(name);
    }
}
