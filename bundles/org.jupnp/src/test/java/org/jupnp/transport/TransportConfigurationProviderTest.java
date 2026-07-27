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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;
import org.jupnp.transport.spi.StreamServerConfiguration;

/**
 * @author Holger Friedrich - initial contribution
 */
class TransportConfigurationProviderTest {

    @TempDir
    Path tempDir;

    private ClassLoader previousContextClassLoader;

    @BeforeEach
    void rememberContextClassLoader() {
        previousContextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    @AfterEach
    void restoreContextClassLoaderAndInterruptFlag() {
        Thread.currentThread().setContextClassLoader(previousContextClassLoader);
        // Clear the flag in case a test left it set, so it doesn't leak into other tests.
        Thread.interrupted();
    }

    @Test
    void multipleProvidersFailFast() throws IOException {
        Thread.currentThread().setContextClassLoader(isolatedClassLoaderWithProviders(
                FirstTestTransportConfiguration.class, SecondTestTransportConfiguration.class));

        InitializationException exception = assertThrows(InitializationException.class,
                TransportConfigurationProvider::getDefaultTransportConfiguration);

        assertTrue(exception.getMessage().contains(FirstTestTransportConfiguration.class.getName()));
        assertTrue(exception.getMessage().contains(SecondTestTransportConfiguration.class.getName()));
    }

    @Test
    void interruptedRetryWaitPreservesInterruptedExceptionAsCause() {
        // No transport bundle is on this module's test class path, so the first attempt already finds
        // nothing and the loop proceeds to its retry delay -- where the pre-set interrupt flag below
        // makes Thread.sleep() fail immediately, deterministically, without needing a second thread.
        Thread.currentThread().interrupt();

        InitializationException exception = assertThrows(InitializationException.class,
                TransportConfigurationProvider::getDefaultTransportConfiguration);

        assertInstanceOf(InterruptedException.class, exception.getCause());
    }

    private ClassLoader isolatedClassLoaderWithProviders(Class<?>... providers) throws IOException {
        Path servicesDir = tempDir.resolve("META-INF/services");
        Files.createDirectories(servicesDir);
        StringBuilder content = new StringBuilder();
        for (Class<?> provider : providers) {
            content.append(provider.getName()).append('\n');
        }
        Files.writeString(servicesDir.resolve(TransportConfiguration.class.getName()), content.toString(),
                StandardCharsets.UTF_8);
        return new URLClassLoader(new URL[] { tempDir.toUri().toURL() }, previousContextClassLoader);
    }

    public static final class FirstTestTransportConfiguration
            implements TransportConfiguration<StreamClientConfiguration, StreamServerConfiguration> {

        @Override
        public StreamClient<StreamClientConfiguration> createStreamClient(ExecutorService executorService,
                StreamClientConfiguration configuration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StreamServer<StreamServerConfiguration> createStreamServer(int listenerPort) {
            throw new UnsupportedOperationException();
        }
    }

    public static final class SecondTestTransportConfiguration
            implements TransportConfiguration<StreamClientConfiguration, StreamServerConfiguration> {

        @Override
        public StreamClient<StreamClientConfiguration> createStreamClient(ExecutorService executorService,
                StreamClientConfiguration configuration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public StreamServer<StreamServerConfiguration> createStreamServer(int listenerPort) {
            throw new UnsupportedOperationException();
        }
    }
}
