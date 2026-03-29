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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.jupnp.model.types.Base64Datatype;
import org.jupnp.model.types.DLNADoc;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.DateTimeDatatype;
import org.jupnp.model.types.DoubleDatatype;
import org.jupnp.model.types.FloatDatatype;
import org.jupnp.model.types.IntegerDatatype;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.model.types.UnsignedIntegerFourBytesDatatype;
import org.jupnp.model.types.UnsignedIntegerOneByteDatatype;
import org.jupnp.model.types.UnsignedIntegerTwoBytesDatatype;
import org.jupnp.model.types.csv.CSVBoolean;
import org.jupnp.model.types.csv.CSVString;

class DatatypesTest {

    @Test
    void upperLowerCase() {
        // Broken devices do this
        assertEquals(Datatype.Builtin.STRING, Datatype.Builtin.getByDescriptorName("String"));
        assertEquals(Datatype.Builtin.STRING, Datatype.Builtin.getByDescriptorName("strinG"));
        assertEquals(Datatype.Builtin.STRING, Datatype.Builtin.getByDescriptorName("STRING"));
        assertEquals(Datatype.Builtin.STRING, Datatype.Builtin.getByDescriptorName("string"));
    }

    @Test
    void validUnsignedIntegers() {

        UnsignedIntegerOneByteDatatype typeOne = new UnsignedIntegerOneByteDatatype();
        assertEquals(123L, typeOne.valueOf("123").getValue());

        UnsignedIntegerTwoBytesDatatype typeTwo = new UnsignedIntegerTwoBytesDatatype();
        assertEquals(257L, typeTwo.valueOf("257").getValue());

        UnsignedIntegerFourBytesDatatype typeFour = new UnsignedIntegerFourBytesDatatype();
        assertEquals(65536L, typeFour.valueOf("65536").getValue());
        assertEquals(4294967295L, typeFour.valueOf("4294967295").getValue());

        // Well, no need to write another test for that
        assertEquals(1L, typeFour.valueOf("4294967295").increment(true).getValue());
    }

    @Test
    void invalidUnsignedIntegersOne() {
        UnsignedIntegerOneByteDatatype typeOne = new UnsignedIntegerOneByteDatatype();
        assertThrows(InvalidValueException.class, () -> typeOne.valueOf("256"));
    }

    @Test
    void invalidUnsignedIntegersTwo() {
        UnsignedIntegerTwoBytesDatatype typeTwo = new UnsignedIntegerTwoBytesDatatype();
        assertThrows(InvalidValueException.class, () -> typeTwo.valueOf("65536"));
    }

    @Test
    void signedIntegers() {
        IntegerDatatype type = new IntegerDatatype(1);
        assertTrue(type.isValid(123));
        assertTrue(type.isValid(-124));
        assertEquals(123, type.valueOf("123"));
        assertEquals(-124, type.valueOf("-124"));
        assertFalse(type.isValid(256));

        type = new IntegerDatatype(2);
        assertTrue(type.isValid(257));
        assertTrue(type.isValid(-257));
        assertEquals(257, type.valueOf("257"));
        assertEquals(-257, type.valueOf("-257"));
        assertFalse(type.isValid(32768));
    }

