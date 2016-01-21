package com.suntek.rcs.wificonnector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

public class ConnectWiFiReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("WiFiConnector", "Received broadcast");
        String ssid = MainActivity.get(context, MainActivity.AUTO_CONNECT_SSID);

        if (TextUtils.isEmpty(ssid)) {
            return;
        }

        WifiManager mWiFiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        List<WifiConfiguration> wifiConfigurations = mWiFiManager.getConfiguredNetworks();
        for (WifiConfiguration wifiConfiguration : wifiConfigurations) {
            if (wifiConfiguration.SSID.equals(ssid)) {
                Log.d("WiFiConnector", "Connecting to wifi: " + ssid);
                mWiFiManager.enableNetwork(wifiConfiguration.networkId, true);
                break;
            }
        }
    }
}