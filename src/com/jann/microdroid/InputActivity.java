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
import android.widget.ToggleButton;
import android.widget.Toast;

public class  InputActivity extends ListActivity
{

    MicrodroidApplication microapp;
    private static final boolean D = true;
    private static final String TAG = "InputActivity";

    public static final int TYPE_BUTTON = 0;
    public static final int TYPE_ADC = 1;
    public static final int TYPE_DIGITAL_INPUT = 2;
    private static final int TYPE_MAX_COUNT = TYPE_ADC + 1;
    private static final int LIST_INPUT_RESPONCE_ID = 48;
    private static final int GET_INPUT_RESPONCE_ID = 49;

    public static final int STATE_ON = 1;
    public static final int STATE_OFF = 0;

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
        mAdapter = new MyCustomAdapter();
        setListAdapter(mAdapter);
        microapp = (MicrodroidApplication) getApplication();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        ViewHolder holder = null;
        int type = mAdapter.getItemViewType(position);
        if (D) Log.d(TAG, "clicked: " + type);
        System.out.println("getView " + position + " " + v + " type = " + type);
        if (v != null)
        {
            holder = (ViewHolder)v.getTag();
            switch (type)
            {
            case TYPE_BUTTON:
                //holder = (ViewHolder)v.getTag();
                Toast.makeText(InputActivity.this, "Clicking this button here has no effect, " +
                               "you have to press the real hardware button!" , Toast.LENGTH_LONG).show();
                microapp.sendMessageWithResponseID("get " + holder.inputButtonLabel.getText(), GET_INPUT_RESPONCE_ID);
                break;
            case TYPE_ADC:
                break;
            case TYPE_DIGITAL_INPUT:
                Toast.makeText(InputActivity.this, "The state of this" +
                               "input will be updated immediately." , Toast.LENGTH_LONG).show();
                microapp.sendMessageWithResponseID("get " + holder.inputButtonLabel.getText(), GET_INPUT_RESPONCE_ID);
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
                    startActivity(new Intent(InputActivity.this, MicrodroidActivity.class));
                    break;
                }
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_TIMEOUT:
                microapp.stopService();
                startActivity(new Intent(InputActivity.this, MicrodroidActivity.class));
                break;
            case MicrodroidApplication.MESSAGE_RAW_BLUETOOTH_RECEIVED:
                //Toast.makeText(InputActivity.this, "Received: " + msg.obj , Toast.LENGTH_SHORT).show();
                if (((String)msg.obj).startsWith("[info] User button pressed"))
                {
                    mAdapter.changeState("BUTTON1", STATE_ON);
                    //InputDevice button = new InputDevice(TYPE_BUTTON,"BUTTON2",STATE_ON);
                    //mAdapter.add(button);
                }
                if (((String)msg.obj).startsWith("[info] User button released"))
                {
                    mAdapter.changeState("BUTTON1", STATE_OFF);
                }

                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_COMMAND:
                if (msg.arg1 == LIST_INPUT_RESPONCE_ID)
                {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> response = (ArrayList<String>)msg.obj;
                    List<ValueAndString> getFlagState = new ArrayList<ValueAndString>();
                    for (int i = 1; i < response.size() - 1; i++)
                    {
                        //aboutTextView.append(response.get(i)+"\n");
                        if (response.get(i).startsWith("BUTTON"))
                        {
                            InputDevice out = new InputDevice(TYPE_BUTTON, response.get(i), STATE_OFF);
                            mAdapter.add(out);
                            getFlagState.add(new ValueAndString(GET_INPUT_RESPONCE_ID, "get " + response.get(i)));
                        }
                        else if (response.get(i).startsWith("IN_12V_"))
                        {
                            InputDevice out = new InputDevice(TYPE_DIGITAL_INPUT, response.get(i), STATE_OFF);
                            mAdapter.add(out);
                            getFlagState.add(new ValueAndString(GET_INPUT_RESPONCE_ID, "get " + response.get(i)));
                        }
                    }
//                      for(int i=1;i<response.size()-1;i++)
//                      {
//                          microapp.sendMessageWithResponseID("get "+response.get(i), GET_INPUT_LED_RESPONCE_ID);
//                      }
                    microapp.sendMessagesWithResponseID(getFlagState);
                }
                else if (msg.arg1 == GET_INPUT_RESPONCE_ID)
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
        microapp.sendMessageWithResponseID("list inputs", LIST_INPUT_RESPONCE_ID);
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
            for (int i = 0; i < mData.size(); i++)
            {
                if (mData.get(i).name.equals(item.name))
                {
                    return;
                }
            }
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
                case TYPE_BUTTON:
                    convertView = mInflater.inflate(R.layout.item_button, null);
                    holder.inputButtonLabel = (TextView)convertView.findViewById(R.id.buttonText);
                    holder.inputButtonLabel.setText(mData.get(position).name);
                    holder.inputButton = (ToggleButton)convertView.findViewById(R.id.inputButton);
                    holder.inputButton.setChecked(mData.get(position).state == STATE_ON);
                    break;
                case TYPE_ADC:

                    break;
                case TYPE_DIGITAL_INPUT:
                    convertView = mInflater.inflate(R.layout.item_digital_in, null);
                    holder.inputButtonLabel = (TextView)convertView.findViewById(R.id.inputName);
                    holder.inputButtonLabel.setText(mData.get(position).name);
                    holder.inputRadioButton = (RadioButton)convertView.findViewById(R.id.radioButtonIN);
                    holder.inputRadioButton.setChecked(mData.get(position).state == STATE_ON);

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
        public TextView inputButtonLabel;
        public ToggleButton inputButton;
        public RadioButton inputRadioButton;
    }
}
