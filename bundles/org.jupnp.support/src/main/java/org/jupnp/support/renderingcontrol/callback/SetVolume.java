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
package org.jupnp.support.renderingcontrol.callback;

import org.jupnp.controlpoint.ActionCallback;
import org.jupnp.model.action.ActionInvocation;
import org.jupnp.model.meta.Service;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.UnsignedIntegerTwoBytes;
import org.jupnp.support.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christian Bauer - Initial Contribution
 * @author Amit Kumar Mondal - Code Refactoring
 */
public abstract class SetVolume extends ActionCallback {

    private final Logger logger = LoggerFactory.getLogger(SetVolume.class);

    protected SetVolume(Service<?, ?> service, long newVolume) {
        this(new UnsignedIntegerFourBytes(0), service, newVolume);
    }

    protected SetVolume(UnsignedIntegerFourBytes instanceId, Service<?, ?> service, long newVolume) {
        super(new ActionInvocation<>(service.getAction("SetVolume")));
        getActionInvocation().setInput("InstanceID", instanceId);
        getActionInvocation().setInput("Channel", Channel.Master.toString());
        getActionInvocation().setInput("DesiredVolume", new UnsignedIntegerTwoBytes(newVolume));
    }

    @Override
    public void success(ActionInvocation invocation) {
        logger.debug("Executed successfully");
    }
}
