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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christian Bauer
 */
public class CacheControl {

    private int maxAge = -1;
    private int sharedMaxAge = -1;

    private boolean noCache = false;
    private List<String> noCacheFields = new ArrayList<>();

    private boolean privateFlag = false;
    private List<String> privateFields = new ArrayList<>();

    private boolean noStore = false;
    private boolean noTransform = true;
    private boolean mustRevalidate = false;
    private boolean proxyRevalidate = false;

    private Map<String, String> cacheExtensions = new HashMap<>();

    public int getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    public int getSharedMaxAge() {
        return sharedMaxAge;
    }

    public void setSharedMaxAge(int sharedMaxAge) {
        this.sharedMaxAge = sharedMaxAge;
    }

    public boolean isNoCache() {
        return noCache;
    }

    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    public List<String> getNoCacheFields() {
        return noCacheFields;
    }

    public void setNoCacheFields(List<String> noCacheFields) {
        this.noCacheFields = noCacheFields;
    }

    public boolean isPrivateFlag() {
        return privateFlag;
    }

    public void setPrivateFlag(boolean privateFlag) {
        this.privateFlag = privateFlag;
    }

    public List<String> getPrivateFields() {
        return privateFields;
    }

    public void setPrivateFields(List<String> privateFields) {
        this.privateFields = privateFields;
    }

    public boolean isNoStore() {
        return noStore;
    }

    public void setNoStore(boolean noStore) {
        this.noStore = noStore;
    }

    public boolean isNoTransform() {
        return noTransform;
    }

    public void setNoTransform(boolean noTransform) {
        this.noTransform = noTransform;
    }

    public boolean isMustRevalidate() {
        return mustRevalidate;
    }

    public void setMustRevalidate(boolean mustRevalidate) {
        this.mustRevalidate = mustRevalidate;
    }

    public boolean isProxyRevalidate() {
        return proxyRevalidate;
    }

    public void setProxyRevalidate(boolean proxyRevalidate) {
        this.proxyRevalidate = proxyRevalidate;
    }

    public Map<String, String> getCacheExtensions() {
        return cacheExtensions;
    }

    public void setCacheExtensions(Map<String, String> cacheExtensions) {
        this.cacheExtensions = cacheExtensions;
    }

    public static CacheControl valueOf(String s) throws IllegalArgumentException {
        if (s == null) {
            return null;
        }
        CacheControl result = new CacheControl();

        String[] directives = s.split(",");
        for (String directive : directives) {
            directive = directive.trim();

            String[] nameValue = directive.split("=");
            String name = nameValue[0].trim();
            String value = null;
            if (nameValue.length > 1) {
                value = nameValue[1].trim();
                if (value.startsWith("\"")) {
                    value = value.substring(1);
                }
                if (value.endsWith("\"")) {
                    value = value.substring(0, value.length() - 1);
                }
            }

            String lowercase = name.toLowerCase();
            if ("no-cache".equals(lowercase)) {
                result.setNoCache(true);
                if (value != null && !value.isEmpty()) {
                    result.getNoCacheFields().add(value);
                }
            } else if ("private".equals(lowercase)) {
                result.setPrivateFlag(true);
                if (value != null && !value.isEmpty()) {
                    result.getPrivateFields().add(value);
                }
            } else if ("no-store".equals(lowercase)) {
                result.setNoStore(true);
            } else if ("max-age".equals(lowercase)) {
                if (value == null) {
                    throw new IllegalArgumentException("CacheControl max-age header does not have a value: " + value);
                }
                result.setMaxAge(Integer.parseInt(value));
            } else if ("s-maxage".equals(lowercase)) {
                if (value == null) {
                    throw new IllegalArgumentException("CacheControl s-maxage header does not have a value: " + value);
                }
                result.setSharedMaxAge(Integer.parseInt(value));
            } else if ("no-transform".equals(lowercase)) {
                result.setNoTransform(true);
            } else if ("must-revalidate".equals(lowercase)) {
                result.setMustRevalidate(true);
            } else if ("proxy-revalidate".equals(lowercase)) {
                result.setProxyRevalidate(true);
            } else if ("public".equals(lowercase)) {
                // ignore
            } else {
                if (value == null) {
                    value = "";
                }
                result.getCacheExtensions().put(name, value);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isPrivateFlag()) {
            sb.append("public");
        }
        if (isMustRevalidate()) {
            append("must-revalidate", sb);
        }
        if (isNoTransform()) {
            append("no-transform", sb);
        }
        if (isNoStore()) {
            append("no-store", sb);
        }
        if (isProxyRevalidate()) {
            append("proxy-revalidate", sb);
        }
        if (getSharedMaxAge() > -1) {
            append("s-maxage", sb).append("=").append(getSharedMaxAge());
        }
        if (getMaxAge() > -1) {
            append("max-age", sb).append("=").append(getMaxAge());
        }
        if (isNoCache()) {
            List<String> fields = getNoCacheFields();
            if (fields.isEmpty()) {
                append("no-cache", sb);
            } else {
                for (String field : getNoCacheFields()) {
                    append("no-cache", sb).append("=\"").append(field).append("\"");
                }
            }
        }
        if (isPrivateFlag()) {
            List<String> fields = getPrivateFields();
            if (fields.isEmpty()) {
                append("private", sb);
            } else {
                for (String field : getPrivateFields()) {
                    append("private", sb).append("=\"").append(field).append("\"");
                }
            }
        }
        for (String key : getCacheExtensions().keySet()) {
            String val = getCacheExtensions().get(key);
            append(key, sb);
            if (val != null && !val.isEmpty()) {
                sb.append("=\"").append(val).append("\"");
            }
        }
        return sb.toString();
    }

    private StringBuilder append(String s, StringBuilder sb) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(s);
        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CacheControl that = (CacheControl) o;

        if (maxAge != that.maxAge) {
            return false;
        }
        if (mustRevalidate != that.mustRevalidate) {
            return false;
        }
        if (noCache != that.noCache) {
            return false;
        }
        if (noStore != that.noStore) {
            return false;
        }
        if (noTransform != that.noTransform) {
            return false;
        }
        if (privateFlag != that.privateFlag) {
            return false;
        }
        if (proxyRevalidate != that.proxyRevalidate) {
            return false;
        }
        if (sharedMaxAge != that.sharedMaxAge) {
            return false;
        }
        if (!cacheExtensions.equals(that.cacheExtensions)) {
            return false;
        }
        if (!noCacheFields.equals(that.noCacheFields)) {
            return false;
        }
        if (!privateFields.equals(that.privateFields)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = maxAge;
        result = 31 * result + sharedMaxAge;
        result = 31 * result + (noCache ? 1 : 0);
        result = 31 * result + noCacheFields.hashCode();
        result = 31 * result + (privateFlag ? 1 : 0);
        result = 31 * result + privateFields.hashCode();
        result = 31 * result + (noStore ? 1 : 0);
        result = 31 * result + (noTransform ? 1 : 0);
        result = 31 * result + (mustRevalidate ? 1 : 0);
        result = 31 * result + (proxyRevalidate ? 1 : 0);
        result = 31 * result + cacheExtensions.hashCode();
        return result;
    }
}
