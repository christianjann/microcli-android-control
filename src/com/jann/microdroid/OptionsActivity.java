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
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

public class  OptionsActivity extends ListActivity
{

    MicrodroidApplication microapp;
    private static final boolean D = true;
    private static final String TAG = "OptionsActivity";

    public static final int TYPE_OPTION = 0;
    private static final int TYPE_MAX_COUNT = TYPE_OPTION + 1;

    public static final int STATE_ON = 1;
    public static final int STATE_OFF = 0;

    private static final int LIST_OPTIONS_RESPONCE_ID = 24;
    private static final int GET_OPTION_RESPONCE_ID = 25;

    private MyCustomAdapter mAdapter;
    private String mCurrentOption = "";

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
        //super.onListItemClick(l, v, position, id);
        ViewHolder holder = null;
        int type = mAdapter.getItemViewType(position);
        if (D) Log.d(TAG, "clicked: " + type);
        System.out.println("getView " + position + " " + v + " type = " + type);
        if (v != null)
        {
            switch (type)
            {
            case TYPE_OPTION:
                holder = (ViewHolder)v.getTag();

                //Toast.makeText(OptionsActivity.this, "TODO!" , Toast.LENGTH_LONG).show();
                //microapp.sendMessage("get BUTTON1");
                mCurrentOption = holder.optionName.getText().toString();
                if (holder.optionState.isChecked())
                {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            switch (which)
                            {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                //holder.optionState.setChecked(false);
                                microapp.sendMessage("disable " + mCurrentOption);
                                mAdapter.changeState(mCurrentOption, STATE_OFF);
                                // ask again to be sure it is off
                                microapp.sendMessageWithResponseID("get " + mCurrentOption, GET_OPTION_RESPONCE_ID);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Are you sure to disable " + mCurrentOption + "?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();

                }
                else
                {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            switch (which)
                            {
                            case DialogInterface.BUTTON_POSITIVE:
                                //Yes button clicked
                                //holder.optionState.setChecked(false);
                                microapp.sendMessage("enable " + mCurrentOption);
                                mAdapter.changeState(mCurrentOption, STATE_ON);
                                // ask again to be sure it is on
                                microapp.sendMessageWithResponseID("get " + mCurrentOption, GET_OPTION_RESPONCE_ID);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                //No button clicked
                                break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Are you sure to enable " + mCurrentOption + "?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
                }

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
                    startActivity(new Intent(OptionsActivity.this, MicrodroidActivity.class));
                    break;
                }
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_TIMEOUT:
                microapp.stopService();
                startActivity(new Intent(OptionsActivity.this, MicrodroidActivity.class));
                break;
            case MicrodroidApplication.MESSAGE_RAW_BLUETOOTH_RECEIVED:
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_COMMAND:
                if (msg.arg1 == LIST_OPTIONS_RESPONCE_ID)
                {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> response = (ArrayList<String>)msg.obj;
                    List<ValueAndString> getOptionState = new ArrayList<ValueAndString>();
                    for (int i = 1; i < response.size() - 1; i++)
                    {
                        //aboutTextView.append(response.get(i)+"\n");
                        InputDevice opt = new InputDevice(TYPE_OPTION, response.get(i), STATE_OFF);
                        mAdapter.add(opt);
                        getOptionState.add(new ValueAndString(GET_OPTION_RESPONCE_ID, "get " + response.get(i)));
                    }
//                      for(int i=1;i<response.size()-1;i++)
//                      {
//                          microapp.sendMessageWithResponseID("get "+response.get(i), GET_OPTION_RESPONCE_ID);
//                      }
                    microapp.sendMessagesWithResponseID(getOptionState);
                }
                else if (msg.arg1 == GET_OPTION_RESPONCE_ID)
                {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> response = (ArrayList<String>)msg.obj;
                    // remove "microcli> get " response.get(0).replaceFirst( "microcli> get ", "")
                    // (response.get(0).substring(14)
                    mAdapter.changeState(response.get(0).substring(14),
                                         response.get(1).equals("1") ? STATE_ON : STATE_OFF);
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
        microapp.sendMessageWithResponseID("list options", LIST_OPTIONS_RESPONCE_ID);
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
                case TYPE_OPTION:
                    convertView = mInflater.inflate(R.layout.item_option, null);
                    holder.optionName = (TextView)convertView.findViewById(R.id.optionName);
                    holder.optionName.setText(mData.get(position).name);
                    holder.optionState = (CheckBox)convertView.findViewById(R.id.checkBoxOptionState);
                    holder.optionState.setChecked(mData.get(position).state == STATE_ON);
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
        public TextView optionName;
        public CheckBox optionState;
    }
}
