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

import java.util.ArrayList;
import java.util.List;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class  FlagsActivity extends ListActivity
{

    MicrodroidApplication microapp;
    private static final boolean D = true;
    private static final String TAG = "FlagsActivity";

    public static final int TYPE_FLAG = 0;
    private static final int TYPE_MAX_COUNT = TYPE_FLAG + 1;

    public static final int STATE_ON = 1;
    public static final int STATE_OFF = 0;

    private static final int LIST_FLAGS_RESPONCE_ID = 12;
    private static final int GET_FLAG_RESPONCE_ID = 14;

    private MyCustomAdapter mAdapter;

    public class InputDevice
    {
        int type;
        String name;
        int state;

        InputDevice(int type, String name, int state)
        {
            this.type = type;
            this.name = name;
            this.state = state;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        microapp = (MicrodroidApplication) getApplication();

        mAdapter = new MyCustomAdapter();

        setListAdapter(mAdapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        int type = mAdapter.getItemViewType(position);
        if (D) Log.d(TAG, "clicked: " + type);
        System.out.println("getView " + position + " " + v + " type = " + type);
        if (v != null)
        {
            switch (type)
            {
            case TYPE_FLAG:
                Toast.makeText(FlagsActivity.this, "Clicking this flag here has no effect, " +
                               "you have to reset the controller to reset this flag" , Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    private final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
            case MicrodroidApplication.MESSAGE_STATE_CHANGE:
                switch (msg.arg1)
                {
                case BluetoothConnectionService.STATE_CONNECTED:
                    break;
                case BluetoothConnectionService.STATE_CONNECTING:
                    break;
                case BluetoothConnectionService.STATE_LISTEN:
                case BluetoothConnectionService.STATE_NONE:
                    microapp.stopService();
                    startActivity(new Intent(FlagsActivity.this, MicrodroidActivity.class));
                    break;
                }
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_TIMEOUT:
                microapp.stopService();
                startActivity(new Intent(FlagsActivity.this, MicrodroidActivity.class));
                break;
            case MicrodroidApplication.MESSAGE_RAW_BLUETOOTH_RECEIVED:
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_COMMAND:
                if (msg.arg1 == LIST_FLAGS_RESPONCE_ID)
                {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> response = (ArrayList<String>)msg.obj;
                    List<ValueAndString> getFlagState = new ArrayList<ValueAndString>();
                    for (int i = 1; i < response.size() - 1; i++)
                    {
                        InputDevice flag = new InputDevice(TYPE_FLAG, response.get(i), STATE_OFF);
                        mAdapter.add(flag);
                        getFlagState.add(new ValueAndString(GET_FLAG_RESPONCE_ID, "get " + response.get(i)));
                    }
                    microapp.sendMessagesWithResponseID(getFlagState);
                }
                else if (msg.arg1 == GET_FLAG_RESPONCE_ID)
                {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> response = (ArrayList<String>)msg.obj;
                    // remove "microcli> get " response.get(0).replaceFirst( "microcli> get ", "")
                    // (response.get(0).substring(14)
                    if (response.size() > 2)
                    {
                        mAdapter.changeState(response.get(0).substring(14),
                                             response.get(1).equals("1") ? STATE_ON : STATE_OFF);
                    }
                }
                break;
            }
        }
    };

    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");
        microapp.addUiUpdateHandler(mHandler);
        microapp.sendMessageWithResponseID("list flags", LIST_FLAGS_RESPONCE_ID);
    }

    @Override
    public synchronized void onPause()
    {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
        microapp.removeUiUpdateHandler(mHandler);
    }

    private class MyCustomAdapter extends BaseAdapter
    {
        private ArrayList<InputDevice> mData = new ArrayList<InputDevice>();
        private LayoutInflater mInflater;

        public MyCustomAdapter()
        {
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void add(final InputDevice item)
        {
            mData.add(item);
            notifyDataSetChanged();
        }

        public void changeState(String name, int state)
        {
            for (int i = 0; i < mData.size(); i++)
            {
                if (mData.get(i).name.equals(name))
                {
                    mData.get(i).state = state;
                    notifyDataSetChanged();
                    setListAdapter(mAdapter); // just a hack to force a view update
                }
            }
        }

        @Override
        public int getItemViewType(int position)
        {
            //return mSeparatorsSet.contains(position) ? TYPE_LCD : TYPE_LED;
            return mData.get(position).type;
        }

        @Override
        public int getViewTypeCount()
        {
            return TYPE_MAX_COUNT;
        }

        public int getCount()
        {
            return mData.size();
        }

        public String getItem(int position)
        {
            return mData.get(position).name;
        }

        public long getItemId(int position)
        {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder = null;
            int type = getItemViewType(position);
            System.out.println("getView " + position + " " + convertView + " type = " + type);
            if (convertView == null)
            {
                holder = new ViewHolder();
                switch (type)
                {
                case TYPE_FLAG:
                    convertView = mInflater.inflate(R.layout.item_flag, null);
                    holder.flagName = (TextView)convertView.findViewById(R.id.flagName);
                    holder.flagName.setText(mData.get(position).name);
                    holder.flagState = (RadioButton)convertView.findViewById(R.id.radioButtonFlagState);
                    holder.flagState.setChecked(mData.get(position).state == STATE_ON);
                    break;

                }
                convertView.setTag(holder);
            }
            else
            {
                holder = (ViewHolder)convertView.getTag();
            }

            return convertView;
        }
    }

    public static class ViewHolder
    {
        public TextView flagName;
        public RadioButton flagState;
    }
}
