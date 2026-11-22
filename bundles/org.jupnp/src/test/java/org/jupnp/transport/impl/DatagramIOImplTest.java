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
package org.jupnp.transport.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class DatagramIOImplTest {

    @Test
    void keepsSubsequentDatagramsQueuedWhileOneSendIsWaiting() throws Exception {
        InetAddress loopback = InetAddress.getLoopbackAddress();
        CountDownLatch firstSend = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BlockingQueue<Integer> sent = new LinkedBlockingQueue<>();
        DatagramIOImpl io = new DatagramIOImpl(new DatagramIOConfigurationImpl()) {
            @Override
            void sendPacket(MulticastSocket socket, DatagramPacket packet) {
                int value = packet.getData()[packet.getOffset()];
                if (value == 1) {
                    firstSend.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                sent.add(value);
            }
        };
        io.init(loopback, 0, null, null);
        try {
            io.send(new DatagramPacket(new byte[] { 1 }, 1, loopback, 1900));
            assertTrue(firstSend.await(2, TimeUnit.SECONDS));

            byte[] reused = { 0, 2 };
            DatagramPacket next = new DatagramPacket(reused, 1, 1, loopback, 1900);
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> io.send(next));
            reused[1] = 3;
            release.countDown();

            assertEquals(1, sent.poll(2, TimeUnit.SECONDS));
            assertEquals(2, sent.poll(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            io.stop();
        }
    }
}
