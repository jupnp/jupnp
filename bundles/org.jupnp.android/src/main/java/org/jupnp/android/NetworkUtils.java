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
package org.jupnp.android;

import org.jupnp.model.ModelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Android network helpers.
 *
 * @author Michael Pujos
 */
public class NetworkUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtils.class);

    public static NetworkInfo getConnectedNetworkInfo(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
            return networkInfo;
        }

        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
            return networkInfo;
        }

        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
            return networkInfo;
        }

        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
            return networkInfo;
        }

        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
            return networkInfo;
        }

        LOGGER.info("Could not find any connected network...");

        return null;
    }

    public static boolean isEthernet(NetworkInfo networkInfo) {
        return isNetworkType(networkInfo, ConnectivityManager.TYPE_ETHERNET);
    }

    public static boolean isWifi(NetworkInfo networkInfo) {
        return isNetworkType(networkInfo, ConnectivityManager.TYPE_WIFI) || ModelUtil.ANDROID_EMULATOR;
    }

    public static boolean isMobile(NetworkInfo networkInfo) {
        return isNetworkType(networkInfo, ConnectivityManager.TYPE_MOBILE)
                || isNetworkType(networkInfo, ConnectivityManager.TYPE_WIMAX);
    }

    public static boolean isNetworkType(NetworkInfo networkInfo, int type) {
        return networkInfo != null && networkInfo.getType() == type;
    }

    public static boolean isSSDPAwareNetwork(NetworkInfo networkInfo) {
        return isWifi(networkInfo) || isEthernet(networkInfo);
    }
}
