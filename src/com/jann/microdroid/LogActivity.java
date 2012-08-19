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

/*
 * http://developer.android.com/resources/tutorials/views/hello-tabwidget.html
 */


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;


public class LogActivity extends  Activity
{
    MicrodroidApplication microapp;
    private static final boolean D = true;
    private TextView mTitle;
    private static final String TAG = "LOGActivity";

    // Layout Views
    private ListView mConversationView;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        microapp = (MicrodroidApplication) getApplication();

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.logging_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.title_connected_to);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mTitle.append(microapp.mConnectedDeviceName);


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
                    startActivity(new Intent(LogActivity.this, MicrodroidActivity.class));
                    break;
                }
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_TIMEOUT:
                microapp.stopService();
                startActivity(new Intent(LogActivity.this, MicrodroidActivity.class));
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
        // Initialize the array adapter for the conversation thread
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(microapp.mLogArrayAdapter);
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
        inflater.inflate(R.menu.menu_logging, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.clearLog:
            microapp.mLogArrayAdapter.clear();
            return true;
        case R.id.itemPrefs:
            startActivity(new Intent(this, PrefsActivity.class));
            break;
        }
        return true;
    }

}

