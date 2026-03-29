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
package org.jupnp.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Christian Bauer
 */
public class Iterators {

    /**
     * A default implementation with no elements.
     */
    public static class Empty<E> implements Iterator<E> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A fixed single element.
     */
    public static class Singular<E> implements Iterator<E> {

        protected final E element;
        protected int current;

        public Singular(E element) {
            this.element = element;
        }

        @Override
        public boolean hasNext() {
            return current == 0;
        }

        @Override
        public E next() {
            current++;
            return element;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Wraps a collection and provides stable iteration with thread-safe removal.
     * <p>
     * Internally uses the iterator of a <code>CopyOnWriteArrayList</code>, when
     * <code>remove()</code> is called, delegates to {@link #synchronizedRemove(int)}.
     * </p>
     */
    public abstract static class Synchronized<E> implements Iterator<E> {

        final Iterator<E> wrapped;

        int nextIndex = 0;
        boolean removedCurrent = false;

        protected Synchronized(Collection<E> collection) {
            this.wrapped = new CopyOnWriteArrayList<>(collection).iterator();
        }

        @Override
        public boolean hasNext() {
            return wrapped.hasNext();
        }

        @Override
        public E next() {
            removedCurrent = false;
            nextIndex++;
            return wrapped.next();
        }

        @Override
        public void remove() {
            if (nextIndex == 0) {
                throw new IllegalStateException("Call next() first");
            }
            if (removedCurrent) {
                throw new IllegalStateException("Already removed current, call next()");
            }
            synchronizedRemove(nextIndex - 1);
            removedCurrent = true;
        }

        /**
         * Must remove the element at the given index from the original collection in a
         * thread-safe fashion.
         */
        protected abstract void synchronizedRemove(int index);
    }
}
