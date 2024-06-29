/*
 * Copyright (C) 2011-2024 4th Line GmbH, Switzerland and others
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
package org.jupnp.model.meta;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.jupnp.model.Validatable;
import org.jupnp.model.ValidationError;
import org.jupnp.model.types.BinHexDatatype;
import org.jupnp.util.MimeType;
import org.jupnp.util.SpecificationViolationReporter;
import org.jupnp.util.URIUtil;

/**
 * The metadata of a device icon, might include the actual image data of a local icon.
 *
 * <p>
 * Note that validation of icons is lax on purpose, a valid <code>Icon</code> might still
 * return <code>null</code> from {@link #getMimeType()}, {@link #getWidth()},
 * {@link #getHeight()}, and {@link #getDepth()}. However, {@link #getUri()} will return
 * a valid URI for a valid <code>Icon</code>.
 * </p>
 *
 * @author Christian Bauer
 * @author Jochen Hiller - use SpecificationViolationReporter
 */
public class Icon implements Validatable {

    private final MimeType mimeType;
    private final int width;
    private final int height;
    private final int depth;
    private final URI uri;
    private final byte[] data;

    // Package mutable state
    private Device device;

    /**
     * Used internally by jUPnP when {@link RemoteDevice} is discovered, you shouldn't have to call this.
     */
    public Icon(String mimeType, int width, int height, int depth, URI uri) {
        this(mimeType != null && !mimeType.isEmpty() ? MimeType.valueOf(mimeType) : null, width, height, depth, uri,
                null);
    }

    /**
     * Use this constructor if your local icon data can be resolved on the classpath, for
     * example: <code>MyClass.class.getResource("/my/icon.png)</code>
     *
     * @param url A URL of the icon data that can be read with <code>new File(url.toURI())</code>.
     */
    public Icon(String mimeType, int width, int height, int depth, URL url) throws IOException {
        this(mimeType, width, height, depth, new File(URIUtil.toURI(url)));
    }

    /**
     * Use this constructor if your local icon data can be resolved with a <code>File</code>, the file's
     * name must be unique within the scope of a device.
     */
    public Icon(String mimeType, int width, int height, int depth, File file) throws IOException {
        this(mimeType, width, height, depth, file.getName(), Files.readAllBytes(file.toPath()));
    }

    /**
     * Use this constructor if your local icon data is an <code>InputStream</code>.
     *
     * @param uniqueName Must be a valid URI path segment and unique within the scope of a device.
     */
    public Icon(String mimeType, int width, int height, int depth, String uniqueName, InputStream is)
            throws IOException {
        this(mimeType, width, height, depth, uniqueName, convert(is));
    }

    /**
     * Use this constructor if your local icon data is in a <code>byte[]</code>.
     *
     * @param uniqueName Must be a valid URI path segment and unique within the scope of a device.
     */
    public Icon(String mimeType, int width, int height, int depth, String uniqueName, byte[] data) {
        this(mimeType != null && !mimeType.isEmpty() ? MimeType.valueOf(mimeType) : null, width, height, depth,
                URI.create(uniqueName), data);
    }

    /**
     * Use this constructor if your local icon is binary data encoded with <em>BinHex</em>.
     *
     * @param uniqueName Must be a valid URI path segment and unique within the scope of a device.
     * @param binHexEncoded The icon bytes encoded as BinHex.
     */
    public Icon(String mimeType, int width, int height, int depth, String uniqueName, String binHexEncoded) {
        this(mimeType, width, height, depth, uniqueName,
                binHexEncoded != null && !binHexEncoded.isEmpty() ? new BinHexDatatype().valueOf(binHexEncoded) : null);
    }

    protected Icon(MimeType mimeType, int width, int height, int depth, URI uri, byte[] data) {
        this.mimeType = mimeType;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.uri = uri;
        this.data = data;
    }

    public MimeType getMimeType() {
        return mimeType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public URI getUri() {
        return uri;
    }

    public byte[] getData() {
        return data;
    }

    public Device getDevice() {
        return device;
    }

    void setDevice(Device device) {
        if (this.device != null) {
            throw new IllegalStateException("Final value has been set already, model is immutable");
        }
        this.device = device;
    }

    /**
     * Converts the given InputStream into a byte array. This method should be replaced by
     * java.io.InputStream#readAllBytes when Android 13 (API Level 33) is more widely used.
     *
     * @param inputStream the InputStream to be converted
     * @return a byte array containing the data from the InputStream
     * @throws IOException if an I/O error occurs
     */
    public static byte[] convert(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];

        // Read data from InputStream in chunks of 1024 bytes
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            // Write the read data into ByteArrayOutputStream
            buffer.write(data, 0, nRead);
        }

        // Return the complete byte array
        return buffer.toByteArray();
    }

    @Override
    public List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        if (getMimeType() == null) {
            SpecificationViolationReporter.report(getDevice(), "Invalid icon, missing mime type: {}", this);
        }
        if (getWidth() == 0) {
            SpecificationViolationReporter.report(getDevice(), "Invalid icon, missing width: {}", this);
        }
        if (getHeight() == 0) {
            SpecificationViolationReporter.report(getDevice(), "Invalid icon, missing height: {}", this);
        }
        if (getDepth() == 0) {
            SpecificationViolationReporter.report(getDevice(), "Invalid icon, missing bitmap depth: {}", this);
        }

        if (getUri() == null) {
            errors.add(new ValidationError(getClass(), "uri", "URL is required"));
        } else {
            try {
                URL testURI = getUri().toURL();
                if (testURI == null) {
                    throw new MalformedURLException();
                }
            } catch (MalformedURLException e) {
                errors.add(new ValidationError(getClass(), "uri", "URL must be valid: " + e.getMessage()));
            } catch (IllegalArgumentException e) {
                // Relative URI is fine here!
            }
        }

        return errors;
    }

    public Icon deepCopy() {
        return new Icon(getMimeType(), getWidth(), getHeight(), getDepth(), getUri(), getData());
    }

    @Override
    public String toString() {
        return "Icon(" + getWidth() + "x" + getHeight() + ", MIME: " + getMimeType() + ") " + getUri();
    }
}