    @Test
    void dateAndTime() {
        DateTimeDatatype type = (DateTimeDatatype) Datatype.Builtin.DATE.getDatatype();

        Calendar expected = Calendar.getInstance();
        expected.set(Calendar.YEAR, 2010);
        expected.set(Calendar.MONTH, 10);
        expected.set(Calendar.DAY_OF_MONTH, 3);
        expected.set(Calendar.HOUR_OF_DAY, 8);
        expected.set(Calendar.MINUTE, 9);
        expected.set(Calendar.SECOND, 10);

        Calendar parsedDate = type.valueOf("2010-11-03");
        assertEquals(expected.get(Calendar.YEAR), parsedDate.get(Calendar.YEAR));
        assertEquals(expected.get(Calendar.MONTH), parsedDate.get(Calendar.MONTH));
        assertEquals(expected.get(Calendar.DAY_OF_MONTH), parsedDate.get(Calendar.DAY_OF_MONTH));
        assertEquals("2010-11-03", type.getString(expected));

        type = (DateTimeDatatype) Datatype.Builtin.DATETIME.getDatatype();

        parsedDate = type.valueOf("2010-11-03");
        assertEquals(expected.get(Calendar.YEAR), parsedDate.get(Calendar.YEAR));
        assertEquals(expected.get(Calendar.MONTH), parsedDate.get(Calendar.MONTH));
        assertEquals(expected.get(Calendar.DAY_OF_MONTH), parsedDate.get(Calendar.DAY_OF_MONTH));

        parsedDate = type.valueOf("2010-11-03T08:09:10");
        assertEquals(expected.get(Calendar.YEAR), parsedDate.get(Calendar.YEAR));
        assertEquals(expected.get(Calendar.MONTH), parsedDate.get(Calendar.MONTH));
        assertEquals(expected.get(Calendar.DAY_OF_MONTH), parsedDate.get(Calendar.DAY_OF_MONTH));
        assertEquals(expected.get(Calendar.HOUR_OF_DAY), parsedDate.get(Calendar.HOUR_OF_DAY));
        assertEquals(expected.get(Calendar.MINUTE), parsedDate.get(Calendar.MINUTE));
        assertEquals(expected.get(Calendar.SECOND), parsedDate.get(Calendar.SECOND));

        assertEquals("2010-11-03T08:09:10", type.getString(expected));
    }

    @Test
    void dateAndTimeWithZone() {
        DateTimeDatatype type = new DateTimeDatatype(
                new String[] { "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ssZ" },
                "yyyy-MM-dd'T'HH:mm:ssZ") {
            @Override
            protected TimeZone getTimeZone() {
                // Set the "local" timezone to CET for the test
                return TimeZone.getTimeZone("CET");
            }
        };

        Calendar expected = Calendar.getInstance();
        expected.setTimeZone(TimeZone.getTimeZone("CET"));
        expected.set(Calendar.YEAR, 2010);
        expected.set(Calendar.MONTH, 10);
        expected.set(Calendar.DAY_OF_MONTH, 3);
        expected.set(Calendar.HOUR_OF_DAY, 8);
        expected.set(Calendar.MINUTE, 9);
        expected.set(Calendar.SECOND, 10);

        Calendar parsedDate = type.valueOf("2010-11-03T08:09:10");
        assertEquals(expected.get(Calendar.YEAR), parsedDate.get(Calendar.YEAR));
        assertEquals(expected.get(Calendar.MONTH), parsedDate.get(Calendar.MONTH));
        assertEquals(expected.get(Calendar.DAY_OF_MONTH), parsedDate.get(Calendar.DAY_OF_MONTH));
        assertEquals(expected.get(Calendar.HOUR_OF_DAY), parsedDate.get(Calendar.HOUR_OF_DAY));
        assertEquals(expected.get(Calendar.MINUTE), parsedDate.get(Calendar.MINUTE));
        assertEquals(expected.get(Calendar.SECOND), parsedDate.get(Calendar.SECOND));

        parsedDate = type.valueOf("2010-11-03T08:09:10+0100");
        assertEquals(expected.get(Calendar.YEAR), parsedDate.get(Calendar.YEAR));
        assertEquals(expected.get(Calendar.MONTH), parsedDate.get(Calendar.MONTH));
        assertEquals(expected.get(Calendar.DAY_OF_MONTH), parsedDate.get(Calendar.DAY_OF_MONTH));
        assertEquals(expected.get(Calendar.HOUR_OF_DAY), parsedDate.get(Calendar.HOUR_OF_DAY));
        assertEquals(expected.get(Calendar.MINUTE), parsedDate.get(Calendar.MINUTE));
        assertEquals(expected.get(Calendar.SECOND), parsedDate.get(Calendar.SECOND));
        assertEquals(expected.getTimeZone(), parsedDate.getTimeZone());

        assertEquals("2010-11-03T08:09:10+0100", type.getString(expected));
    }

