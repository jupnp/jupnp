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
package org.jupnp.model.profile;

import org.jupnp.model.meta.DeviceDetails;

/**
 * Provides custom device details metadata based on control point profile.
 * <p>
 * Use this instead of {@link DeviceDetails} when you create a
 * {@link org.jupnp.model.meta.LocalDevice} if dynamic metadata is
 * required - e.g. when your control points expect different DLNA capabilities
 * or if they are otherwise incompatible with the standard metadata of the
 * service you provide. You can then provide custom metadata for each
 * control point based on the detected control point information.
 * </p>
 * <p>
 * Don't forget to provide a default, that is, if none of your conditions match
 * you still have to provide a minimal {@link DeviceDetails} instance for
 * generic control points.
 * </p>
 *
 * @author Mario Franco
 * @author Christian Bauer
 */
public interface DeviceDetailsProvider {
    DeviceDetails provide(RemoteClientInfo info);
}
