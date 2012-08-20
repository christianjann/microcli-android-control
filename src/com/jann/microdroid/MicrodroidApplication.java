/**
 * The main application
 *
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;

public class MicrodroidApplication extends Application implements OnSharedPreferenceChangeListener
{

    private static final String TAG = MicrodroidApplication.class.getSimpleName();
    private static final boolean D = true;
    private SharedPreferences prefs;
    boolean devmode;

    public String titleText = "";
    private Handler uiUpdateHandler = null;

    // Message types sent from the BluetoothConnectionService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_RAW_BLUETOOTH_RECEIVED = 6;
    public static final int MESSAGE_RAW_BLUETOOTH_SENT = 7;
    public static final int MESSAGE_RESPONSE_COMMAND = 8;
    public static final int MESSAGE_RESPONSE_TIMEOUT = 9;
    public static final int WiFi = 333333;
    public static final int Bluetooth = 444444;

    public final String CUSTOM_INTENT = "com.micro.microdroid.statebroadcast";
    public static final String BROADCAST_ACTION = "com.micro.microdroid.statebroadcast";
    private Intent intent;

    // Key names received from the BluetoothConnectionService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    private BluetoothDevice mDevice;

    // Name of the connected device
    public String mConnectedDeviceName = null;
    // Array adapter for a textView
    public ArrayAdapter<String> mLogArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    public BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    public BluetoothConnectionService mBluetoothConnectionService = null;
    public WiFiConnectionService mWiFiConnectionService = null;

    private static final int NO_COMMAND = -1;
    private static final int WAITING_FOR_COMMAND_START = 0;
    private static final int WAITING_FOR_COMMAND_END = 1;

    private ArrayList<String> mResponseArrayList = null;
    private String mCurrentCommand = null;
    private int mCurrentID = -1;
    private int mCurrentCommandState = NO_COMMAND;
    private Queue<ValueAndString> mCommandTodoList = null;

    private static final int PING_RESPONCE_ID = 346;
    private Timer mTimer = null;
    private Timer mResponseTimeoutTimer = null;

    public int connectionType = WiFi;

    @Override
    public void onCreate()
    {
        super.onCreate();

        Log.i(TAG, "onCreated");

        // Setup preferences
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        devmode = prefs.getBoolean("devmode", true);

        mResponseArrayList = new ArrayList<String>();
        mCommandTodoList = new LinkedList<ValueAndString>();
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
        // Stop the connection services
//        if (mBluetoothConnectionService != null) mBluetoothConnectionService.stop();
        if (mWiFiConnectionService != null)
        {
            mWiFiConnectionService.stop();
            mWiFiConnectionService = null;
        }
        stopService(intent);
        mLogArrayAdapter.clear();
        Log.i(TAG, "onTerminated");
    }

    public synchronized void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        devmode = prefs.getBoolean("devmode", true);
    }

    public int openBluetoothAdapter()
    {
        // Get local Bluetooth adapter
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//        // If the adapter is null, then Bluetooth is not supported
//        if (mBluetoothAdapter == null)
//        {
//            return -1;
//        }

        return 0;
    }

    public void setupService()
    {
        // Initialize the array adapter for the messages log
        mLogArrayAdapter = new ArrayAdapter<String>(this, R.layout.console_message);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        if (connectionType == Bluetooth)
        {
            if (mBluetoothConnectionService == null)
            {
                Log.d(TAG, "setup Bluetooth Service");

                // Initialize the BluetoothConnectionService to perform bluetooth connections
//                mBluetoothConnectionService = new BluetoothConnectionService(this, mHandler);
            }
        }
        else if (connectionType == WiFi)
        {
            if (mWiFiConnectionService == null)
            {
                Log.d(TAG, "setup WiFi Service");

                // Initialize the WiFiConnectionService to perform WiFi connections
                mWiFiConnectionService = new WiFiConnectionService(this, mHandler);
            }
        }
    }

    public void startService()
    {
        if (connectionType == Bluetooth)
        {
            if (mBluetoothConnectionService != null)
            {
                // Only if the state is STATE_NONE, do we know that we haven't started already
//                if (mBluetoothConnectionService.getState() == BluetoothConnectionService.STATE_NONE)
//                {
//                    // Start the BluetoothConnectionService
//                    mBluetoothConnectionService.start();
//                }
            }
        }
        else if (connectionType == WiFi)
        {
            if (mWiFiConnectionService != null)
            {
                // Only if the state is STATE_NONE, do we know that we haven't started already
                if (mWiFiConnectionService.getState() == WiFiConnectionService.STATE_NONE)
                {
                    // Start the BluetoothConnectionService
                    mWiFiConnectionService.start();
                }
            }
        }
    }

    public void stopService()
    {
//        if (mBluetoothConnectionService != null) mBluetoothConnectionService.stop();
        if (mWiFiConnectionService != null) mWiFiConnectionService.stop();
    }

    public void startPingTimer()
    {
        if (mTimer == null)
        {
            mTimer = new Timer();
            TimerTask timerTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    sendMessageWithResponseID("ping", PING_RESPONCE_ID);
                }
            };
            mTimer.schedule(timerTask, 10000, 10000);
        }
    }

    public void stopPingTimer()
    {
        if (mTimer != null)
        {
            mTimer.cancel();
            mTimer = null;
        }
    }

    public void startResponseTimeoutTimer()
    {
        if (mResponseTimeoutTimer == null)
        {
            mResponseTimeoutTimer = new Timer();
            TimerTask timerTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    if (mCurrentCommandState != NO_COMMAND)
                    {
                        if (uiUpdateHandler != null) uiUpdateHandler.obtainMessage(MESSAGE_RESPONSE_TIMEOUT,
                                    mCurrentID, mCurrentCommandState).sendToTarget();
                        stopResponseTimeoutTimer();
                        mCommandTodoList.clear();
                        mCurrentCommandState = NO_COMMAND;
                    }
                }
            };
            if (connectionType == Bluetooth)
                mResponseTimeoutTimer.schedule(timerTask, 5000);
            else if (connectionType == WiFi)
                mResponseTimeoutTimer.schedule(timerTask, 15000);
        }
    }

    public void stopResponseTimeoutTimer()
    {
        if (mResponseTimeoutTimer != null)
        {
            mResponseTimeoutTimer.cancel();
            mResponseTimeoutTimer = null;
        }
    }

    public void pairDevice(String address)
    {
//        // Get the BLuetoothDevice object
//        mDevice = mBluetoothAdapter.getRemoteDevice(address);
//
//        // Attempt to connect to the device
//        mBluetoothConnectionService.connect(mDevice);
    }

    public void addUiUpdateHandler(Handler handler)
    {
        uiUpdateHandler = handler;
    }
    public void removeUiUpdateHandler(Handler handler)
    {
        uiUpdateHandler = null;
    }

    //The Handler that gets information back from the ConnectionService
    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case MESSAGE_STATE_CHANGE:
                if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1)
                {
                case BluetoothConnectionService.STATE_CONNECTED:
                case WiFiConnectionService.STATE_CONNECTED:
                    //mBluetoothConnectionService.write("lcd clear".getBytes());
                    if (uiUpdateHandler != null) uiUpdateHandler.obtainMessage(MESSAGE_STATE_CHANGE,
                                BluetoothConnectionService.STATE_CONNECTED, -1).sendToTarget();
                    break;
                case BluetoothConnectionService.STATE_CONNECTING:
                    if (uiUpdateHandler != null) uiUpdateHandler.obtainMessage(MESSAGE_STATE_CHANGE,
                                BluetoothConnectionService.STATE_CONNECTING, -1).sendToTarget();
                    break;
                case BluetoothConnectionService.STATE_LISTEN:
                case BluetoothConnectionService.STATE_NONE:
                case WiFiConnectionService.STATE_NONE:
                    if (uiUpdateHandler != null) uiUpdateHandler.obtainMessage(MESSAGE_STATE_CHANGE,
                                BluetoothConnectionService.STATE_NONE, -1).sendToTarget();
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mLogArrayAdapter.add("Me:  " + writeMessage.replaceAll("\\r\\n", ""));
                if (uiUpdateHandler != null) uiUpdateHandler.obtainMessage(MESSAGE_RAW_BLUETOOTH_SENT,
                            writeMessage).sendToTarget();
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                if(readMessage.length()>0)
                {
	                mLogArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
	                if (uiUpdateHandler != null) uiUpdateHandler.obtainMessage(MESSAGE_RAW_BLUETOOTH_RECEIVED,
	                            readMessage).sendToTarget();
	                if (mCurrentCommandState == WAITING_FOR_COMMAND_START)
	                {
	                    if (readMessage.startsWith(mCurrentCommand))
	                    {
	                        mCurrentCommandState = WAITING_FOR_COMMAND_END;
	                        mResponseArrayList.add(readMessage);
	                    }
	                }
	                else if (mCurrentCommandState == WAITING_FOR_COMMAND_END)
	                {
	                    mResponseArrayList.add(readMessage);
	                    if (readMessage.startsWith("[end]"))
	                    {
	                        mCurrentCommandState = NO_COMMAND;
	                        stopResponseTimeoutTimer();
	                        if (uiUpdateHandler != null) uiUpdateHandler.obtainMessage(MESSAGE_RESPONSE_COMMAND,
	                                    mCurrentID, -1, mResponseArrayList.clone()).sendToTarget();
	                        pollCommandTodoList();
	                    }
	                }
                }
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    public synchronized void sendMessage(String message)
    {
        if (connectionType == Bluetooth)
        {
//            // Check that we're actually connected before trying anything
//            if (mBluetoothConnectionService.getState() != BluetoothConnectionService.STATE_CONNECTED)
//            {
//                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Check that there's actually something to send
//            if (message.length() > 0)
//            {
//                // Get the message bytes and tell the BluetoothConnectionService to write
//                byte[] send = message.getBytes();
//                mBluetoothConnectionService.write(send);
//
//                // Reset out string buffer to zero and clear the edit text field
//                mOutStringBuffer.setLength(0);
//                //mOutEditText.setText(mOutStringBuffer);
//            }
        }
        else if (connectionType == WiFi)
        {
            // Check that we're actually connected before trying anything
            if (mWiFiConnectionService.getState() != WiFiConnectionService.STATE_CONNECTED)
            {
                Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
                return;
            }

            // Check that there's actually something to send
            if (message.length() > 0)
            {
                // Get the message bytes and tell the WiFiConnectionService to write
                mWiFiConnectionService.sendMessage(message);

                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
                //mOutEditText.setText(mOutStringBuffer);
            }
        }
    }

    public synchronized void sendMessageWithResponseID(String message, int response_id_to_use)
    {
        if (mCurrentCommandState != NO_COMMAND)
        {
            mCommandTodoList.offer(new ValueAndString(response_id_to_use, message));
        }
        else
        {
            mCurrentCommandState = WAITING_FOR_COMMAND_START;
            mCurrentCommand = "microcli> " + message;
            mCurrentID = response_id_to_use;
            mResponseArrayList.clear();
            sendMessage(message);
            startResponseTimeoutTimer();
        }
    }

    public synchronized void sendMessagesWithResponseID(List<ValueAndString> messages)
    {

        for (int i = 0; i < messages.size(); i++)
            sendMessageWithResponseID(messages.get(i).str, messages.get(i).value);
    }

    private void pollCommandTodoList()
    {
        if (mCurrentCommandState == NO_COMMAND)
        {
            if (mCommandTodoList.isEmpty() == false)
            {
                ValueAndString tmp = mCommandTodoList.poll();
                sendMessageWithResponseID(tmp.str, tmp.value);
            }
        }
    }

    public String readLine()
    {
        String line = "";
        return line;
    }
}