    @Test
    void time() {
        DateTimeDatatype type = (DateTimeDatatype) Datatype.Builtin.TIME.getDatatype();

        Calendar expected = Calendar.getInstance();
        expected.setTime(new Date(0));
        expected.set(Calendar.HOUR_OF_DAY, 8);
        expected.set(Calendar.MINUTE, 9);
        expected.set(Calendar.SECOND, 10);

        Calendar parsedTime = type.valueOf("08:09:10");
        assertEquals(expected.get(Calendar.HOUR_OF_DAY), parsedTime.get(Calendar.HOUR_OF_DAY));
        assertEquals(expected.get(Calendar.MINUTE), parsedTime.get(Calendar.MINUTE));
        assertEquals(expected.get(Calendar.SECOND), parsedTime.get(Calendar.SECOND));
        assertEquals("08:09:10", type.getString(expected));
    }

    @Test
    void timeWithZone() {
        DateTimeDatatype type = new DateTimeDatatype(new String[] { "HH:mm:ssZ", "HH:mm:ss" }, "HH:mm:ssZ") {
            @Override
            protected TimeZone getTimeZone() {
                // Set the "local" timezone to CET for the test
                return TimeZone.getTimeZone("CET");
            }
        };

        Calendar expected = Calendar.getInstance();
        expected.setTimeZone(TimeZone.getTimeZone("CET"));
        expected.setTime(new Date(0));
        expected.set(Calendar.HOUR_OF_DAY, 8);
        expected.set(Calendar.MINUTE, 9);
        expected.set(Calendar.SECOND, 10);

        assertEquals(expected.getTimeInMillis(), type.valueOf("08:09:10").getTimeInMillis());
        assertEquals(expected.getTimeInMillis(), type.valueOf("08:09:10+0100").getTimeInMillis());
        assertEquals("08:09:10+0100", type.getString(expected));
    }

    @Test
    void base64() {
        Base64Datatype type = (Base64Datatype) Datatype.Builtin.BIN_BASE64.getDatatype();
        assertArrayEquals(new byte[] { 107, 86, -10 }, type.valueOf("a1b2"));
        assertEquals("a1b2", type.getString(new byte[] { 107, 86, -10 }));
    }

    @Test
    void simpleCSV() {
        List<String> csv = new CSVString("foo,bar,baz");
        assertEquals(3, csv.size());
        assertEquals("foo", csv.get(0));
        assertEquals("bar", csv.get(1));
        assertEquals("baz", csv.get(2));
        assertEquals("foo,bar,baz", csv.toString());

        csv = new CSVString("f\\\\oo,b\\,ar,b\\\\az");
        assertEquals(3, csv.size());
        assertEquals("f\\oo", csv.get(0));
        assertEquals("b,ar", csv.get(1));
        assertEquals("b\\az", csv.get(2));

        List<Boolean> csvBoolean = new CSVBoolean("1,1,0");
        assertEquals(3, csvBoolean.size());
        assertEquals(true, csvBoolean.get(0));
        assertEquals(true, csvBoolean.get(1));
        assertEquals(false, csvBoolean.get(2));
        assertEquals("1,1,0", csvBoolean.toString());
    }

    @Test
    void parseDLNADoc() {
        DLNADoc doc = DLNADoc.valueOf("DMS-1.50");
        assertEquals("DMS", doc.getDevClass());
        assertEquals(DLNADoc.Version.V1_5.toString(), doc.getVersion());
        assertEquals("DMS-1.50", doc.toString());

        doc = DLNADoc.valueOf("M-DMS-1.50");
        assertEquals("M-DMS", doc.getDevClass());
        assertEquals(DLNADoc.Version.V1_5.toString(), doc.getVersion());
        assertEquals("M-DMS-1.50", doc.toString());
    }

    @Test
    void caseSensitivity() {
        Datatype.Builtin dt = Datatype.Builtin.getByDescriptorName("datetime");
        assertNotNull(dt);
        dt = Datatype.Builtin.getByDescriptorName("dateTime");
        assertNotNull(dt);
        dt = Datatype.Builtin.getByDescriptorName("DATETIME");
        assertNotNull(dt);
    }

    @Test
    void valueOfDouble() {
        DoubleDatatype dt = (DoubleDatatype) Datatype.Builtin.R8.getDatatype();
        Double d = dt.valueOf("1.23");
        assertEquals(1.23d, d);
    }

    @Test
    void valueOfFloat() {
        FloatDatatype dt = (FloatDatatype) Datatype.Builtin.R4.getDatatype();
        Float f = dt.valueOf("1.23456");
        assertEquals(1.23456f, f);
    }
}
