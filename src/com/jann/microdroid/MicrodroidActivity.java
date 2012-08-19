/*
 * This file is part of MicroCLI.
 *
 * Copyright (C) 2012 Christian Jann <christian.jann@ymail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jann.microdroid;

import java.net.InetAddress;
import java.net.UnknownHostException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

import android.widget.TextView;
import android.widget.Toast;

public class MicrodroidActivity extends Activity
{
    private static final String TAG = "MicrodroidActivity";

    private static final boolean USE_BLUETOOTH = false;
    private static final boolean RUN_IN_EMULATOR = true;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_PAIR_DEVICE = 3;

    Button connectBluetoothButton;
    Button connectWiFiButton;

    MicrodroidApplication microapp;

    private static final boolean D = true;

    private TextView mTitle;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);

        // Find views
        connectBluetoothButton = (Button) findViewById(R.id.button1);
        connectBluetoothButton.setOnClickListener(mBluetoothConnectListener);

        connectWiFiButton = (Button) findViewById(R.id.button2);
        connectWiFiButton.setOnClickListener(mWiFiConnectListener);

        microapp = (MicrodroidApplication) getApplication();
        microapp.stopService();
        if (microapp.openBluetoothAdapter() != 0)
        {
            Toast.makeText(this, "Bluetooth is not available.", Toast.LENGTH_LONG).show();
            if (USE_BLUETOOTH)
                finish();
        }
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        //microapp.addUiUpdateHandler(mHandler);
        if (USE_BLUETOOTH)
        {
            if (!microapp.mBluetoothAdapter.isEnabled())
            {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                // Otherwise, setup the chat session
            }
        }

        microapp.stopService();
        microapp.stopPingTimer();
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");
        microapp.addUiUpdateHandler(mHandler);
    }

    @Override
    public synchronized void onPause()
    {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
        microapp.removeUiUpdateHandler(mHandler);
    }

    @Override
    public void onStop()
    {
        super.onStop();
        //microapp.removeUiUpdateHandler(mHandler);
        if (D) Log.e(TAG, "-- ON STOP --");

    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        microapp.stopService();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode)
        {
        case REQUEST_PAIR_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK)
            {
                // Get the device MAC address
                String address = data.getExtras()
                                 .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                microapp.pairDevice(address);
            }
            break;
        case REQUEST_CONNECT_DEVICE:
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK)
            {
                // Bluetooth is now enabled, so set up the service
                microapp.setupService();
            }
            else
            {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    public void scanAndConnectNewBluetoothDevice()
    {
        if (USE_BLUETOOTH)
        {
            microapp.connectionType = MicrodroidApplication.Bluetooth;
            microapp.setupService();
            microapp.startService();
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_PAIR_DEVICE);
        }
        else
        {
            Toast.makeText(MicrodroidActivity.this, "The App was compiled with Bluetooth support disabled.",
                           Toast.LENGTH_SHORT).show();
        }
    }

    private String intToIp(int i)
    {
        return (i & 0xFF) + "." +
               ((i >> 8) & 0xFF) + "." +
               ((i >> 16) & 0xFF) + "." +
               ((i >> 24) & 0xFF);
    }

    public void connectNewWiFiDevice()
    {
        if (!RUN_IN_EMULATOR)
        {
            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            String extra = "";
            if ((wifi.isWifiEnabled() == true))
            {
                Toast.makeText(MicrodroidActivity.this, "MOBILE Is Connected TO WI-FI!", Toast.LENGTH_SHORT).show();

                WifiInfo info = wifi.getConnectionInfo();
                if (D)Log.d(TAG, "\n\nWiFi Status: " + info.toString());

                // DhcpInfo is a simple object for retrieving the results of a DHCP request
                DhcpInfo dhcp = wifi.getDhcpInfo();
                if (dhcp == null)
                {
                    Log.d(TAG, "Could not get dhcp info");
                    return;
                }
                if (D)Log.d(TAG, "\n\nWiFi DNS1: " + intToIp(dhcp.dns1));

                String ip_micro_de = "";
                try
                {
                    InetAddress[] hostInetAddress
                        = InetAddress.getAllByName("micro.de");
                    String all = "";
                    for (int i = 0; i < hostInetAddress.length; i++)
                    {
                        ip_micro_de = hostInetAddress[i].toString();
                        all = all + String.valueOf(i) + " : "
                              + ip_micro_de + "\n";
                    }
                    if (D)Log.d(TAG, "\n\nIP Info: " + all);

                }
                catch (UnknownHostException e)
                {
                    e.printStackTrace();
                }

                if (intToIp(dhcp.dns1).equals("192.168.1.1") && ip_micro_de.equals("wifi.jann.cc/192.168.1.1"))
                {
                    microapp.connectionType = MicrodroidApplication.WiFi;
                    microapp.setupService();
                    microapp.startService();
                    return;
                }
                else
                {
                    extra = " Error: worng DNS or IP: " + intToIp(dhcp.dns1) + ". You aren't " +
                            "connected to the correct hardware.";
                }
            }

            AlertDialog.Builder WIFIOFF = new Builder(MicrodroidActivity.this);
            //WIFIOFF.setCancelable(false);
            WIFIOFF.setTitle("Connection Error");
            WIFIOFF.setMessage(" Please enable your WiFi and connect your device." + extra);
            WIFIOFF.setPositiveButton("Open WiFi Settings", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            });
            WIFIOFF.show();
        }
        else if (RUN_IN_EMULATOR)
        {
            microapp.connectionType = MicrodroidApplication.WiFi;
            microapp.setupService();
            microapp.startService();
            return;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.scan_bluetooth:
            scanAndConnectNewBluetoothDevice();
            return true;
        case R.id.scan_wifi:
            connectNewWiFiDevice();
            return true;
        case R.id.itemPrefs:
            startActivity(new Intent(this, PrefsActivity.class));
            break;
        }
        return true;
    }

    // Called when button is clicked
    View.OnClickListener mBluetoothConnectListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            Log.d(TAG, "onClick");
            scanAndConnectNewBluetoothDevice();
        }
    };

    View.OnClickListener mWiFiConnectListener = new OnClickListener()
    {
        public void onClick(View v)
        {
            Log.d(TAG, "onClick");
            connectNewWiFiDevice();
        }
    };

    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case MicrodroidApplication.MESSAGE_STATE_CHANGE:
//                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1)
                {
                case BluetoothConnectionService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected);
                    //mTitle.append(mConnectedDeviceName);
                    startActivity(new Intent(MicrodroidActivity.this, DeviceActivity.class));
                    break;
                case BluetoothConnectionService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothConnectionService.STATE_LISTEN:
                case BluetoothConnectionService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                }
            }
        }
    };
}
