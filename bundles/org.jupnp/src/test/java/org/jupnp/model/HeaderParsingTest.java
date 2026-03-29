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

import java.net.URI;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.jupnp.model.message.header.*;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.NamedDeviceType;
import org.jupnp.model.types.NamedServiceType;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceType;
import org.jupnp.util.MimeType;

class HeaderParsingTest {

    @Test
    void parseContentTypeHeader() {
        ContentTypeHeader header = new ContentTypeHeader(MimeType.valueOf("foo/bar;charset=\"utf-8\""));
        assertEquals("foo/bar;charset=\"utf-8\"", header.getString());
    }

    @Test
    void parseDeviceType() {
        DeviceType deviceType = DeviceType.valueOf("urn:foo-bar:device:MyDeviceType:123");
        assertEquals("foo-bar", deviceType.getNamespace());
        assertEquals("MyDeviceType", deviceType.getType());
        assertEquals(123, deviceType.getVersion());
    }

    @Test
    void parseUDADeviceType() {
        UDADeviceType deviceType = (UDADeviceType) DeviceType.valueOf("urn:schemas-upnp-org:device:MyDeviceType:123");
        assertEquals("MyDeviceType", deviceType.getType());
        assertEquals(123, deviceType.getVersion());
    }

    @Test
    void parseInvalidDeviceTypeHeader() {
        DeviceTypeHeader header = new DeviceTypeHeader();
        header.setString("urn:foo-bar:device:!@#:123");
        assertEquals("foo-bar", header.getValue().getNamespace());
        assertEquals("---", header.getValue().getType());
        assertEquals(123, header.getValue().getVersion());
        assertEquals("urn:foo-bar:device:---:123", header.getString());
    }

    @Test
    void parseDeviceTypeHeaderURI() {
        DeviceTypeHeader header = new DeviceTypeHeader(URI.create("urn:schemas-upnp-org:device:MyDeviceType:123"));
        assertEquals("schemas-upnp-org", header.getValue().getNamespace());
        assertEquals("MyDeviceType", header.getValue().getType());
        assertEquals(123, header.getValue().getVersion());
        assertEquals("urn:schemas-upnp-org:device:MyDeviceType:123", header.getString());
    }

    @Test
    void parseDeviceUSNHeader() {
        DeviceUSNHeader header = new DeviceUSNHeader();
        header.setString("uuid:MY-DEVICE-123::urn:schemas-upnp-org:device:MY-DEVICE-TYPE:1");
        assertEquals("MY-DEVICE-123", header.getValue().getUdn().getIdentifierString());
        assertInstanceOf(UDADeviceType.class, header.getValue().getDeviceType());
    }

    @Test
    void parseDeviceUSNHeaderStatic() {
        DeviceUSNHeader header = new DeviceUSNHeader(
                NamedDeviceType.valueOf("uuid:MY-DEVICE-123::urn:schemas-upnp-org:device:MY-DEVICE-TYPE:1"));
        assertEquals("MY-DEVICE-123", header.getValue().getUdn().getIdentifierString());
        assertInstanceOf(UDADeviceType.class, header.getValue().getDeviceType());
    }

    @Test
    void parseInvalidDeviceUSNHeader() {
        DeviceUSNHeader header = new DeviceUSNHeader();
        assertThrows(InvalidHeaderException.class,
                () -> header.setString("uuid:MY-DEVICE-123--urn:schemas-upnp-org:device:MY-DEVICE-TYPE:1"));
    }

