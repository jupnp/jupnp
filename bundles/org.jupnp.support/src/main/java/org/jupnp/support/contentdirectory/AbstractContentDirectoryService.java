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
package org.jupnp.support.contentdirectory;

import java.util.ArrayList;
import java.util.List;

import org.jupnp.binding.annotations.UpnpAction;
import org.jupnp.binding.annotations.UpnpInputArgument;
import org.jupnp.binding.annotations.UpnpOutputArgument;
import org.jupnp.binding.annotations.UpnpService;
import org.jupnp.binding.annotations.UpnpServiceId;
import org.jupnp.binding.annotations.UpnpServiceType;
import org.jupnp.binding.annotations.UpnpStateVariable;
import org.jupnp.binding.annotations.UpnpStateVariables;
import org.jupnp.internal.compat.java.beans.PropertyChangeSupport;
import org.jupnp.model.types.ErrorCode;
import org.jupnp.model.types.UnsignedIntegerFourBytes;
import org.jupnp.model.types.csv.CSV;
import org.jupnp.model.types.csv.CSVString;
import org.jupnp.support.model.BrowseFlag;
import org.jupnp.support.model.BrowseResult;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.SortCriterion;

/**
 * Simple ContentDirectory service skeleton.
 * <p>
 * Only state variables and actions required by <em>ContentDirectory:1</em>
 * (not the optional ones) are implemented.
 * </p>
 *
 * @author Alessio Gaeta
 * @author Christian Bauer
 * @author Amit Kumar Mondal - Code Refactoring
 */

@UpnpService(serviceId = @UpnpServiceId("ContentDirectory"), serviceType = @UpnpServiceType(value = "ContentDirectory", version = 1))

@UpnpStateVariables({ @UpnpStateVariable(name = "A_ARG_TYPE_ObjectID", sendEvents = false, datatype = "string"),
        @UpnpStateVariable(name = "A_ARG_TYPE_Result", sendEvents = false, datatype = "string"),
        @UpnpStateVariable(name = "A_ARG_TYPE_BrowseFlag", sendEvents = false, datatype = "string", allowedValuesEnum = BrowseFlag.class),
        @UpnpStateVariable(name = "A_ARG_TYPE_Filter", sendEvents = false, datatype = "string"),
        @UpnpStateVariable(name = "A_ARG_TYPE_SortCriteria", sendEvents = false, datatype = "string"),
        @UpnpStateVariable(name = "A_ARG_TYPE_Index", sendEvents = false, datatype = "ui4"),
        @UpnpStateVariable(name = "A_ARG_TYPE_Count", sendEvents = false, datatype = "ui4"),
        @UpnpStateVariable(name = "A_ARG_TYPE_UpdateID", sendEvents = false, datatype = "ui4"),
        @UpnpStateVariable(name = "A_ARG_TYPE_URI", sendEvents = false, datatype = "uri"),
        @UpnpStateVariable(name = "A_ARG_TYPE_SearchCriteria", sendEvents = false, datatype = "string") })
public abstract class AbstractContentDirectoryService {

    public static final String CAPS_WILDCARD = "*";

    @UpnpStateVariable(sendEvents = false)
    private final CSV<String> searchCapabilities;

    @UpnpStateVariable(sendEvents = false)
    private final CSV<String> sortCapabilities;

    @UpnpStateVariable(sendEvents = true, defaultValue = "0", eventMaximumRateMilliseconds = 200)
    private UnsignedIntegerFourBytes systemUpdateID = new UnsignedIntegerFourBytes(0);

    protected final PropertyChangeSupport propertyChangeSupport;

    protected AbstractContentDirectoryService() {
        this(new ArrayList<>(), new ArrayList<>(), null);
    }

    protected AbstractContentDirectoryService(List<String> searchCapabilities, List<String> sortCapabilities) {
        this(searchCapabilities, sortCapabilities, null);
    }

