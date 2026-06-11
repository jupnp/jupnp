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
package org.jupnp.transport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.jupnp.util.Iterators;

/**
 * @author Holger Friedrich - initial contribution
 */
class SynchronizedIteratorTest {

    @Test
    void testSynchronizedIteratorRemoveAll() {
        List<InetAddress> bindAddresses = new ArrayList<>();
        bindAddresses.add(InetAddress.getLoopbackAddress());
        bindAddresses.add(InetAddress.getLoopbackAddress());

        var synced = new Iterators.Synchronized<>(bindAddresses) {
            @Override
            protected void synchronizedRemove(int index) {
                synchronized (bindAddresses) {
                    bindAddresses.remove(index);
                }
            }
        };

        assertTrue(synced.hasNext());
        synced.next();
        synced.remove();

        assertTrue(synced.hasNext());
        synced.next();
        synced.remove();

        assertFalse(synced.hasNext());
        assertTrue(bindAddresses.isEmpty());
    }
}
