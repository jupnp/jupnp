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

import org.junit.jupiter.api.Test;
import org.jupnp.UpnpService;
import org.jupnp.binding.xml.DeviceDescriptorBinder;
import org.jupnp.binding.xml.UDA10DeviceDescriptorBinderImpl;
import org.jupnp.data.SampleData;
import org.jupnp.data.SampleDeviceRoot;
import org.jupnp.mock.MockUpnpService;
import org.jupnp.model.meta.Device;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.meta.RemoteService;
import org.jupnp.model.meta.StateVariable;
import org.jupnp.model.meta.StateVariableAllowedValueRange;
import org.jupnp.model.meta.StateVariableTypeDetails;
import org.jupnp.model.resource.Resource;
import org.jupnp.model.resource.ServiceEventCallbackResource;
import org.jupnp.model.types.DLNADoc;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.IntegerDatatype;
import org.jupnp.model.types.ServiceId;
import org.jupnp.model.types.ServiceType;
import org.jupnp.model.types.UDADeviceType;
import org.jupnp.model.types.UDAServiceId;
import org.jupnp.model.types.UDN;

class IncompatibilityTest {

    @Test
    void validateMSFTServiceType() {
        // TODO: UPNP VIOLATION: Microsoft violates the spec and sends periods in domain names instead of hyphens!
        ServiceType serviceType = ServiceType.valueOf("urn:microsoft.com:service:X_MS_MediaReceiverRegistrar:1");
        assertEquals("microsoft.com", serviceType.getNamespace());
        assertEquals("X_MS_MediaReceiverRegistrar", serviceType.getType());
        assertEquals(1, serviceType.getVersion());
    }

    // TODO: UPNP VIOLATION: Azureus sends a URN as the service ID suffix. This doesn't violate the spec but it's
    // unusual...
    @Test
    void validateAzureusServiceId() {
        ServiceId serviceId = ServiceId
                .valueOf("urn:upnp-org:serviceId:urn:schemas-upnp-org:service:ConnectionManager");
        assertEquals("upnp-org", serviceId.getNamespace());
        assertEquals("urn:schemas-upnp-org:service:ConnectionManager", serviceId.getId());
    }

    // TODO: UPNP VIOLATION: PS Audio Bridge has invalid service IDs
    @Test
    void validatePSAudioBridgeServiceId() {
        ServiceId serviceId = ServiceId.valueOf("urn:foo:ThisSegmentShouldBeNamed'service':baz");
        assertEquals("foo", serviceId.getNamespace());
        assertEquals("baz", serviceId.getId());
    }

    // TODO: UPNP VIOLATION: Some devices send spaces in URNs
    @Test
    void validateSpacesInServiceType() {
        String st = "urn:schemas-upnp-org:service: WANDSLLinkConfig:1";
        ServiceType serviceType = ServiceType.valueOf(st);
        assertEquals("schemas-upnp-org", serviceType.getNamespace());
        assertEquals("WANDSLLinkConfig", serviceType.getType());
        assertEquals(1, serviceType.getVersion());
    }

    @Test
    void validateIntelServiceId() {
        // The Intel UPnP tools NetworkLight sends a valid but weird identifier with a dot
        ServiceId serviceId = ServiceId.valueOf("urn:upnp-org:serviceId:DimmingService.0001");
        assertEquals("upnp-org", serviceId.getNamespace());
        assertEquals("DimmingService.0001", serviceId.getId());

        // TODO: UPNP VIOLATION: The Intel UPnP tools MediaRenderer sends an invalid identifier, we need to deal
        serviceId = ServiceId.valueOf("urn:schemas-upnp-org:service:AVTransport");
        assertEquals("upnp-org", serviceId.getNamespace());
        assertEquals("AVTransport", serviceId.getId());
    }