    protected AbstractContentDirectoryService(List<String> searchCapabilities, List<String> sortCapabilities,
            PropertyChangeSupport propertyChangeSupport) {
        this.propertyChangeSupport = propertyChangeSupport != null ? propertyChangeSupport
                : new PropertyChangeSupport(this);
        this.searchCapabilities = new CSVString();
        this.searchCapabilities.addAll(searchCapabilities);
        this.sortCapabilities = new CSVString();
        this.sortCapabilities.addAll(sortCapabilities);
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "SearchCaps"))
    public CSV<String> getSearchCapabilities() {
        return searchCapabilities;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "SortCaps"))
    public CSV<String> getSortCapabilities() {
        return sortCapabilities;
    }

    @UpnpAction(out = @UpnpOutputArgument(name = "Id"))
    public synchronized UnsignedIntegerFourBytes getSystemUpdateID() {
        return systemUpdateID;
    }

    public PropertyChangeSupport getPropertyChangeSupport() {
        return propertyChangeSupport;
    }

    /**
     * Call this method after making changes to your content directory.
     * <p>
     * This will notify clients that their view of the content directory is potentially
     * outdated and has to be refreshed.
     * </p>
     */
    protected synchronized void changeSystemUpdateID() {
        Long oldUpdateID = getSystemUpdateID().getValue();
        systemUpdateID.increment(true);
        getPropertyChangeSupport().firePropertyChange("SystemUpdateID", oldUpdateID, getSystemUpdateID().getValue());
    }

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "Result", stateVariable = "A_ARG_TYPE_Result", getterName = "getResult"),
            @UpnpOutputArgument(name = "NumberReturned", stateVariable = "A_ARG_TYPE_Count", getterName = "getCount"),
            @UpnpOutputArgument(name = "TotalMatches", stateVariable = "A_ARG_TYPE_Count", getterName = "getTotalMatches"),
            @UpnpOutputArgument(name = "UpdateID", stateVariable = "A_ARG_TYPE_UpdateID", getterName = "getContainerUpdateID") })
    public BrowseResult browse(@UpnpInputArgument(name = "ObjectID", aliases = "ContainerID") String objectId,
            @UpnpInputArgument(name = "BrowseFlag") String browseFlag,
            @UpnpInputArgument(name = "Filter") String filter,
            @UpnpInputArgument(name = "StartingIndex", stateVariable = "A_ARG_TYPE_Index") UnsignedIntegerFourBytes firstResult,
            @UpnpInputArgument(name = "RequestedCount", stateVariable = "A_ARG_TYPE_Count") UnsignedIntegerFourBytes maxResults,
            @UpnpInputArgument(name = "SortCriteria") String orderBy) throws ContentDirectoryException {

        SortCriterion[] orderByCriteria;
        try {
            orderByCriteria = SortCriterion.valueOf(orderBy);
        } catch (Exception e) {
            throw new ContentDirectoryException(ContentDirectoryErrorCode.UNSUPPORTED_SORT_CRITERIA, e.toString());
        }

        try {
            return browse(objectId, BrowseFlag.valueOrNullOf(browseFlag), filter, firstResult.getValue(),
                    maxResults.getValue(), orderByCriteria);
        } catch (ContentDirectoryException e) {
            throw e;
        } catch (Exception e) {
            throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, e.toString());
        }
    }

    /**
     * Implement this method to implement browsing of your content.
     * <p>
     * This is a required action defined by <em>ContentDirectory:1</em>.
     * </p>
     * <p>
     * You should wrap any exception into a {@link ContentDirectoryException}, so a propery
     * error message can be returned to control points.
     * </p>
     */
    public abstract BrowseResult browse(String objectID, BrowseFlag browseFlag, String filter, long firstResult,
            long maxResults, SortCriterion[] orderby) throws ContentDirectoryException;

    @UpnpAction(out = {
            @UpnpOutputArgument(name = "Result", stateVariable = "A_ARG_TYPE_Result", getterName = "getResult"),
            @UpnpOutputArgument(name = "NumberReturned", stateVariable = "A_ARG_TYPE_Count", getterName = "getCount"),
            @UpnpOutputArgument(name = "TotalMatches", stateVariable = "A_ARG_TYPE_Count", getterName = "getTotalMatches"),
            @UpnpOutputArgument(name = "UpdateID", stateVariable = "A_ARG_TYPE_UpdateID", getterName = "getContainerUpdateID") })
    public BrowseResult search(
            @UpnpInputArgument(name = "ContainerID", stateVariable = "A_ARG_TYPE_ObjectID") String containerId,
            @UpnpInputArgument(name = "SearchCriteria") String searchCriteria,
            @UpnpInputArgument(name = "Filter") String filter,
            @UpnpInputArgument(name = "StartingIndex", stateVariable = "A_ARG_TYPE_Index") UnsignedIntegerFourBytes firstResult,
            @UpnpInputArgument(name = "RequestedCount", stateVariable = "A_ARG_TYPE_Count") UnsignedIntegerFourBytes maxResults,
            @UpnpInputArgument(name = "SortCriteria") String orderBy) throws ContentDirectoryException {

        SortCriterion[] orderByCriteria;
        try {
            orderByCriteria = SortCriterion.valueOf(orderBy);
        } catch (Exception e) {
            throw new ContentDirectoryException(ContentDirectoryErrorCode.UNSUPPORTED_SORT_CRITERIA, e.toString());
        }

        try {
            return search(containerId, searchCriteria, filter, firstResult.getValue(), maxResults.getValue(),
                    orderByCriteria);
        } catch (ContentDirectoryException e) {
            throw e;
        } catch (Exception e) {
            throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, e.toString());
        }
    }

    /**
     * Override this method to implement searching of your content.
     * <p>
     * The default implementation returns an empty result.
     * </p>
     */
    public BrowseResult search(String containerId, String searchCriteria, String filter, long firstResult,
            long maxResults, SortCriterion[] orderBy) throws ContentDirectoryException {

        try {
            return new BrowseResult(new DIDLParser().generate(new DIDLContent()), 0, 0);
        } catch (Exception e) {
            throw new ContentDirectoryException(ErrorCode.ACTION_FAILED, e.toString());
        }
    }
}
