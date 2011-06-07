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
 
package com.tonchidot.nfc_contact_exchanger.lib;

import java.util.Date;

import com.tonchidot.nfc_contact_exchanger.ContactsProvider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateFormat;

public class Contact {
    public long id;
    public String name;
    public double latitude;
    public double longitude;
    public long timestamp;
    public String vcard;

    public String getFormattedDate(Context context) {
        java.text.DateFormat dateFormat = DateFormat.getMediumDateFormat(context);
        java.text.DateFormat timeFormat = DateFormat.getTimeFormat(context);
        return dateFormat.format(new Date(timestamp)) + " - "
                + timeFormat.format(new Date(timestamp));
    }

    public boolean save(Context context) {
        ContentResolver cp = context.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(ContactsProvider.KEY_NAME, name);
        values.put(ContactsProvider.KEY_DATE, timestamp);
        values.put(ContactsProvider.KEY_LATITUDE, latitude);
        values.put(ContactsProvider.KEY_LONGITUDE, longitude);
        values.put(ContactsProvider.KEY_VCARD, vcard);

        if (id < 1) {
            Uri uri = cp.insert(ContactsProvider.CONTENT_URI, values);
            id = Integer.parseInt(uri.getPathSegments().get(1));
            return id > 0;
        } else {
            Uri uri = Uri.withAppendedPath(ContactsProvider.CONTENT_URI, "" + id);
            int count = cp.update(uri, values, null, null);
            return count == 1;
        }
    }

    public static Contact createFromCursor(Cursor cursor) {
        Contact entry = new Contact();
        entry.id = cursor.getLong(cursor.getColumnIndex(ContactsProvider.KEY_ID));
        entry.name = cursor.getString(cursor.getColumnIndex(ContactsProvider.KEY_NAME));
        entry.timestamp = cursor.getLong(cursor.getColumnIndex(ContactsProvider.KEY_DATE));
        entry.latitude = cursor.getDouble(cursor.getColumnIndex(ContactsProvider.KEY_LATITUDE));
        entry.longitude = cursor.getDouble(cursor.getColumnIndex(ContactsProvider.KEY_LONGITUDE));
        entry.vcard = cursor.getString(cursor.getColumnIndex(ContactsProvider.KEY_VCARD));
        return entry;
    }

}