    @Test
    void parseInvalidEXTHeader() {
        EXTHeader header = new EXTHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("MUST BE EMPTY STRING"));
    }

    @Test
    void parseHostHeaderConstructor() {
        HostHeader header = new HostHeader("foo.bar", 1234);
        assertEquals("foo.bar", header.getValue().getHost());
        assertEquals(1234, header.getValue().getPort());

        header = new HostHeader(1234);
        assertEquals(Constants.IPV4_UPNP_MULTICAST_GROUP, header.getValue().getHost());
        assertEquals(1234, header.getValue().getPort());
    }

    @Test
    void parseHostHeader() {
        HostHeader header = new HostHeader();
        assertEquals(Constants.IPV4_UPNP_MULTICAST_GROUP, header.getValue().getHost());
        assertEquals(Constants.UPNP_MULTICAST_PORT, header.getValue().getPort());

        header = new HostHeader();
        header.setString("foo.bar:1234");
        assertEquals("foo.bar", header.getValue().getHost());
        assertEquals(1234, header.getValue().getPort());

        header = new HostHeader();
        header.setString("foo.bar");
        assertEquals("foo.bar", header.getValue().getHost());
        assertEquals(Constants.UPNP_MULTICAST_PORT, header.getValue().getPort());
    }

    @Test
    void parseInvalidHostHeader() {
        HostHeader header = new HostHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("foo.bar:abc"));
    }

    @Test
    void parseInvalidLocationHeader() {
        LocationHeader header = new LocationHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("this://is.not...a valid URL"));
    }

    @Test
    void parseInvalidMANHeader() {
        MANHeader header = new MANHeader("abc");
        assertThrows(InvalidHeaderException.class, () -> header.setString("\"foo.bar\"; ns = baz"));
    }

    @Test
    void parseMANHeaderNoNS() {
        MANHeader header = new MANHeader("abc");
        header.setString("\"foo.bar\"");
        assertEquals("foo.bar", header.getValue());
        assertNull(header.getNamespace());
        assertEquals("\"foo.bar\"", header.getString());
    }

    @Test
    void parseMANHeaderNS() {
        MANHeader header = new MANHeader("abc");
        header.setString("\"foo.bar\"; ns =12");
        assertEquals("foo.bar", header.getValue());
        assertEquals("12", header.getNamespace());
        assertEquals("\"foo.bar\"; ns=12", header.getString());
    }

    @Test
    void parseMaxAgeHeader() {
        MaxAgeHeader header = new MaxAgeHeader();
        header.setString("max-age=1234, foobar=baz");
        assertEquals(1234, header.getValue());
    }

    @Test
    void parseInvalidMaxAgeHeader() {
        MaxAgeHeader header = new MaxAgeHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("max-foo=123"));
    }

    @Test
    void parseMXHeader() {
        MXHeader header = new MXHeader();
        header.setString("111");
        assertEquals(111, header.getValue());

        header = new MXHeader();
        header.setString("123");
        assertEquals(MXHeader.DEFAULT_VALUE, header.getValue());
    }

    @Test
    void parseInvalidMXHeader() {
        MXHeader header = new MXHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("abc"));
    }

    @Test
    void parseInvalidNTSHeader() {
        NTSHeader header = new NTSHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("foo"));
    }

    @Test
    void parseInvalidRootDeviceHeader() {
        RootDeviceHeader header = new RootDeviceHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("upnp:foodevice"));
    }

    @Test
    void parseServerHeader() {
        ServerHeader header = new ServerHeader();
        header.setString("foo/1 UPnP/1.1 bar/2");
        assertEquals("foo", header.getValue().getOsName());
        assertEquals("1", header.getValue().getOsVersion());
        assertEquals("bar", header.getValue().getProductName());
        assertEquals("2", header.getValue().getProductVersion());
        assertEquals(1, header.getValue().getMajorVersion());
        assertEquals(1, header.getValue().getMinorVersion());

        // Commas...
        header = new ServerHeader();
        header.setString("foo/1, UPnP/1.1, bar/2");
        assertEquals("foo", header.getValue().getOsName());
        assertEquals("1", header.getValue().getOsVersion());
        assertEquals("bar", header.getValue().getProductName());
        assertEquals("2", header.getValue().getProductVersion());
        assertEquals(1, header.getValue().getMajorVersion());
        assertEquals(1, header.getValue().getMinorVersion());

        // Whitespace in tokens
        header = new ServerHeader();
        header.setString("foo baz/1 UPnP/1.1 bar abc/2");
        assertEquals("foo baz", header.getValue().getOsName());
        assertEquals("1", header.getValue().getOsVersion());
        assertEquals("bar abc", header.getValue().getProductName());
        assertEquals("2", header.getValue().getProductVersion());
        assertEquals(1, header.getValue().getMajorVersion());
        assertEquals(1, header.getValue().getMinorVersion());

        // Commas and whitespace!
        header = new ServerHeader();
        header.setString("foo baz/1, UPnP/1.1, bar abc/2");
        assertEquals("foo baz", header.getValue().getOsName());
        assertEquals("1", header.getValue().getOsVersion());
        assertEquals("bar abc", header.getValue().getProductName());
        assertEquals("2", header.getValue().getProductVersion());
        assertEquals(1, header.getValue().getMajorVersion());
        assertEquals(1, header.getValue().getMinorVersion());

        // Absolutely not valid!
        header = new ServerHeader();
        header.setString("foo/1 UPnP/1.");
        assertEquals(ServerClientTokens.UNKNOWN_PLACEHOLDER, header.getValue().getOsName());
        assertEquals(ServerClientTokens.UNKNOWN_PLACEHOLDER, header.getValue().getOsVersion());
        assertEquals(ServerClientTokens.UNKNOWN_PLACEHOLDER, header.getValue().getProductName());
        assertEquals(ServerClientTokens.UNKNOWN_PLACEHOLDER, header.getValue().getProductVersion());
        assertEquals(1, header.getValue().getMajorVersion());
        assertEquals(0, header.getValue().getMinorVersion()); // Assume UDA 1.0
    }

    @Test
    void parseInvalidServerHeader() {
        ServerHeader header = new ServerHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("foo/1 baz/123 bar/2"));
    }

    @Test
    void parseServiceType() {
        ServiceType serviceType = ServiceType.valueOf("urn:foo-bar:service:MyServiceType:123");
        assertEquals("foo-bar", serviceType.getNamespace());
        assertEquals("MyServiceType", serviceType.getType());
        assertEquals(123, serviceType.getVersion());
    }

    @Test
    void parseUDAServiceType() {
        UDAServiceType serviceType = (UDAServiceType) ServiceType
                .valueOf("urn:schemas-upnp-org:service:MyServiceType:123");
        assertEquals("MyServiceType", serviceType.getType());
        assertEquals(123, serviceType.getVersion());
    }

    @Test
    void parseInvalidServiceTypeHeader() {
        ServiceTypeHeader header = new ServiceTypeHeader();
        header.setString("urn:foo-bar:service:!@#:123");
        assertEquals("foo-bar", header.getValue().getNamespace());
        assertEquals("---", header.getValue().getType());
        assertEquals(123, header.getValue().getVersion());
        assertEquals("urn:foo-bar:service:---:123", header.getString());
    }

    @Test
    void parseServiceTypeHeaderURI() {
        ServiceTypeHeader header = new ServiceTypeHeader(URI.create("urn:schemas-upnp-org:service:MyServiceType:123"));
        assertEquals("schemas-upnp-org", header.getValue().getNamespace());
        assertEquals("MyServiceType", header.getValue().getType());
        assertEquals(123, header.getValue().getVersion());
        assertEquals("urn:schemas-upnp-org:service:MyServiceType:123", header.getString());
    }

    @Test
    void parseServiceUSNHeader() {
        ServiceUSNHeader header = new ServiceUSNHeader();
        header.setString("uuid:MY-SERVICE-123::urn:schemas-upnp-org:service:MY-SERVICE-TYPE:1");
        assertEquals("MY-SERVICE-123", header.getValue().getUdn().getIdentifierString());
        assertInstanceOf(UDAServiceType.class, header.getValue().getServiceType());
    }

    @Test
    void parseServiceUSNHeaderStatic() {
        ServiceUSNHeader header = new ServiceUSNHeader(
                NamedServiceType.valueOf("uuid:MY-SERVICE-123::urn:schemas-upnp-org:service:MY-SERVICE-TYPE:1"));
        assertEquals("MY-SERVICE-123", header.getValue().getUdn().getIdentifierString());
        assertInstanceOf(UDAServiceType.class, header.getValue().getServiceType());
    }

    @Test
    void parseInvalidServiceUSNHeader() {
        ServiceUSNHeader header = new ServiceUSNHeader();
        assertThrows(InvalidHeaderException.class,
                () -> header.setString("uuid:MY-SERVICE-123--urn:schemas-upnp-org:service:MY-SERVICE-TYPE:1"));
    }

    @Test
    void parseInvalidSTAllHeader() {
        STAllHeader header = new STAllHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("ssdp:foo"));
    }

    @Test
    void parseInvalidUDADeviceTypeHeader() {
        UDADeviceTypeHeader header = new UDADeviceTypeHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("urn:foo-bar:device:!@#:123"));
    }

    @Test
    void parseUDADeviceTypeHeaderURI() {
        UDADeviceTypeHeader header = new UDADeviceTypeHeader(
                URI.create("urn:schemas-upnp-org:device:MyDeviceType:123"));
        assertEquals("schemas-upnp-org", header.getValue().getNamespace());
        assertEquals("MyDeviceType", header.getValue().getType());
        assertEquals(123, header.getValue().getVersion());
        assertEquals("urn:schemas-upnp-org:device:MyDeviceType:123", header.getString());
    }

    @Test
    void parseInvalidUDAServiceTypeHeader() {
        UDAServiceTypeHeader header = new UDAServiceTypeHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("urn:foo-bar:service:!@#:123"));
    }

    @Test
    void parseUDAServiceTypeHeaderURI() {
        UDAServiceTypeHeader header = new UDAServiceTypeHeader(
                URI.create("urn:schemas-upnp-org:service:MyServiceType:123"));
        assertEquals("schemas-upnp-org", header.getValue().getNamespace());
        assertEquals("MyServiceType", header.getValue().getType());
        assertEquals(123, header.getValue().getVersion());
        assertEquals("urn:schemas-upnp-org:service:MyServiceType:123", header.getString());
    }

    @Test
    void parseUDNHeader() {
        UDNHeader header = new UDNHeader();
        header.setString("uuid:MY-UUID-1234");
        assertEquals("MY-UUID-1234", header.getValue().getIdentifierString());
    }

    @Test
    void parseInvalidUDNHeaderPrefix() {
        UDNHeader header = new UDNHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("MY-UUID-1234"));
    }

    @Test
    void parseInvalidUDNHeaderURN() {
        UDNHeader header = new UDNHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("uuid:MY-UUID-1234::urn:foo-bar:baz"));
    }

    @Test
    void parseInvalidUSNRootDeviceHeader() {
        USNRootDeviceHeader header = new USNRootDeviceHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("uuid:MY-UUID-1234::upnp:rootfoo"));
    }

    @Test
    void parseSoapActionHeader() {
        SoapActionHeader header = new SoapActionHeader(
                URI.create("urn:schemas-upnp-org:service:MyServiceType:1#MyAction"));
        assertEquals("schemas-upnp-org", header.getValue().getServiceType().getNamespace());
        assertEquals("MyServiceType", header.getValue().getServiceType().getType());
        assertEquals(1, header.getValue().getServiceType().getVersion());
        assertEquals("MyAction", header.getValue().getActionName());
        assertEquals("\"urn:schemas-upnp-org:service:MyServiceType:1#MyAction\"", header.getString());
    }

    @Test
    void parseSoapActionHeaderString() {
        SoapActionHeader header = new SoapActionHeader();
        header.setString("\"urn:schemas-upnp-org:service:MyServiceType:1#MyAction\"");
        assertEquals("schemas-upnp-org", header.getValue().getServiceType().getNamespace());
        assertEquals("MyServiceType", header.getValue().getServiceType().getType());
        assertEquals(1, header.getValue().getServiceType().getVersion());
        assertEquals("MyAction", header.getValue().getActionName());
        assertEquals("\"urn:schemas-upnp-org:service:MyServiceType:1#MyAction\"", header.getString());
    }

    @Test
    void parseSoapActionHeaderQueryString() {
        SoapActionHeader header = new SoapActionHeader();
        header.setString("\"urn:schemas-upnp-org:control-1-0#QueryStateVariable\"");
        assertNull(header.getValue().getServiceType());
        assertEquals("control-1-0", header.getValue().getType());
        assertNull(header.getValue().getVersion());
        assertEquals("QueryStateVariable", header.getValue().getActionName());
        assertEquals("\"urn:schemas-upnp-org:control-1-0#QueryStateVariable\"", header.getString());
    }

    @Test
    void parseEventSequenceHeaderString() {
        EventSequenceHeader header = new EventSequenceHeader();
        header.setString("0");
        assertEquals(0L, header.getValue().getValue());
        header.setString("001");
        assertEquals(1L, header.getValue().getValue());
        header.setString("123");
        assertEquals(123L, header.getValue().getValue());
    }

    @Test
    void parseTimeoutHeaderString() {
        TimeoutHeader header = new TimeoutHeader();
        header.setString("Second-123");
        assertEquals(123, header.getValue());
        header.setString("Second-infinite");
        assertEquals(TimeoutHeader.INFINITE_VALUE, header.getValue());
    }

    @Test
    void parseCallbackHeaderString() {
        CallbackHeader header = new CallbackHeader();
        header.setString("<http://127.0.0.1/foo>");
        assertEquals(1, header.getValue().size());
        assertEquals("http://127.0.0.1/foo", header.getValue().get(0).toString());

        header.setString("<http://127.0.0.1/foo><http://127.0.0.1/bar>");
        assertEquals(2, header.getValue().size());
        assertEquals("http://127.0.0.1/foo", header.getValue().get(0).toString());
        assertEquals("http://127.0.0.1/bar", header.getValue().get(1).toString());

        header.setString("<http://127.0.0.1/foo> <http://127.0.0.1/bar>");
        assertEquals(2, header.getValue().size());
        assertEquals("http://127.0.0.1/foo", header.getValue().get(0).toString());
        assertEquals("http://127.0.0.1/bar", header.getValue().get(1).toString());
    }

    @Test
    void parseInvalidCallbackHeaderString() {
        CallbackHeader header = new CallbackHeader();
        header.setString("<http://127.0.0.1/foo> <ftp://127.0.0.1/bar>");
        assertEquals(1, header.getValue().size());
        assertEquals("http://127.0.0.1/foo", header.getValue().get(0).toString());
    }

    @Test
    void parseSubscriptionIdHeaderString() {
        SubscriptionIdHeader header = new SubscriptionIdHeader();
        header.setString("uuid:123-123-123-123");
        assertEquals("uuid:123-123-123-123", header.getValue());
    }

    @Test
    void parseInvalidSubscriptionIdHeaderString() {
        SubscriptionIdHeader header = new SubscriptionIdHeader();
        assertThrows(InvalidHeaderException.class, () -> header.setString("abc:123-123-123-123"));
    }

    @Test
    void parseInterfaceMacAddress() {
        InterfaceMacHeader header = new InterfaceMacHeader("00:17:ab:e9:65:a0");
        assertEquals(6, header.getValue().length);
        assertEquals("00:17:AB:E9:65:A0", header.getString().toUpperCase(Locale.ENGLISH));
    }

    @Test
    void parseRange() {
        RangeHeader header = new RangeHeader("bytes=1539686400-1540210688");
        assertEquals(1539686400L, header.getValue().getFirstByte());
        assertEquals(1540210688L, header.getValue().getLastByte());
        assertEquals("bytes=1539686400-1540210688", header.getString());
    }

    @Test
    void parseContentRange() {
        ContentRangeHeader header = new ContentRangeHeader("bytes 1539686400-1540210688/21323123");
        assertEquals(1539686400L, header.getValue().getFirstByte());
        assertEquals(1540210688L, header.getValue().getLastByte());
        assertEquals(21323123L, header.getValue().getByteLength());
        assertEquals("bytes 1539686400-1540210688/21323123", header.getString());
    }

    @Test
    void parsePragma() {
        PragmaHeader header = new PragmaHeader("no-cache");
        assertEquals("no-cache", header.getValue().getValue());
        assertEquals("no-cache", header.getString());

        header.setString("token=value");
        assertEquals("token", header.getValue().getToken());
        assertEquals("value", header.getValue().getValue());
        assertEquals("token=value", header.getString());

        header.setString("token=\"value\"");
        assertEquals("token", header.getValue().getToken());
        assertEquals("value", header.getValue().getValue());
        assertEquals("token=\"value\"", header.getString());
    }
}
