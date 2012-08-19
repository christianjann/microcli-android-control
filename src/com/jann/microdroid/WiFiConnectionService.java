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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for sending and receiving broadcast packets.
 * It has a thread that listens for incoming packets.
 */
public class WiFiConnectionService
{
    // Debugging
    private static final String TAG = "WiFiConnectionService";
    private static final boolean D = true;

    public static final int STATE_CONNECTED = 456;
    public static final int STATE_NONE = 457;

    // Member fields
    private final Handler mHandler;
    private ComThread mConnectedThread;
    private int mState;

    Context mContext ;

    /**
     * Constructor. Prepares a new WiFi connection service.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public WiFiConnectionService(Context context, Handler handler)
    {
        //mAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
        mHandler = handler;
        mState = STATE_NONE;
    }

    /**
     * Set the current state of the WiFi connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state)
    {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MicrodroidApplication.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState()
    {
        return mState;
    }

    /**
     * Start the WiFi connection service. Specifically start ConnectedThread to begin
     * listening incoming broadcast packets.
     */
    public synchronized void start()
    {
        if (D) Log.d(TAG, "start");

        mConnectedThread = new ComThread();
        mConnectedThread.start();
    }

    /**
     * Stop thread
     */
    public synchronized void stop()
    {
        if (D) Log.d(TAG, "stop");
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    public void write(byte[] out)
    {
        mConnectedThread.write(out);
    }

    public void sendMessage(String message)
    {
        mConnectedThread.write(new String(message + "\r\n").getBytes());
    }

    /**
     * This thread handles all incoming and outgoing transmissions.
     */
    private class ComThread extends Thread
    {
        private static final int TCP_SERVER_PORT = 2000;
        private Socket mSocket ;
        //InetAddress myBcastIP, myLocalIP ;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ComThread()
        {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try
            {
                mSocket = new Socket("192.168.1.1", TCP_SERVER_PORT);

                // Get theSocket input and output streams
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();

            }
            catch (IOException e)
            {
                Log.e(TAG, "Could not make socket", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            if (mmInStream == null || mmOutStream == null)
            {
                Log.e(TAG, "(mmInStream == null || mmOutStream == null)");
                setState(STATE_NONE);
            }

            // Send the name of the connected device back to the UI Activity
            Message msg = mHandler.obtainMessage(MicrodroidApplication.MESSAGE_DEVICE_NAME);
            Bundle bundle = new Bundle();
            bundle.putString(MicrodroidApplication.DEVICE_NAME, "WiFi");
            msg.setData(bundle);
            mHandler.sendMessage(msg);

        }

        public void run()
        {
            try
            {
                BufferedReader buf = new BufferedReader(new InputStreamReader(mmInStream), 1024);
                String line;

                setState(STATE_CONNECTED);

                //Listen on socket to receive messages
                while (true)
                {
                    if ((line = buf.readLine()) != null)
                    {
                        mHandler.obtainMessage(MicrodroidApplication.MESSAGE_READ, line.length(), -1, line.getBytes()).sendToTarget();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                Log.e(TAG, "disconnected", e);
                setState(STATE_NONE);
                //WiFiConnectionService.this.start();
            }
        }

        /**
          * Write broadcast packet.
          */
        public void write(byte[] buffer)
        {

            try
            {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MicrodroidApplication.MESSAGE_WRITE, -1, -1, buffer)
                .sendToTarget();
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException ex)
                {
                    //break;
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
            try
            {
                mSocket.shutdownInput();
                mSocket.shutdownOutput();
                mmInStream.close();
                mmOutStream.close();
                mSocket.close();
                mSocket = null;
            }
            catch (Exception e)
            {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}

