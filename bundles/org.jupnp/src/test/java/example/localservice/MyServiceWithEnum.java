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
package example.localservice;

import org.jupnp.binding.annotations.*;

@UpnpService(serviceId = @UpnpServiceId("MyService"), serviceType = @UpnpServiceType(namespace = "mydomain", value = "MyService"), stringConvertibleTypes = MyStringConvertible.class)
public class MyServiceWithEnum {

    public enum Color {
        Red,
        Green,
        Blue
    }

    @UpnpStateVariable
    private Color color;

    @UpnpAction(out = @UpnpOutputArgument(name = "Out"))
    public Color getColor() {
        return color;
    }

    @UpnpAction
    public void setColor(@UpnpInputArgument(name = "In") String color) {
        this.color = Color.valueOf(color);
    }
}
