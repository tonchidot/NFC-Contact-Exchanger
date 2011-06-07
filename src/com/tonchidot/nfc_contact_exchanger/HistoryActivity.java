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
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.tonchidot.nfc_contact_exchanger.lib.Contact;
import com.tonchidot.nfc_contact_exchanger.widgets.IconContextMenu;
import com.tonchidot.nfc_contact_exchanger.widgets.IconContextMenu.IconContextMenuOnClickListener;

public class HistoryActivity extends BaseAnalyticsActivity {

    private static final int SORT_NAME = 1;
    private static final int SORT_DATE = 2;
    private int currentSorting = SORT_DATE;

    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ListView contactsList = (ListView) findViewById(R.id.list_contacts);

        List<Contact> entries = new ArrayList<Contact>();
        Cursor cursor = managedQuery(ContactsProvider.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            entries.add(Contact.createFromCursor(cursor));
        }
        cursor.close();

        adapter = new HistoryAdapter(this, entries);
        sortList(currentSorting);
        contactsList.setAdapter(adapter);

        contactsList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Contact entry = (Contact) adapter.getItem(position);
                Intent intent = new Intent(HistoryActivity.this, ContactReceivedActivity.class);
                intent.putExtra(ContactReceivedActivity.EXTRA_HISTORY_ID, entry.id);
                startActivity(intent);
            }
        });
        contactsList.setOnItemLongClickListener(createHistoryLongClickListener(this, adapter));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (currentSorting == SORT_NAME) {
            menu.getItem(1).setVisible(true);
            menu.getItem(0).setVisible(false);
        } else {
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_date:
                sortList(SORT_DATE);
                return true;
            case R.id.menu_sort_name:
                sortList(SORT_NAME);
                return true;
            case R.id.menu_map:
                Intent intent = new Intent(this, HistoryMapActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sortList(int sortType) {
        if (sortType == SORT_DATE) {
            adapter.sortByDate();
        } else {
            adapter.sortByName();
        }
        currentSorting = sortType;
    }

    public static OnItemLongClickListener createHistoryLongClickListener(final Context context,
            final HistoryAdapter adapter) {
        return new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view,
                    final int position, final long id) {
                final int MENU_DELETE = 1;

                IconContextMenu iconContextMenu = new IconContextMenu(context);
                iconContextMenu.addItem(context.getString(R.string.menu_delete),
                        android.R.drawable.ic_delete, MENU_DELETE);
                iconContextMenu.setOnClickListener(new IconContextMenuOnClickListener() {

                    @Override
                    public void onClick(int menuId) {
                        switch (menuId) {
                            case MENU_DELETE:
                                Uri uri = Uri.withAppendedPath(ContactsProvider.CONTENT_URI, ""
                                        + id);
                                context.getContentResolver().delete(uri, null, null);
                                adapter.removeItem(id);
                                break;

                            default:
                                break;
                        }
                    }
                });
                iconContextMenu.createMenu(null).show();
                return true;
            }
        };
    }

}