    @Test
    void readColonRelativePaths() throws Exception {
        // Funny URI paths for services, breaks the java.net.URI parser so we deal with this special, see
        // UDA10DeviceDescriptorBinderImpl
        // @formatter:off
        String descriptor =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<root xmlns=\"urn:schemas-upnp-org:device-1-0\">\n" +
                        "   <specVersion>\n" +
                        "      <major>1</major>\n" +
                        "      <minor>0</minor>\n" +
                        "   </specVersion>\n" +
                        "   <device>\n" +
                        "      <deviceType>urn:schemas-upnp-org:device:BinaryLight:1</deviceType>\n" +
                        "      <presentationURL>/</presentationURL>\n" +
                        "      <friendlyName>Network Light (CB-1CE8FF0B14FA)</friendlyName>\n" +
                        "      <manufacturer>OpenSource</manufacturer>\n" +
                        "      <manufacturerURL>http://www.sourceforge.org</manufacturerURL>\n" +
                        "      <modelDescription>Software Emulated Light Bulb</modelDescription>\n" +
                        "      <modelName>Network Light Bulb</modelName>\n" +
                        "      <modelNumber>XPC-L1</modelNumber>\n" +
                        "      <modelURL>http://www.sourceforge.org/</modelURL>\n" +
                        "      <UDN>uuid:872843be-9fb4-4eb4-8250-0b629c047a27</UDN>\n" +
                        "      <iconList>\n" +
                        "         <icon>\n" +
                        "            <mimetype>image/png</mimetype>\n" +
                        "            <width>32</width>\n" +
                        "            <height>32</height>\n" +
                        "            <depth>32</depth>\n" +
                        "            <url>/icon.png</url>\n" +
                        "         </icon>\n" +
                        "         <icon>\n" +
                        "            <mimetype>image/jpg</mimetype>\n" +
                        "            <width>32</width>\n" +
                        "            <height>32</height>\n" +
                        "            <depth>32</depth>\n" +
                        "            <url>/icon.jpg</url>\n" +
                        "         </icon>\n" +
                        "      </iconList>\n" +
                        "      <serviceList>\n" +
                        "         <service>\n" +
                        "            <serviceType>urn:schemas-upnp-org:service:DimmingService:1</serviceType>\n" +
                        "            <serviceId>urn:upnp-org:serviceId:DimmingService.0001</serviceId>\n" +
                        "            <SCPDURL>_urn:upnp-org:serviceId:DimmingService.0001_scpd.xml</SCPDURL>\n" +
                        "            <controlURL>_urn:upnp-org:serviceId:DimmingService.0001_control</controlURL>\n" +
                        "            <eventSubURL>_urn:upnp-org:serviceId:DimmingService.0001_event</eventSubURL>\n" +
                        "         </service>\n" +
                        "         <service>\n" +
                        "            <serviceType>urn:schemas-upnp-org:service:SwitchPower:1</serviceType>\n" +
                        "            <serviceId>urn:upnp-org:serviceId:SwitchPower.0001</serviceId>\n" +
                        "            <SCPDURL>_urn:upnp-org:serviceId:SwitchPower.0001_scpd.xml</SCPDURL>\n" +
                        "            <controlURL>_urn:upnp-org:serviceId:SwitchPower.0001_control</controlURL>\n" +
                        "            <eventSubURL>_urn:upnp-org:serviceId:SwitchPower.0001_event</eventSubURL>\n" +
                        "         </service>\n" +
                        "      </serviceList>\n" +
                        "   </device>\n" +
                        "</root>";
        // @formatter:on

        DeviceDescriptorBinder binder = new UDA10DeviceDescriptorBinderImpl();
        RemoteDevice device = new RemoteDevice(SampleData.createRemoteDeviceIdentity());
        device = binder.describe(device, descriptor);
        assertEquals(2, device.findServices().length);
    }

    // TODO: UPNP VIOLATION: Roku Soundbridge cuts off callback URI path after 100 characters.
    @Test
    void validateCallbackURILength() throws Exception {
        UpnpService upnpService = new MockUpnpService();
        Device<RemoteDeviceIdentity, RemoteDevice, RemoteService> dev = SampleData
                .createRemoteDevice(new RemoteDeviceIdentity(UDN.uniqueSystemIdentifier("I'mARokuSoundbridge"), 1800,
                        SampleDeviceRoot.getDeviceDescriptorURL(), null, SampleData.getLocalBaseAddress()));
        Resource[] resources = upnpService.getConfiguration().getNamespace().getResources(dev);
        boolean test = false;
        for (Resource resource : resources) {
            if (!(resource instanceof ServiceEventCallbackResource)) {
                continue;
            }
            if (resource.getPathQuery().toString().length() < 100) {
                test = true;
            }
        }
        assertTrue(test);
    }

