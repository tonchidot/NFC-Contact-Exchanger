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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.tonchidot.nfc_contact_exchanger.lib.Contact;
import com.tonchidot.nfc_contact_exchanger.lib.ImageDownloaderTask;
import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils;
import com.tonchidot.nfc_contact_exchanger.widgets.BusinessCardWidget;

public class ContactReceivedActivity extends BaseAnalyticsActivity {
    private static final String TAG = ContactReceivedActivity.class.getSimpleName();

    public static final String EXTRA_HISTORY_ID = "EXTRA_HISTORY_ID";

    private Contact entry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.received_contact);

        if (getIntent().hasExtra(EXTRA_HISTORY_ID)) {
            long id = getIntent().getLongExtra(EXTRA_HISTORY_ID, 0);
            Uri uri = Uri.withAppendedPath(ContactsProvider.CONTENT_URI, "" + id);
            Cursor cursor = managedQuery(uri, null, null, null, null);
            if (cursor.moveToNext()) {
                entry = Contact.createFromCursor(cursor);
            } else {
                throw new IllegalArgumentException("Given History id does not exist in Database");
            }
        } else {
            throw new IllegalArgumentException("Needs extra EXTRA_HISTORY_ID!");
        }

        showBusinessCard();
        setUpButtons();
        showMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_entry, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete_entry:
                Uri uri = Uri.withAppendedPath(ContactsProvider.CONTENT_URI, "" + entry.id);
                getContentResolver().delete(uri, null, null);
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showBusinessCard() {
        BusinessCardWidget businessCard = (BusinessCardWidget) findViewById(R.id.businesscard);
        businessCard.setVCard(entry.vcard);

        TextView contactDateText = (TextView) findViewById(R.id.text_meet_date);
        contactDateText.setText(entry.getFormattedDate(this));
    }

    private void setUpButtons() {
        Button addContactButton = (Button) findViewById(R.id.button_add);
        addContactButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                tracker.trackEvent(EVENT_CATEGORY_CONTACTS, EVENT_ACTION_ADD, "", 1);

                // add picture as base64 if only url is there :)
                BusinessCardWidget businessCard = (BusinessCardWidget) findViewById(R.id.businesscard);
                byte[] bytes = businessCard.getJpegBytes();
                if (bytes != null) {
                    String base64Image = Base64.encodeToString(bytes, 0);
                    String regularExpression = "PHOTO;VALUE=URL[:;][^\\n]+(\\n [^\\n])*+\\n";
                    String replaceWith = "PHOTO;ENCODING=BASE64;JPEG:" + base64Image + "\n";
                    entry.vcard = entry.vcard.replaceAll(regularExpression, replaceWith);
                }

                String writtenFile = writeToSdCard("temp.vcard", entry.vcard);
                if (writtenFile != null) {
                    openVCardFile(writtenFile);
                }
            }
        });
    }

    private void showMap() {
        TextView contactPlaceText = (TextView) findViewById(R.id.text_meet_place);
        final OnClickListener openMapClick = new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://maps.google.com/?q=" + entry.latitude + ","
                        + entry.longitude + " (" + entry.name + ")"));
                startActivity(intent);
            }
        };
        contactPlaceText.setOnClickListener(openMapClick);
        try {
            // TODO: do this in asynctask and get a nicer address (locality if
            // set?)
            Geocoder gcd = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = gcd.getFromLocation(entry.latitude, entry.longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(address.getFeatureName())) {
                    sb.append(address.getFeatureName() + ", ");
                }
                if (!TextUtils.isEmpty(address.getLocality())) {
                    sb.append(address.getLocality() + ", ");
                }
                if (!TextUtils.isEmpty(address.getCountryName())) {
                    sb.append(address.getCountryName());
                }
                contactPlaceText.setText(sb.toString());
            }
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }

        final ImageView mapImage = (ImageView) findViewById(R.id.image_map);
        String loc = entry.latitude + "," + entry.longitude;
        String url = "http://maps.google.com/maps/api/staticmap?center=" + loc
                + "&zoom=17&size=400x300&sensor=true&markers=" + loc;
        new ImageDownloaderTask(url) {

            @Override
            protected void onPostExecute(Drawable result) {
                mapImage.setImageDrawable(result);
                mapImage.setOnClickListener(openMapClick);
            }

        }.execute();
    }

    private void openVCardFile(String writtenFile) {
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
        Uri data = Uri.parse("file://" + writtenFile);
        String type = VCardUtils.MIME_TYPE_VCARD.toLowerCase();
        intent.setDataAndType(data, type);
        startActivity(intent);
    }

    private String writeToSdCard(String fileName, String fileContent) {
        // TODO: maybe write this nicer (save file in own folder or as nicer
        // temp file and maybe delete after import?)

        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but
            // all we need
            // to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if (mExternalStorageAvailable && mExternalStorageWriteable) {
            try {
                File root = Environment.getExternalStorageDirectory();
                File file = new File(root, fileName);
                FileWriter fileWriter = new FileWriter(file);
                BufferedWriter out = new BufferedWriter(fileWriter);
                out.write(fileContent);
                out.close();
                return file.getAbsolutePath();
            } catch (IOException e) {
                Log.e(TAG, "Could not write file " + e.getMessage());
            }
        }
        return null;
    }
}
