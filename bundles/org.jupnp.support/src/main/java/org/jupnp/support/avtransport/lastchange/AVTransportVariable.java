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
package org.jupnp.support.avtransport.lastchange;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jupnp.model.types.InvalidValueException;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.support.lastchange.EventedValue;
import org.jupnp.support.lastchange.EventedValueEnum;
import org.jupnp.support.lastchange.EventedValueEnumArray;
import org.jupnp.support.lastchange.EventedValueString;
import org.jupnp.support.lastchange.EventedValueURI;
import org.jupnp.support.lastchange.EventedValueUnsignedIntegerFourBytes;
import org.jupnp.support.model.PlayMode;
import org.jupnp.support.model.RecordQualityMode;
import org.jupnp.support.model.StorageMedium;
import org.jupnp.support.model.TransportAction;

/**
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */
public class AVTransportVariable {

    public static Set<Class<? extends EventedValue<?>>> ALL = new HashSet<>() {
        private static final long serialVersionUID = 4641676953130701810L;

        {
            add(TransportState.class);
            add(TransportStatus.class);
            add(RecordStorageMedium.class);
            add(PossibleRecordStorageMedia.class);
            add(PossiblePlaybackStorageMedia.class);
            add(CurrentPlayMode.class);
            add(TransportPlaySpeed.class);
            add(RecordMediumWriteStatus.class);
            add(CurrentRecordQualityMode.class);
            add(PossibleRecordQualityModes.class);
            add(NumberOfTracks.class);
            add(CurrentTrack.class);
            add(CurrentTrackDuration.class);
            add(CurrentMediaDuration.class);
            add(CurrentTrackMetaData.class);
            add(CurrentTrackURI.class);
            add(AVTransportURI.class);
            add(NextAVTransportURI.class);
            add(AVTransportURIMetaData.class);
            add(NextAVTransportURIMetaData.class);
            add(CurrentTransportActions.class);
            add(RelativeTimePosition.class);
            add(AbsoluteTimePosition.class);
            add(RelativeCounterPosition.class);
            add(AbsoluteCounterPosition.class);
        }
    };

    public static class TransportState extends EventedValueEnum<org.jupnp.support.model.TransportState> {
        public TransportState(org.jupnp.support.model.TransportState avTransportState) {
            super(avTransportState);
        }

        public TransportState(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected org.jupnp.support.model.TransportState enumValueOf(String s) {
            return org.jupnp.support.model.TransportState.valueOf(s);
        }
    }

    public static class TransportStatus extends EventedValueEnum<org.jupnp.support.model.TransportStatus> {
        public TransportStatus(org.jupnp.support.model.TransportStatus transportStatus) {
            super(transportStatus);
        }

        public TransportStatus(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected org.jupnp.support.model.TransportStatus enumValueOf(String s) {
            return org.jupnp.support.model.TransportStatus.valueOf(s);
        }
    }

    public static class RecordStorageMedium extends EventedValueEnum<StorageMedium> {

        public RecordStorageMedium(StorageMedium storageMedium) {
            super(storageMedium);
        }

        public RecordStorageMedium(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected StorageMedium enumValueOf(String s) {
            return StorageMedium.valueOrVendorSpecificOf(s);
        }
    }

    public static class PossibleRecordStorageMedia extends EventedValueEnumArray<StorageMedium> {
        public PossibleRecordStorageMedia(StorageMedium[] e) {
            super(e);
        }

        public PossibleRecordStorageMedia(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected StorageMedium[] enumValueOf(String[] names) {
            if (names == null) {
                return new StorageMedium[0];
            }
            List<StorageMedium> list = new ArrayList<>();
            for (String s : names) {
                list.add(StorageMedium.valueOrVendorSpecificOf(s));
            }
            return list.toArray(new StorageMedium[list.size()]);
        }
    }

    public static class PossiblePlaybackStorageMedia extends PossibleRecordStorageMedia {
        public PossiblePlaybackStorageMedia(StorageMedium[] e) {
            super(e);
        }

