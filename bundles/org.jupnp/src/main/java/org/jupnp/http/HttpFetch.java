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
package org.jupnp.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.jupnp.util.io.IO;

/**
 * @author Christian Bauer
 */
public class HttpFetch {

    public static Representation<byte[]> fetchBinary(URL url) throws IOException {
        return fetchBinary(url, 500, 500);
    }

    public static Representation<byte[]> fetchBinary(URL url, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {
        return fetch(url, connectTimeoutMillis, readTimeoutMillis,
                (urlConnection, is) -> new Representation<>(urlConnection, IO.readAllBytes(is)));
    }

    public static Representation<String> fetchString(URL url, int connectTimeoutMillis, int readTimeoutMillis)
            throws IOException {
        return fetch(url, connectTimeoutMillis, readTimeoutMillis,
                (urlConnection, is) -> new Representation<>(urlConnection, IO.readLines(is)));
    }

    public static <E> Representation<E> fetch(URL url, int connectTimeoutMillis, int readTimeoutMillis,
            RepresentationFactory<E> factory) throws IOException {
        return fetch(url, "GET", connectTimeoutMillis, readTimeoutMillis, factory);
    }

    public static <E> Representation<E> fetch(URL url, String method, int connectTimeoutMillis, int readTimeoutMillis,
            RepresentationFactory<E> factory) throws IOException {

        HttpURLConnection urlConnection = null;
        InputStream is = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod(method);

            urlConnection.setConnectTimeout(connectTimeoutMillis);
            urlConnection.setReadTimeout(readTimeoutMillis);

            is = urlConnection.getInputStream();

            return factory.createRepresentation(urlConnection, is);

            // Any response code above 400 is going to throw IOException (or FileNotFoundException subclass)
        } catch (IOException e) {
            if (urlConnection != null) {
                int responseCode = urlConnection.getResponseCode();
                InputStream errorStream = urlConnection.getErrorStream();
                if (errorStream != null) {
                    while (errorStream.read() != -1) {
                    }
                    errorStream.close();
                }
                throw new IOException("Fetching resource failed, returned status code: " + responseCode);
            }
            throw e;
        } finally {
            if (is != null) {
                is.close();
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public interface RepresentationFactory<E> {
        Representation<E> createRepresentation(URLConnection urlConnection, InputStream is) throws IOException;
    }

    public static void validate(URL url) throws IOException {
        fetch(url, "HEAD", 500, 500,
                (RepresentationFactory) (urlConnection, is) -> new Representation(urlConnection, null));
    }
}