    // TODO: UPNP VIOLATION: Some devices use non-integer service/device type versions
    @Test
    void parseUDADeviceTypeFractions() {
        UDADeviceType deviceType = (UDADeviceType) DeviceType.valueOf("urn:schemas-upnp-org:device:MyDeviceType:1.0");
        assertEquals("MyDeviceType", deviceType.getType());
        assertEquals(1, deviceType.getVersion());
        deviceType = (UDADeviceType) DeviceType.valueOf("urn:schemas-upnp-org:device:MyDeviceType:2.5");
        assertEquals("MyDeviceType", deviceType.getType());
        assertEquals(2, deviceType.getVersion());
    }

    // TODO: UPNP VIOLATION: Of course, adding more rules makes more devices compatible! DLNA genuises ftw!
    @Test
    void parseInvalidDLNADoc() {
        DLNADoc doc = DLNADoc.valueOf("DMS 1.50"); // No hyphen
        assertEquals("DMS", doc.getDevClass());
        assertEquals(DLNADoc.Version.V1_5.toString(), doc.getVersion());
        assertEquals("DMS-1.50", doc.toString());
    }

    // TODO: UPNP VIOLATION: DirecTV HR23/700 High Definition DVR Receiver has invalid default value for statevar
    @Test
    void invalidStateVarDefaultValue() {
        StateVariable stateVariable = new StateVariable("Test", new StateVariableTypeDetails(
                Datatype.Builtin.STRING.getDatatype(), "A", new String[] { "B", "C" }, null));

        boolean foundA = false;
        for (String s : stateVariable.getTypeDetails().getAllowedValues()) {
            if (s.equals("A")) {
                foundA = true;
            }
        }
        assertTrue(foundA);
        assertEquals(3, stateVariable.getTypeDetails().getAllowedValues().length);
        assertEquals(0, stateVariable.validate().size());
    }

    // TODO: UPNP VIOLATION: Onkyo NR-TX808 has a bug in RenderingControl service, switching maximum/minimum value range
    @Test
    void switchedMinimumMaximumValueRange() {
        StateVariable stateVariable = new StateVariable("Test", new StateVariableTypeDetails(
                Datatype.Builtin.I2.getDatatype(), null, null, new StateVariableAllowedValueRange(100, 0)));

        assertEquals(0, stateVariable.validate().size());
        assertEquals(0, stateVariable.getTypeDetails().getAllowedValueRange().getMinimum());
        assertEquals(100, stateVariable.getTypeDetails().getAllowedValueRange().getMaximum());
    }

    // TODO: UPNP VIOLATION: Some renderers (like PacketVideo TMM Player) send
    // RelCount and AbsCount as "NOT_IMPLEMENTED" in GetPositionInfoResponse action.
    @Test
    void invalidIntegerValue() {
        assertEquals(Byte.MAX_VALUE, new IntegerDatatype(1).valueOf("NOT_IMPLEMENTED").intValue());
        assertEquals(Short.MAX_VALUE, new IntegerDatatype(2).valueOf("NOT_IMPLEMENTED").intValue());
        assertEquals(Integer.MAX_VALUE, new IntegerDatatype(4).valueOf("NOT_IMPLEMENTED").intValue());
    }

    @Test
    void parseBrokenServiceType() {
        ServiceType serviceType = ServiceType.valueOf("urn:schemas-upnp-org:serviceId:Foo:1");
        assertEquals("schemas-upnp-org", serviceType.getNamespace());
        assertEquals("Foo", serviceType.getType());
        assertEquals(1, serviceType.getVersion());
    }

    @Test
    void parseBrokenServiceId() {
        ServiceId serviceId = ServiceId.valueOf("urn:my-domain-namespace:service:MyService123");
        assertEquals("my-domain-namespace", serviceId.getNamespace());
        assertEquals("MyService123", serviceId.getId());
        assertEquals("urn:my-domain-namespace:serviceId:MyService123", serviceId.toString());
    }

