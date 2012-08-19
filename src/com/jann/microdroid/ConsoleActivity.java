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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


public class ConsoleActivity extends  Activity
{
    MicrodroidApplication microapp;
    private static final boolean D = true;
    private TextView mTitle;
    private static final String TAG = "ConsoleActivity";

    // Layout Views
    private ListView mConsoleView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Array adapter for the console log
    private ArrayAdapter<String> mConsoleArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        microapp = (MicrodroidApplication) getApplication();

        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.console_main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);

        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.title_connected_to);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        mTitle.append(microapp.mConnectedDeviceName);
    }

    private void setupConsole()
    {
        Log.d(TAG, "setupConsole()");

        // Initialize the array adapter for the console log
        mConsoleArrayAdapter = new ArrayAdapter<String>(this, R.layout.console_message);
        mConsoleView = (ListView) findViewById(R.id.in);
        mConsoleView.setAdapter(mConsoleArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new OnClickListener()
        {
            public void onClick(View v)
            {
                // Send a message using content of the edit text widget
                TextView view = (TextView) findViewById(R.id.edit_text_out);
                String message = view.getText().toString();
                sendMessage(message);
            }
        });

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message)
    {
        microapp.sendMessage(message);
        // Reset out string buffer to zero and clear the edit text field
        mOutStringBuffer.setLength(0);
        mOutEditText.setText(mOutStringBuffer);
    }


    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener()
    {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event)
        {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP)
            {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if (D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

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
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(microapp.mConnectedDeviceName);
                    mConsoleArrayAdapter.clear();
                    break;
                case BluetoothConnectionService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothConnectionService.STATE_LISTEN:
                case BluetoothConnectionService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    microapp.stopService();
                    startActivity(new Intent(ConsoleActivity.this, MicrodroidActivity.class));
                    break;
                }
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_TIMEOUT:
                microapp.stopService();
                startActivity(new Intent(ConsoleActivity.this, MicrodroidActivity.class));
                break;
            case MicrodroidApplication.MESSAGE_RAW_BLUETOOTH_RECEIVED:
                mConsoleArrayAdapter.add(microapp.mConnectedDeviceName + ":  " + (String)msg.obj);
                break;
            case MicrodroidApplication.MESSAGE_RAW_BLUETOOTH_SENT:
                mConsoleArrayAdapter.add("Me:  " + ((String)msg.obj).replaceAll("\\r\\n", ""));
                break;
            }
        }
    };


    @Override
    public void onStart()
    {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");
        setupConsole();
        microapp.addUiUpdateHandler(mHandler);
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");
        microapp.addUiUpdateHandler(mHandler);
        microapp.stopPingTimer();
    }

    @Override
    public synchronized void onPause()
    {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
        microapp.removeUiUpdateHandler(mHandler);
        microapp.startPingTimer();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");
        //microapp.removeUiUpdateHandler(mHandler);
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
            startActivity(new Intent(ConsoleActivity.this, MicrodroidActivity.class));
            return true;
        case R.id.itemPrefs:
            startActivity(new Intent(this, PrefsActivity.class));
            break;
        }
        return true;
    }
}
