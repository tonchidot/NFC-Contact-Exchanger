/*
 * Copyright (C) 2011 Tonchidot Corporation. All rights reserved.
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

package com.tonchidot.nfc_contact_exchanger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import a_vcard.android.syncml.pim.PropertyNode;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tonchidot.nfc_contact_exchanger.lib.Contact;
import com.tonchidot.nfc_contact_exchanger.lib.ImageUtils;
import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils;
import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils.NodeHandler;
import com.tonchidot.nfc_contact_exchanger.tools.ImageStorage;

public class HistoryAdapter extends BaseAdapter implements Filterable {

    private ArrayList<Contact> mOriginalValues;
    private ArrayList<Contact> items;
    private Context context;
    private ImageStorage imgStore;

    public HistoryAdapter(Context context, List<Contact> history) {
        this.context = context;
        this.items = new ArrayList<Contact>(history);
        this.imgStore = new ImageStorage(context);
    }

    public void addItemTop(Contact item) {
        addItem(item, 0);
    }

    public void addItem(Contact item) {
        addItem(item, items.size());
    }

    public void addItem(Contact item, int position) {
        items.add(position, item);
        notifyDataSetChanged();
    }

    public void removeItem(long id) {
        for (int i = 0; i < getCount(); i++) {
            if (id == getItemId(i)) {
                items.remove(i);
                notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return ((Contact) items.get(position)).id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    public void sortByDate() {
        Collections.sort(items, new Comparator<Contact>() {

            @Override
            public int compare(Contact object1, Contact object2) {
                return -(int) (object1.id - object2.id);
            }

        });
        mOriginalValues = items;
        notifyDataSetChanged();
    }

    public void sortByName() {
        Collections.sort(items, new Comparator<Contact>() {

            @Override
            public int compare(Contact object1, Contact object2) {
                return object1.name.compareTo(object2.name);
            }

        });
        mOriginalValues = items;
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return new HistoryEntryFilter();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if (convertView == null) {
            final LayoutInflater layoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.history_line, null);

            holder = new ViewHolder();
            holder.contactImage = (ImageView) convertView.findViewById(R.id.image_contact);
            holder.contactNameText = (TextView) convertView.findViewById(R.id.text_contact_name);
            holder.contactDateText = (TextView) convertView.findViewById(R.id.text_contact_date);
            holder.symbolsLayout = (LinearLayout) convertView
                    .findViewById(R.id.layout_contact_symbols);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final Contact entry = (Contact) getItem(position);

        holder.contactNameText.setText(entry.name);
        holder.contactDateText.setText(entry.getFormattedDate(context));
        loadImage(holder.contactImage, entry);

        holder.symbolsLayout.removeAllViews();
        final int size = ImageUtils.getPixelsForDip(context, 20);

        VCardUtils.doForEachEntry(entry.vcard, new NodeHandler<Void>() {

            @Override
            public Void handle(PropertyNode node) {
                if ("TEL".equals(node.propName)) {
                    if (node.paramMap_TYPE.contains("CELL")) {
                        addIcon(R.drawable.icon_cell);
                    } else {
                        addIcon(R.drawable.icon_phone);
                    }
                } else if ("ADR".equals(node.propName)) {
                    addIcon(R.drawable.icon_address);
                } else if ("EMAIL".equals(node.propName)) {
                    addIcon(R.drawable.icon_email);
                } else if ("BDAY".equals(node.propName)) {
                    addIcon(R.drawable.icon_bday);
                } else if ("URL".equals(node.propName)) {
                    if (node.propValue.contains("twitter.com/")) {
                        addIcon(R.drawable.icon_twitter);
                    } else {
                        addIcon(R.drawable.icon_url);
                    }
                } else if ("NOTE".equals(node.propName)) {
                    addIcon(R.drawable.icon_note);
                } else if ("X-SKYPE-USERNAME".equals(node.propName)) {
                    addIcon(R.drawable.icon_skype);
                }
                return null;
            }

            public void addIcon(int resId) {
                ImageView image = new ImageView(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                params.setMargins(0, 0, size / 3, 0);
                image.setLayoutParams(params);
                image.setImageResource(resId);
                holder.symbolsLayout.addView(image);
            }
        }, false);

        return convertView;
    }

    private void loadImage(final ImageView contactImage, Contact entry) {
        Drawable photoDrawable = VCardUtils.getDrawable(entry.vcard);
        final String photoUrl = VCardUtils.getPhotoUrl(entry.vcard);
        if (photoDrawable != null) {
            contactImage.setTag("");
            contactImage.setImageDrawable(photoDrawable);
        } else if (photoUrl != null) {
            contactImage.setTag(photoUrl);
            contactImage.setImageResource(R.drawable.vcard_default);
            imgStore.displayImageFor(contactImage, entry);
        } else {
            contactImage.setTag("");
            contactImage.setImageResource(R.drawable.vcard_default);
        }

    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {
        ImageView contactImage;
        TextView contactNameText;
        TextView contactDateText;
        LinearLayout symbolsLayout;
    }

    private class HistoryEntryFilter extends Filter {
        private Object lock = new Object();

        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();

            if (mOriginalValues == null) {
                synchronized (lock) {
                    mOriginalValues = new ArrayList<Contact>(items);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                synchronized (lock) {
                    ArrayList<Contact> list = new ArrayList<Contact>(mOriginalValues);
                    results.values = list;
                    results.count = list.size();
                }
            } else {
                String prefixString = prefix.toString().toLowerCase();

                final ArrayList<Contact> values = mOriginalValues;
                final int count = values.size();

                final ArrayList<Contact> newValues = new ArrayList<Contact>(count);

                for (int i = 0; i < count; i++) {
                    final Contact value = values.get(i);
                    final String valueText = value.name.toLowerCase();

                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(value);
                    } else {
                        final String[] words = valueText.split(" ");
                        final int wordCount = words.length;

                        for (int k = 0; k < wordCount; k++) {
                            if (words[k].startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            items = (ArrayList<Contact>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}
