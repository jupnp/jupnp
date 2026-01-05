/*
 * Copyright (C) 2011-2025 4th Line GmbH, Switzerland and others
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

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Unifies various HTTP header representations.
 * <p>
 * Snoracle, do you think you could add something like this to JDK 11g? Or did you kick out
 * the guy who knows how HTTP works so Larry can get his new jet filled up?
 * </p>
 * <p>
 * TODO: This is just a case-insensitive multi-valued map of strings...
 * </p>
 *
 * @author Christian Bauer
 */
public class Headers implements Map<String, List<String>> {

    private static final LinkedHashMap<String, String> NORMALIZED_KEYS = new LinkedHashMap<>(16, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<String, String> eldest) {
            return size() > 1024;
        }
    };

    static final byte CR = 13;
    static final byte LF = 10;

    final Map<String, List<String>> map = new HashMap<>(32);
    private boolean normalizeHeaders = true;

    public Headers() {
    }

    public Headers(Map<String, List<String>> map) {
        putAll(map);
    }

    public Headers(ByteArrayInputStream inputStream) {
        StringBuilder sb = new StringBuilder(256);
        String line = readLine(sb, inputStream);
        String lastHeader = null;
        if (!line.isEmpty()) {
            do {
                char firstChar = line.charAt(0);
                if (lastHeader != null && (firstChar == ' ' || firstChar == '\t')) {
                    List<String> current = this.get(lastHeader);
                    int lastPos = current.size() - 1;
                    String newString = current.get(lastPos) + line.trim();
                    current.set(lastPos, newString);
                } else {
                    lastHeader = splitHeader(line, this::add);
                }

                sb.delete(0, sb.length());
                line = readLine(sb, inputStream);
            } while (!line.isEmpty());
        }
    }

    public Headers(boolean normalizeHeaders) {
        this.normalizeHeaders = normalizeHeaders;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return key != null && key instanceof String && map.containsKey(normalize((String) key));
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public List<String> get(Object key) {
        return map.get(normalize((String) key));
    }

    @Override
    public List<String> put(String key, List<String> value) {
        return map.put(normalize(key), value);
    }

    @Override
    public List<String> remove(Object key) {
        return map.remove(normalize((String) key));
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> t) {
        // Enforce key normalization!
        for (Entry<? extends String, ? extends List<String>> entry : t.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<List<String>> values() {
        return map.values();
    }

    @Override
    public Set<Map.Entry<String, List<String>>> entrySet() {
        return map.entrySet();
    }

    public boolean equals(Object o) {
        return map.equals(o);
    }

    public int hashCode() {
        return map.hashCode();
    }

    public String getFirstHeader(String key) {
        List<String> l = map.get(normalize(key));
        return l != null && !l.isEmpty() ? l.get(0) : null;
    }

    public void add(String key, String value) {
        String k = normalize(key);
        List<String> l = map.computeIfAbsent(k, k1 -> new LinkedList<>());
        l.add(value);
    }

    public void set(String key, String value) {
        String k = normalize(key);
        LinkedList<String> l = new LinkedList<>();
        l.add(value);
        put(k, l);
    }

    private String normalize(String key) {
        String result = key;

        if (normalizeHeaders) {
            if (key == null) {
                return null;
            }
            synchronized (NORMALIZED_KEYS) {
                return NORMALIZED_KEYS.computeIfAbsent(key, (k) -> {
                    char[] b;
                    b = key.toCharArray();
                    final int caseDiff = 'a' - 'A';// android optimization

                    if (b[0] >= 'a' && b[0] <= 'z') {
                        b[0] = (char) (b[0] - caseDiff);
                    }
                    int length = b.length;// android optimization
                    for (int i = 1; i < length; i++) {
                        if (b[i] >= 'A' && b[i] <= 'Z') {
                            b[i] = (char) (b[i] + caseDiff);
                        }
                    }
                    return new String(b);
                });
            }
        }
        return result;
    }

    public static String readLine(ByteArrayInputStream is) {
        return readLine(new StringBuilder(256), is);
    }

    public static String readLine(StringBuilder sb, ByteArrayInputStream is) {
        int nextByte;
        while ((nextByte = is.read()) != -1) {
            char nextChar = (char) nextByte;
            if (nextChar == CR) {
                nextByte = (char) is.read();
                if (nextByte == LF) {
                    break;
                }
            } else if (nextChar == LF) {
                break;
            }

            sb.append(nextChar);
        }
        return sb.toString();
    }

    protected String splitHeader(String sb, BiConsumer<String, String> consumer) {
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;

        nameStart = findNonWhitespace(sb, 0);
        for (nameEnd = nameStart; nameEnd < sb.length(); nameEnd++) {
            char ch = sb.charAt(nameEnd);
            if (ch == ':' || Character.isWhitespace(ch)) {
                break;
            }
        }

        for (colonEnd = nameEnd; colonEnd < sb.length(); colonEnd++) {
            if (sb.charAt(colonEnd) == ':') {
                colonEnd++;
                break;
            }
        }

        valueStart = findNonWhitespace(sb, colonEnd);
        valueEnd = findEndOfString(sb);

        String name = sb.substring(nameStart, nameEnd);
        // This gets a bit messy because there are really HTTP headers without values (go figure...)
        consumer.accept(name,
                sb.length() >= valueStart && sb.length() >= valueEnd && valueStart < valueEnd
                        ? sb.substring(valueStart, valueEnd)
                        : null);
        return name;
    }

    protected int findNonWhitespace(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    protected int findEndOfString(String sb) {
        int result;
        for (result = sb.length(); result > 0; result--) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder headerString = new StringBuilder(512);
        for (Entry<String, List<String>> headerEntry : entrySet()) {

            headerString.append(headerEntry.getKey()).append(": ");

            for (String v : headerEntry.getValue()) {
                headerString.append(v).append(",");
            }
            headerString.delete(headerString.length() - 1, headerString.length());
            headerString.append("\r\n");
        }
        return headerString.toString();
    }
}
