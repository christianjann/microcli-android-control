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

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/*
 * http://hj-shared.orgfree.com/2011/01/android-part-i-customize-list-view/
 * http://hj-shared.orgfree.com/2011/01/android-part-ii-list-view-events-and-webview/
 */
public class ListAdapter extends BaseAdapter
{

    private Activity activity;
    private int[] image;
    private String[] list1;
    private String[] list2;
    private static LayoutInflater inflater = null;

    public ListAdapter(Activity a, int[] image, String[] list1, String[] list2)
    {
        this.activity = a;
        this.image = image;
        this.list1 = list1;
        this.list2 = list2;
        ListAdapter.inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount()
    {
        return image.length;
    }

    public Object getItem(int position)
    {
        return position;
    }

    public long getItemId(int position)
    {
        return position;
    }

    public static class ViewHolder
    {
        public TextView text1;
        public TextView text2;
        public ImageView image;
    }

    public View getView(int position, View convertView, ViewGroup parent)
    {
        View vi = convertView;
        ViewHolder holder;
        if (convertView == null)
        {

            vi = inflater.inflate(R.layout.grid_list_layout, null);

            holder = new ViewHolder();
            holder.text1 = (TextView)vi.findViewById(R.id.item1);
            holder.text2 = (TextView)vi.findViewById(R.id.item2);
            holder.image = (ImageView)vi.findViewById(R.id.icon);

            vi.setTag(holder);
        }
        else
            holder = (ViewHolder)vi.getTag();

        holder.text1.setText(this.list1[position]);
        holder.text2.setText(this.list2[position]);
        holder.image.setImageResource(this.image[position]);

        return vi;
    }
}
