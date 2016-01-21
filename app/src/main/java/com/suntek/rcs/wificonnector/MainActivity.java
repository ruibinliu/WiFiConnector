package com.suntek.rcs.wificonnector;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "WiFiConnector";
    private static final int SCAN_PERIOD = 5000;
    public static final String AUTO_CONNECT_SSID = "auto_connect_ssid";

    private WifiManager mWiFiManager;
    private TextView mText;
    private Handler mHandler;
    private boolean isDisplaying;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        mWiFiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mText = (TextView) findViewById(R.id.text);
        mHandler = new Handler();

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("请选择通过广播自动连接的WiFi");

                List<WifiConfiguration> wifiConfigurations = mWiFiManager.getConfiguredNetworks();
                final String[] items = new String[wifiConfigurations.size()];
                for (int i = 0; i < items.length; i++) {
                    WifiConfiguration wifiConfiguration = wifiConfigurations.get(i);
                    items[i] = wifiConfiguration.SSID;
                }

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        set(MainActivity.this, AUTO_CONNECT_SSID, items[which]);
                        scanWifi();
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, null);

                AlertDialog dialog = builder.create();
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            }
        });
    }

    public static void set(Context context, String key, String value) {
        SharedPreferences mySharedPreferences = context.getSharedPreferences("wifi_connector",
                Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String get(Context context, String key) {
        SharedPreferences mySharedPreferences = context.getSharedPreferences("wifi_connector",
                Activity.MODE_PRIVATE);
        return mySharedPreferences.getString(key, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isDisplaying = true;
        mScanRunnable = new ScanRunnable();
        mHandler.post(mScanRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isDisplaying = false;
        mHandler.removeCallbacks(mScanRunnable);
        mScanRunnable = null;
    }

    private ScanRunnable mScanRunnable;

    class ScanRunnable implements Runnable {
        @Override
        public void run() {
            if (!isDisplaying) {
                return;
            }

            scanWifi();
            mScanRunnable = new ScanRunnable();
            mHandler.postDelayed(new ScanRunnable(), SCAN_PERIOD);
        }
    }

    private synchronized void scanWifi() {
        Log.d(TAG, "Begin scan wifi");

        StringBuffer buffer = new StringBuffer();

        buffer.append("(一) 使用方法:\n");
        buffer.append("1. 点击\"修改通过广播连接的目标WIFI\"按钮,选择目标WiFi\n");
        buffer.append("2. 通过命令行输入\"adb shell am broadcast -a com.suntek.rcs" +
                ".ACTION_CONNECT_WIFI\"触发连接wifi事件.\n");

        buffer.append("\n");
        buffer.append("(二) 基本信息:\n");
        buffer.append("1. 收到广播后,将自动连接WiFi:\n");
        String ssid = get(MainActivity.this, AUTO_CONNECT_SSID);
        if (TextUtils.isEmpty(ssid)) {
            buffer.append("\n");
        } else {
            buffer.append(ssid + "\n");
        }

        buffer.append("\n");
        buffer.append("2. 已记住的WiFi:\n");
        List<WifiConfiguration> wifiConfigurations = mWiFiManager.getConfiguredNetworks();
        if (wifiConfigurations == null) {
            return;
        }
        for (WifiConfiguration wifiConfiguration : wifiConfigurations) {
            String text = String.format("networkId: %d, SSID: %s", wifiConfiguration.networkId,
                    wifiConfiguration.SSID);
            Log.d(TAG, text);
            buffer.append(text + "\n");
        }

        buffer.append("\n");
        buffer.append("3. 扫描到的WiFi:\n");
        List<ScanResult> scanResults = mWiFiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            String text = scanResult.SSID;
            Log.d(TAG, text);
            buffer.append(text + "\n");
        }

        mText.setText(buffer.toString());
        Log.d(TAG, "End scan wifi");
    }

    private void checkPermission() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager
                .PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (permissionsToRequest.size() > 0) {
            requestPermissions(permissionsToRequest.toArray(new String[]{}), 0);
        }
    }
}