    @Test
    void parseServiceNameAsServiceId() {
        UDAServiceId serviceId = UDAServiceId.valueOf("ContentDirectory");
        assertEquals(UDAServiceId.DEFAULT_NAMESPACE, serviceId.getNamespace());
        assertEquals("ContentDirectory", serviceId.getId());
        assertEquals("urn:" + UDAServiceId.DEFAULT_NAMESPACE + ":serviceId:ContentDirectory", serviceId.toString());

        serviceId = UDAServiceId.valueOf("ConnectionManager");
        assertEquals(UDAServiceId.DEFAULT_NAMESPACE, serviceId.getNamespace());
        assertEquals("ConnectionManager", serviceId.getId());
        assertEquals("urn:" + UDAServiceId.DEFAULT_NAMESPACE + ":serviceId:ConnectionManager", serviceId.toString());

        serviceId = UDAServiceId.valueOf("RenderingControl");
        assertEquals(UDAServiceId.DEFAULT_NAMESPACE, serviceId.getNamespace());
        assertEquals("RenderingControl", serviceId.getId());
        assertEquals("urn:" + UDAServiceId.DEFAULT_NAMESPACE + ":serviceId:RenderingControl", serviceId.toString());

        serviceId = UDAServiceId.valueOf("AVTransport");
        assertEquals(UDAServiceId.DEFAULT_NAMESPACE, serviceId.getNamespace());
        assertEquals("AVTransport", serviceId.getId());
        assertEquals("urn:" + UDAServiceId.DEFAULT_NAMESPACE + ":serviceId:AVTransport", serviceId.toString());
    }

    // TODO: UPNP VIOLATION: EyeTV Netstream uses colons in device type token
    @Test
    void parseEyeTVDeviceType() {
        DeviceType deviceType = DeviceType.valueOf("urn:schemas-microsoft-com:device:pbda:tuner:1");
        assertEquals("schemas-microsoft-com", deviceType.getNamespace());
        assertEquals("pbda-tuner", deviceType.getType());
        assertEquals(1, deviceType.getVersion());
    }

    // TODO: UPNP VIOLATION: EyeTV Netstream uses colons in service type token
    @Test
    void parseEyeTVServiceType() {
        ServiceType serviceType = ServiceType.valueOf("urn:schemas-microsoft-com:service:pbda:tuner:1");
        assertEquals("schemas-microsoft-com", serviceType.getNamespace());
        assertEquals("pbda-tuner", serviceType.getType());
        assertEquals(1, serviceType.getVersion());
    }

    // TODO: UPNP VIOLATION: Ceyton InfiniTV uses colons in service type token and 'serviceId' instead of 'service'
    @Test
    void parseCeytonInfiniTVServiceType() {
        ServiceType serviceType = ServiceType.valueOf("urn:schemas-opencable-com:serviceId:dri2:debug:1");
        assertEquals("schemas-opencable-com", serviceType.getNamespace());
        assertEquals("dri2-debug", serviceType.getType());
        assertEquals(1, serviceType.getVersion());
    }

    // TODO: UPNP VIOLATION: Handle garbage sent by Eyecon Android app
    @Test
    void parseEyeconServiceId() {
        ServiceId serviceId = ServiceId.valueOf("urn:upnp-orgerviceId:urnchemas-upnp-orgervice:Foo");
        assertEquals(UDAServiceId.DEFAULT_NAMESPACE, serviceId.getNamespace());
        assertEquals("Foo", serviceId.getId());
        assertEquals("urn:" + UDAServiceId.DEFAULT_NAMESPACE + ":serviceId:Foo", serviceId.toString());
    }

    // TODO: UPNP VIOLATION: Escient doesn't provide any device type token
    @Test
    void parseEscientDeviceType() {
        DeviceType deviceType = DeviceType.valueOf("urn:schemas-upnp-org:device::1");
        assertEquals("schemas-upnp-org", deviceType.getNamespace());
        assertEquals(DeviceType.UNKNOWN, deviceType.getType());
        assertEquals(1, deviceType.getVersion());
    }

    // TODO: UPNP VIOLATION: Kodak Media Server doesn't provide any service ID token
    @Test
    void parseKodakServiceId() {
        ServiceId serviceId = ServiceId.valueOf("urn:upnp-org:serviceId:");
        assertEquals("upnp-org", serviceId.getNamespace());
        assertEquals(ServiceId.UNKNOWN, serviceId.getId());
        assertEquals("urn:upnp-org:serviceId:" + ServiceId.UNKNOWN, serviceId.toString());
    }
}
