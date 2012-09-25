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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceActivity extends Activity
{
    MicrodroidApplication microapp;
    private static final boolean D = true;

    private static final String TAG = "DeviceActivity";

    private int[] imageList = { android.R.drawable.ic_menu_manage, android.R.drawable.ic_menu_edit,
                                android.R.drawable.ic_menu_search, android.R.drawable.ic_menu_agenda,
                                android.R.drawable.ic_menu_save, android.R.drawable.ic_menu_info_details
                              };
    private String[] list1 = { "Options", "Input/Output", "Flags", "Console", "Logging", "About" };
    private String[] list2 = { "Change device options.",
                               "View and configure Input/Output",
                               "Flags get set if something unexpected happens.",
                               "Send commands directly or execute scripts.",
                               "Show a log of the communication with this device.",
                               "About this device."
                             };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        microapp = (MicrodroidApplication) getApplication();

        // Set up the window layout
        setContentView(R.layout.list);

        // Set up the custom title
        setStatus(getString(R.string.title_connected_to, microapp.mConnectedDeviceName));

        ListView lv = (ListView) findViewById(R.id.lvMain);

        ListAdapter listAdapter = new ListAdapter(DeviceActivity.this, imageList, list1, list2);
        lv.setAdapter(listAdapter);

        lv.setOnItemClickListener(new OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> adapt, View view, int position, long id)
            {
                TextView tvTitle = (TextView) view.findViewById(R.id.item1);
                if (tvTitle.getText().equals("Input/Output"))
                {
                    Intent i = new Intent(DeviceActivity.this, IOActivity.class);
                    startActivity(i);
                }
                else if (tvTitle.getText().equals("Console"))
                {
                    Intent i = new Intent(DeviceActivity.this, ConsoleActivity.class);
                    startActivity(i);
                }
                else if (tvTitle.getText().equals("Logging"))
                {
                    Intent i = new Intent(DeviceActivity.this, LogActivity.class);
                    startActivity(i);
                }
                else if (tvTitle.getText().equals("About"))
                {
                    Intent i = new Intent(DeviceActivity.this, AboutDeviceActivity.class);
                    startActivity(i);
                }
                else if (tvTitle.getText().equals("Flags"))
                {
                    Intent i = new Intent(DeviceActivity.this, FlagsActivity.class);
                    startActivity(i);
                }
                else if (tvTitle.getText().equals("Options"))
                {
                    Intent i = new Intent(DeviceActivity.this, OptionsActivity.class);
                    startActivity(i);
                }
            }
        });
        microapp.startPingTimer();

    }

    private final void setStatus(int resId)
    {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle)
    {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the ConnectionService
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case MicrodroidApplication.MESSAGE_STATE_CHANGE:
                if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1)
                {
                case BluetoothConnectionService.STATE_CONNECTED:
                    break;
                case BluetoothConnectionService.STATE_CONNECTING:
                    break;
                case BluetoothConnectionService.STATE_LISTEN:
                case BluetoothConnectionService.STATE_NONE:
                    microapp.stopService();
                    startActivity(new Intent(DeviceActivity.this, MicrodroidActivity.class));
                    break;
                }
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_TIMEOUT:
                microapp.stopService();
                startActivity(new Intent(DeviceActivity.this, MicrodroidActivity.class));
                break;
            case MicrodroidApplication.MESSAGE_RAW_BLUETOOTH_RECEIVED:
                break;
            case MicrodroidApplication.MESSAGE_RAW_BLUETOOTH_SENT:
                break;
            }
        }
    };

    @Override
    public void onStart()
    {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");
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
        if (D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode)
        {
        case 0:
            break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_device, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.disconnect:
            // Disconnect and launch the MicrodroidActivity to connect a new device
            microapp.stopService();
            startActivity(new Intent(DeviceActivity.this, MicrodroidActivity.class));
            return true;
        case R.id.itemPrefs:
            startActivity(new Intent(this, PrefsActivity.class));
            break;
        }
        return true;
    }
}
