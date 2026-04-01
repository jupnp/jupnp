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
package org.jupnp.support.model.item;

import static org.jupnp.support.model.DIDLObject.Property.DC;
import static org.jupnp.support.model.DIDLObject.Property.UPNP;

import java.util.List;

import org.jupnp.support.model.Person;
import org.jupnp.support.model.Res;
import org.jupnp.support.model.StorageMedium;
import org.jupnp.support.model.container.Container;

/**
 * @author Christian Bauer
 */
public class AudioBook extends AudioItem {

    public static final Class CLASS = new Class("object.item.audioItem.audioBook");

    public AudioBook() {
        setClazz(CLASS);
    }

    public AudioBook(Item other) {
        super(other);
    }

    public AudioBook(String id, Container parent, String title, String creator, Res... resource) {
        this(id, parent.getId(), title, creator, null, null, null, resource);
    }

    public AudioBook(String id, Container parent, String title, String creator, String producer, String contributor,
            String date, Res... resource) {
        this(id, parent.getId(), title, creator, new Person(producer), new Person(contributor), date, resource);
    }

    public AudioBook(String id, String parentID, String title, String creator, Person producer, Person contributor,
            String date, Res... resource) {
        super(id, parentID, title, creator, resource);
        setClazz(CLASS);
        if (producer != null) {
            addProperty(new UPNP.PRODUCER(producer));
        }
        if (contributor != null) {
            addProperty(new DC.CONTRIBUTOR(contributor));
        }
        if (date != null) {
            setDate(date);
        }
    }

    public StorageMedium getStorageMedium() {
        return getFirstPropertyValue(UPNP.STORAGE_MEDIUM.class);
    }

    public AudioBook setStorageMedium(StorageMedium storageMedium) {
        replaceFirstProperty(new UPNP.STORAGE_MEDIUM(storageMedium));
        return this;
    }

    public Person getFirstProducer() {
        return getFirstPropertyValue(UPNP.PRODUCER.class);
    }

    public Person[] getProducers() {
        List<Person> list = getPropertyValues(UPNP.PRODUCER.class);
        return list.toArray(new Person[list.size()]);
    }

    public AudioBook setProducers(Person[] persons) {
        removeProperties(UPNP.PRODUCER.class);
        for (Person p : persons) {
            addProperty(new UPNP.PRODUCER(p));
        }
        return this;
    }

    public Person getFirstContributor() {
        return getFirstPropertyValue(DC.CONTRIBUTOR.class);
    }

    public Person[] getContributors() {
        List<Person> list = getPropertyValues(DC.CONTRIBUTOR.class);
        return list.toArray(new Person[list.size()]);
    }

    public AudioBook setContributors(Person[] contributors) {
        removeProperties(DC.CONTRIBUTOR.class);
        for (Person p : contributors) {
            addProperty(new DC.CONTRIBUTOR(p));
        }
        return this;
    }

    public String getDate() {
        return getFirstPropertyValue(DC.DATE.class);
    }

    public AudioBook setDate(String date) {
        replaceFirstProperty(new DC.DATE(date));
        return this;
    }
}
