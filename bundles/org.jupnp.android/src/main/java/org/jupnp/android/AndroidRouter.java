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

import org.jupnp.UpnpServiceConfiguration;
import org.jupnp.model.ModelUtil;
import org.jupnp.protocol.ProtocolFactory;
import org.jupnp.transport.RouterException;
import org.jupnp.transport.RouterImpl;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * Monitors all network connectivity changes, switching the router accordingly.
 *
 * @author Michael Pujos
 * @author Christian Bauer
 */
public class AndroidRouter extends RouterImpl {

    private final Logger logger = LoggerFactory.getLogger(AndroidRouter.class);

    private final Context context;

    private final WifiManager wifiManager;
    protected WifiManager.MulticastLock multicastLock;
    protected WifiManager.WifiLock wifiLock;
    protected NetworkInfo networkInfo;
    protected BroadcastReceiver broadcastReceiver;

    public AndroidRouter(UpnpServiceConfiguration configuration, ProtocolFactory protocolFactory, Context context)
            throws InitializationException {
        super(configuration, protocolFactory);

        this.context = context;
        this.wifiManager = ((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
        this.networkInfo = NetworkUtils.getConnectedNetworkInfo(context);

        // Only register for network connectivity changes if we are not running on emulator
        if (!ModelUtil.ANDROID_EMULATOR) {
            this.broadcastReceiver = createConnectivityBroadcastReceiver();
            context.registerReceiver(broadcastReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        }
    }

    protected BroadcastReceiver createConnectivityBroadcastReceiver() {
        return new ConnectivityBroadcastReceiver();
    }

    @Override
    protected int getLockTimeoutMillis() {
        return 15000;
    }

    @Override
    public void shutdown() throws RouterException {
        super.shutdown();
        unregisterBroadcastReceiver();
    }

    @Override
    public boolean enable() throws RouterException {
        lock(writeLock);
        try {
            boolean enabled = super.enable();
            // Enable multicast on the WiFi network interface, requires android.permission.CHANGE_WIFI_MULTICAST_STATE
            if (enabled && isWifi()) {
                setWiFiMulticastLock(true);
                setWifiLock(true);
            }
            return enabled;
        } finally {
            unlock(writeLock);
        }
    }

    @Override
    public boolean disable() throws RouterException {
        lock(writeLock);
        try {
            // Disable multicast on WiFi network interface, requires android.permission.CHANGE_WIFI_MULTICAST_STATE
            if (isWifi()) {
                setWiFiMulticastLock(false);
                setWifiLock(false);
            }
            return super.disable();
        } finally {
            unlock(writeLock);
        }
    }

    public NetworkInfo getNetworkInfo() {
        return networkInfo;
    }

    public boolean isMobile() {
        return NetworkUtils.isMobile(networkInfo);
    }

    public boolean isWifi() {
        return NetworkUtils.isWifi(networkInfo);
    }

    public boolean isEthernet() {
        return NetworkUtils.isEthernet(networkInfo);
    }

    public boolean enableWiFi() {
        logger.info("Enabling WiFi...");
        try {
            return wifiManager.setWifiEnabled(true);
        } catch (Exception e) {
            // workaround (HTC One X, 4.0.3)
            // java.lang.SecurityException: Permission Denial: writing com.android.providers.settings.SettingsProvider
            // uri content://settings/system from pid=4691, uid=10226 requires android.permission.WRITE_SETTINGS
            // at android.os.Parcel.readException(Parcel.java:1332)
            // at android.os.Parcel.readException(Parcel.java:1286)
            // at android.net.wifi.IWifiManager$Stub$Proxy.setWifiEnabled(IWifiManager.java:1115)
            // at android.net.wifi.WifiManager.setWifiEnabled(WifiManager.java:946)
            logger.warn("SetWifiEnabled failed", e);
            return false;
        }
    }

    public void unregisterBroadcastReceiver() {
        if (broadcastReceiver != null) {
            context.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }
    }

    protected void setWiFiMulticastLock(boolean enable) {
        if (multicastLock == null) {
            multicastLock = wifiManager.createMulticastLock(getClass().getSimpleName());
        }

        if (enable) {
            if (multicastLock.isHeld()) {
                logger.warn("WiFi multicast lock already acquired");
            } else {
                logger.info("WiFi multicast lock acquired");
                multicastLock.acquire();
            }
        } else {
            if (multicastLock.isHeld()) {
                logger.info("WiFi multicast lock released");
                multicastLock.release();
            } else {
                logger.warn("WiFi multicast lock already released");
            }
        }
    }

    protected void setWifiLock(boolean enable) {
        if (wifiLock == null) {
            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, getClass().getSimpleName());
        }

        if (enable) {
            if (wifiLock.isHeld()) {
                logger.warn("WiFi lock already acquired");
            } else {
                logger.info("WiFi lock acquired");
                wifiLock.acquire();
            }
        } else {
            if (wifiLock.isHeld()) {
                logger.info("WiFi lock released");
                wifiLock.release();
            } else {
                logger.warn("WiFi lock already released");
            }
        }
    }

    /**
     * Can be overridden by subclasses to do additional work.
     *
     * @param oldNetwork <code>null</code> when first called by constructor.
     */
    protected void onNetworkTypeChange(NetworkInfo oldNetwork, NetworkInfo newNetwork) throws RouterException {
        logger.info("Network type changed {} => {}", oldNetwork == null ? "" : oldNetwork.getTypeName(),
                newNetwork == null ? "NONE" : newNetwork.getTypeName());

        if (disable()) {
            logger.info("Disabled router on network type change (old network: {})",
                    oldNetwork == null ? "NONE" : oldNetwork.getTypeName());
        }

        networkInfo = newNetwork;
        if (enable()) {
            // Can return false (via earlier InitializationException thrown by NetworkAddressFactory) if
            // no bindable network address found!
            logger.info("Enabled router on network type change (new network: {})",
                    newNetwork == null ? "NONE" : newNetwork.getTypeName());
        }
    }

    /**
     * Handles errors when network has been switched, during reception of
     * network switch broadcast. Logs a warning by default, override to
     * change this behavior.
     */
    protected void handleRouterExceptionOnNetworkTypeChange(RouterException e) {
        Throwable cause = Exceptions.unwrap(e);
        if (cause instanceof InterruptedException) {
            logger.info("Router was interrupted", e);
        } else {
            logger.warn("Router error on network change", e);
        }
    }

    class ConnectivityBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                return;
            }

            displayIntentInfo(intent);

            NetworkInfo newNetworkInfo = NetworkUtils.getConnectedNetworkInfo(context);

            // When Android switches WiFI => MOBILE, sometimes we may have a short transition
            // with no network: WIFI => NONE, NONE => MOBILE
            // The code below attempts to make it look like a single WIFI => MOBILE
            // transition, retrying up to 3 times getting the current network.
            //
            // Note: this can block the UI thread for up to 3s
            if (networkInfo != null && newNetworkInfo == null) {
                for (int i = 1; i <= 3; i++) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        return;
                    }
                    logger.warn("{} => NONE network transition, waiting for new network... retry #{}",
                            networkInfo.getTypeName(), i);
                    newNetworkInfo = NetworkUtils.getConnectedNetworkInfo(context);
                    if (newNetworkInfo != null) {
                        break;
                    }
                }
            }

            if (isSameNetworkType(networkInfo, newNetworkInfo)) {
                logger.info("No actual network change... ignoring event!");
            } else {
                try {
                    onNetworkTypeChange(networkInfo, newNetworkInfo);
                } catch (RouterException e) {
                    handleRouterExceptionOnNetworkTypeChange(e);
                }
            }
        }

        protected boolean isSameNetworkType(NetworkInfo network1, NetworkInfo network2) {
            if (network1 == null && network2 == null) {
                return true;
            }
            if (network1 == null || network2 == null) {
                return false;
            }
            return network1.getType() == network2.getType();
        }

        protected void displayIntentInfo(Intent intent) {
            boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
            boolean isFailover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);

            NetworkInfo currentNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            NetworkInfo otherNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            logger.info("Connectivity change detected...");
            logger.info("EXTRA_NO_CONNECTIVITY: {}", noConnectivity);
            logger.info("EXTRA_REASON: {}", reason);
            logger.info("EXTRA_IS_FAILOVER: {}", isFailover);
            logger.info("EXTRA_NETWORK_INFO: {}", currentNetworkInfo == null ? "none" : currentNetworkInfo);
            logger.info("EXTRA_OTHER_NETWORK_INFO: {}", otherNetworkInfo == null ? "none" : otherNetworkInfo);
            logger.info("EXTRA_EXTRA_INFO: {}", intent.getStringExtra(ConnectivityManager.EXTRA_EXTRA_INFO));
        }
    }
}
