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
package org.jupnp.tool.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Test cases for PrintUtils test class.
 * 
 * @author Jochen Hiller - Initial contribution
 */
class PrintUtilsTest {

    @Test
    void testPrintTable() {
        List<String[]> table = new ArrayList<>();
        table.add(new String[] { "IP", "Model", "SerialNumber" });
        table.add(new String[] { "==", "=====", "============" });
        table.add(new String[] { "192.168.2.1", "QIVICON", "1234567890" });
        table.add(new String[] { "192.168.2.2", "FRITZ!Box 7490", "-" });
        table.add(new String[] { "192.168.2.3", "DCS-932L", "-" });
        table.add(new String[] { "192.168.2.4", "Philips hue bridge 2012", "123456789012" });
        table.add(new String[] { "192.168.2.5", "Sonos PLAY:3", "-" });
        String s = PrintUtils.printTable(table, 4);
        // System.out.println(s);

        c(s, "IP             Model                      SerialNumber    ");
        c(s, "==             =====                      ============    ");
        c(s, "192.168.2.1    QIVICON                    1234567890      ");
        c(s, "192.168.2.2    FRITZ!Box 7490             -               ");
        c(s, "192.168.2.3    DCS-932L                   -               ");
        c(s, "192.168.2.4    Philips hue bridge 2012    123456789012    ");
        c(s, "192.168.2.5    Sonos PLAY:3               -               ");
    }

    private void c(String s, String mustContain) {
        assertTrue(s.contains(mustContain));
    }
}
