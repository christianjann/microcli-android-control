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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

public class  OutputActivity extends ListActivity
{
    MicrodroidApplication microapp;
    private static final boolean D = true;
    private static final String TAG = "OutputActivity";

    public static final int TYPE_ON_OFF = 0;
    public static final int TYPE_LCD = 1;
    private static final int TYPE_MAX_COUNT = TYPE_LCD + 1;

    private MyCustomAdapter mAdapter;
    private static final int LIST_OUTPUT_RESPONCE_ID = 45;
    private static final int GET_INPUT_LED_RESPONCE_ID = 46;
    private static final int GET_INPUT_LCD_RESPONCE_ID = 47;

    public static final int STATE_ON = 1;
    public static final int STATE_OFF = 0;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        microapp = (MicrodroidApplication) getApplication();
        mAdapter = new MyCustomAdapter();
        setListAdapter(mAdapter);
    }

    @Override
    public synchronized void onResume()
    {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");
        microapp.addUiUpdateHandler(mHandler);
        microapp.sendMessageWithResponseID("list outputs", LIST_OUTPUT_RESPONCE_ID);
    }

    @Override
    public synchronized void onPause()
    {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
        microapp.removeUiUpdateHandler(mHandler);
    }

    public class OutputDevice
    {
        int type, state;
        String name, str1, str2;

        OutputDevice(int type, String name, int state, String str1, String str2)
        {
            this.type = type;
            this.name = name;
            this.state = state;
            this.str1 = str1;
            this.str2 = str2;
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
                    startActivity(new Intent(OutputActivity.this, MicrodroidActivity.class));
                    break;
                }
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_TIMEOUT:
                microapp.stopService();
                startActivity(new Intent(OutputActivity.this, MicrodroidActivity.class));
                break;
            case MicrodroidApplication.MESSAGE_RAW_BLUETOOTH_RECEIVED:
                break;
            case MicrodroidApplication.MESSAGE_RESPONSE_COMMAND:
                if (msg.arg1 == LIST_OUTPUT_RESPONCE_ID)
                {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> response = (ArrayList<String>)msg.obj;
                    List<ValueAndString> getFlagState = new ArrayList<ValueAndString>();
                    for (int i = 1; i < response.size() - 1; i++)
                    {
                        //aboutTextView.append(response.get(i)+"\n");
                        if (response.get(i).startsWith("LED") || response.get(i).startsWith("RELAIS") ||
                                response.get(i).startsWith("OUT_12V_"))
                        {
                            OutputDevice out = new OutputDevice(TYPE_ON_OFF, response.get(i), STATE_OFF, "", "");
                            mAdapter.add(out);
                            getFlagState.add(new ValueAndString(GET_INPUT_LED_RESPONCE_ID, "get " + response.get(i)));
                        }
                        else if (response.get(i).startsWith("LCD"))
                        {
                            OutputDevice out = new OutputDevice(TYPE_LCD, response.get(i), STATE_OFF, "Line1", "Line2");
                            mAdapter.add(out);
                            getFlagState.add(new ValueAndString(GET_INPUT_LCD_RESPONCE_ID, "get " + response.get(i)));
                        }
                    }
//                      for(int i=1;i<response.size()-1;i++)
//                      {
//                          microapp.sendMessageWithResponseID("get "+response.get(i), GET_INPUT_LED_RESPONCE_ID);
//                      }
                    microapp.sendMessagesWithResponseID(getFlagState);
                }
                else if (msg.arg1 == GET_INPUT_LED_RESPONCE_ID)
                {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> response = (ArrayList<String>)msg.obj;
                    // remove "microcli> get " response.get(0).replaceFirst( "microcli> get ", "")
                    // (response.get(0).substring(14)
                    if (response.size() > 2)
                    {
                        mAdapter.changeState(response.get(0).substring(14),
                                             response.get(1).equals("1") ? STATE_ON : STATE_OFF, "", "");
                    }

                }
                else if (msg.arg1 == GET_INPUT_LCD_RESPONCE_ID)
                {
                    @SuppressWarnings("unchecked")
                    ArrayList<String> response = (ArrayList<String>)msg.obj;
                    // remove "microcli> get " response.get(0).replaceFirst( "microcli> get ", "")
                    // (response.get(0).substring(14)
                    if (response.size() > 3)
                    {
                        mAdapter.changeState(response.get(0).substring(14), STATE_ON, response.get(1), response.get(2));
                    }
                }
                break;
            }
        }
    };


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
            case TYPE_ON_OFF:
                holder = (ViewHolder)v.getTag();
                //Toast.makeText(OutputActivity.this, "Item: " + holder.ledRadioButton.getText(),
                //      Toast.LENGTH_LONG).show();
                if (holder.ledRadioButton.isChecked())
                {
                    //holder.ledRadioButton.setChecked(false);
                    //microapp.sendMessage("disable TOGGLE_LEDS");
                    microapp.sendMessage("disable " + holder.ledName.getText());
                    microapp.sendMessageWithResponseID("get " + holder.ledName.getText(), GET_INPUT_LED_RESPONCE_ID);
                }
                else
                {
                    //holder.ledRadioButton.setChecked(true);
                    //microapp.sendMessage("disable TOGGLE_LEDS");
                    microapp.sendMessage("enable " + holder.ledName.getText());
                    microapp.sendMessageWithResponseID("get " + holder.ledName.getText(), GET_INPUT_LED_RESPONCE_ID);
                }

                break;
            case TYPE_LCD:
                holder = (ViewHolder)v.getTag();
                //Toast.makeText(OutputActivity.this, "SEPARATOR: " + holder.name.getText(),
                //      Toast.LENGTH_LONG).show();
                //holder.line1.setText("clicked");
                //microapp.sendMessage("lcd 0 0 clicked");

                LayoutInflater factory = LayoutInflater.from(this);
                final View lcdEntryView = factory.inflate(R.layout.lcd_text_input, null);
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Display Text");
                alert.setMessage("Enter the text that should be displayed on the LCD display:");
                //final EditText input = new EditText(this);
                alert.setView(lcdEntryView);

                //AlertDialog lcdPrompt = alert.create();
                final EditText line1 = (EditText) lcdEntryView.findViewById(R.id.editTextLine1);
                final EditText line2 = (EditText) lcdEntryView.findViewById(R.id.editTextLine2);
                line1.setText(holder.line1.getText());
                line2.setText(holder.line2.getText());

                alert.setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String l1 = line1.getText().toString();
                        String l2 = line2.getText().toString();
                        microapp.sendMessage("lcd clear");
                        microapp.sendMessage("lcd 0 0 \"" + l1 + "\"");
                        microapp.sendMessage("lcd 1 0 \"" + l2 + "\"");
                        microapp.sendMessageWithResponseID("get LCD1", GET_INPUT_LCD_RESPONCE_ID);
                        //holder.line1.setText(value);
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int which)
                    {
                        // TODO Auto-generated method stub

                    }
                });
                alert.show();


                break;
            }
        }
    }

    private class MyCustomAdapter extends BaseAdapter
    {
        private ArrayList<OutputDevice> mData = new ArrayList<OutputDevice>();
        private LayoutInflater mInflater;

        //private TreeSet<Integer> mSeparatorsSet = new TreeSet<Integer>();

        public MyCustomAdapter()
        {
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void add(final OutputDevice item)
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

        public void changeState(String name, int state, String str1, String str2)
        {
            for (int i = 0; i < mData.size(); i++)
            {
                if (mData.get(i).name.equals(name))
                {
                    mData.get(i).state = state;
                    mData.get(i).str1 = str1;
                    mData.get(i).str2 = str2;
                    notifyDataSetChanged();
                    setListAdapter(mAdapter); // just a hack to force a view update
                }
            }
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
                case TYPE_ON_OFF:
                    convertView = mInflater.inflate(R.layout.item_led, null);
                    holder.ledRadioButton = (RadioButton)convertView.findViewById(R.id.radioButtonLED);
                    holder.ledRadioButton.setChecked(mData.get(position).state == STATE_ON);
                    holder.ledName = (TextView)convertView.findViewById(R.id.ledName);
                    holder.ledName.setText(mData.get(position).name);
                    break;
                case TYPE_LCD:
                    convertView = mInflater.inflate(R.layout.item_lcd, null);
                    holder.name = (TextView)convertView.findViewById(R.id.name);
                    holder.line1 = (TextView)convertView.findViewById(R.id.line1);
                    holder.line2 = (TextView)convertView.findViewById(R.id.line2);
                    holder.name.setText(mData.get(position).name);
                    holder.line1.setText(mData.get(position).str1);
                    holder.line2.setText(mData.get(position).str2);
                    holder.image = (ImageView)convertView.findViewById(R.id.lcdicon);
                    holder.image.setImageResource(R.drawable.ic_menu_lcd);
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
        public TextView name;
        public TextView line1;
        public TextView line2;
        public RadioButton ledRadioButton;
        public TextView ledName;
        public ImageView image;
    }
}