        public PossiblePlaybackStorageMedia(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class CurrentPlayMode extends EventedValueEnum<PlayMode> {
        public CurrentPlayMode(PlayMode playMode) {
            super(playMode);
        }

        public CurrentPlayMode(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected PlayMode enumValueOf(String s) {
            return PlayMode.valueOf(s);
        }
    }

    public static class TransportPlaySpeed extends EventedValueString {
        static final Pattern pattern = Pattern.compile("^-?\\d+(/\\d+)?$", Pattern.CASE_INSENSITIVE);

        public TransportPlaySpeed(String value) {
            super(value);
            if (!pattern.matcher(value).matches()) {
                throw new InvalidValueException("Can't parse TransportPlaySpeed speeds.");
            }
        }

        public TransportPlaySpeed(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class RecordMediumWriteStatus
            extends EventedValueEnum<org.jupnp.support.model.RecordMediumWriteStatus> {
        public RecordMediumWriteStatus(org.jupnp.support.model.RecordMediumWriteStatus recordMediumWriteStatus) {
            super(recordMediumWriteStatus);
        }

        public RecordMediumWriteStatus(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected org.jupnp.support.model.RecordMediumWriteStatus enumValueOf(String s) {
            return org.jupnp.support.model.RecordMediumWriteStatus.valueOf(s);
        }
    }

    public static class CurrentRecordQualityMode extends EventedValueEnum<RecordQualityMode> {
        public CurrentRecordQualityMode(RecordQualityMode recordQualityMode) {
            super(recordQualityMode);
        }

        public CurrentRecordQualityMode(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected RecordQualityMode enumValueOf(String s) {
            return RecordQualityMode.valueOf(s);
        }
    }

    public static class PossibleRecordQualityModes extends EventedValueEnumArray<RecordQualityMode> {
        public PossibleRecordQualityModes(RecordQualityMode[] e) {
            super(e);
        }

        public PossibleRecordQualityModes(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected RecordQualityMode[] enumValueOf(String[] names) {
            if (names == null) {
                return new RecordQualityMode[0];
            }
            List<RecordQualityMode> list = new ArrayList<>();
            for (String s : names) {
                list.add(RecordQualityMode.valueOf(s));
            }
            return list.toArray(new RecordQualityMode[list.size()]);
        }
    }

    public static class NumberOfTracks extends EventedValueUnsignedIntegerFourBytes {
        public NumberOfTracks(UnsignedIntegerFourBytes value) {
            super(value);
        }

        public NumberOfTracks(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class CurrentTrack extends EventedValueUnsignedIntegerFourBytes {
        public CurrentTrack(UnsignedIntegerFourBytes value) {
            super(value);
        }

        public CurrentTrack(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class CurrentTrackDuration extends EventedValueString {
        public CurrentTrackDuration(String value) {
            super(value);
        }

        public CurrentTrackDuration(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class CurrentMediaDuration extends EventedValueString {
        public CurrentMediaDuration(String value) {
            super(value);
        }

        public CurrentMediaDuration(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class CurrentTrackMetaData extends EventedValueString {
        public CurrentTrackMetaData(String value) {
            super(value);
        }

        public CurrentTrackMetaData(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class CurrentTrackURI extends EventedValueURI {
        public CurrentTrackURI(URI value) {
            super(value);
        }

        public CurrentTrackURI(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class AVTransportURI extends EventedValueURI {
        public AVTransportURI(URI value) {
            super(value);
        }

        public AVTransportURI(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class NextAVTransportURI extends EventedValueURI {
        public NextAVTransportURI(URI value) {
            super(value);
        }

        public NextAVTransportURI(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class AVTransportURIMetaData extends EventedValueString {
        public AVTransportURIMetaData(String value) {
            super(value);
        }

        public AVTransportURIMetaData(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class NextAVTransportURIMetaData extends EventedValueString {
        public NextAVTransportURIMetaData(String value) {
            super(value);
        }

        public NextAVTransportURIMetaData(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class CurrentTransportActions extends EventedValueEnumArray<TransportAction> {
        public CurrentTransportActions(TransportAction[] e) {
            super(e);
        }

        public CurrentTransportActions(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }

        @Override
        protected TransportAction[] enumValueOf(String[] names) {
            if (names == null) {
                return new TransportAction[0];
            }
            List<TransportAction> list = new ArrayList<>();
            for (String s : names) {
                list.add(TransportAction.valueOf(s));
            }
            return list.toArray(new TransportAction[list.size()]);
        }
    }

    public static class RelativeTimePosition extends EventedValueString {
        public RelativeTimePosition(String value) {
            super(value);
        }

        public RelativeTimePosition(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class AbsoluteTimePosition extends EventedValueString {
        public AbsoluteTimePosition(String value) {
            super(value);
        }

        public AbsoluteTimePosition(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class RelativeCounterPosition extends EventedValueString {
        public RelativeCounterPosition(String value) {
            super(value);
        }

        public RelativeCounterPosition(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }

    public static class AbsoluteCounterPosition extends EventedValueString {
        public AbsoluteCounterPosition(String value) {
            super(value);
        }

        public AbsoluteCounterPosition(Map.Entry<String, String>[] attributes) {
            super(attributes);
        }
    }
}
