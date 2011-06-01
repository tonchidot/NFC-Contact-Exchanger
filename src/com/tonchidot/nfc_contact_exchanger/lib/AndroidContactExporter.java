
package com.tonchidot.nfc_contact_exchanger.lib;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;

import com.tonchidot.nfc_contact_exchanger.R;

public class AndroidContactExporter {
    private Context context;
    private ContentResolver resolver;

    public AndroidContactExporter(Context context) {
        this.context = context;
        this.resolver = context.getContentResolver();
    }

    public String getVCardStringFromUri(Uri contactUri) {
        // TODO: use this and maybe remove pic? or resize pic? at least
        // refactor! :) only remove pic when writing to tag!
        String lookupKey = getContactLookupKey(contactUri);
        try {
            AssetFileDescriptor afd = context.getContentResolver().openAssetFileDescriptor(
                    Uri.withAppendedPath(Contacts.CONTENT_VCARD_URI, lookupKey), "r");
            FileInputStream input = afd.createInputStream();

            int ch;
            StringBuffer strContent = new StringBuffer("");
            while ((ch = input.read()) != -1)
                strContent.append((char) ch);

            input.close();

            // TODO: add things that are not added by Android:
            // TWITTER
            // FACEBOOK
            // SKYPE?
            // PHOTO from SKYPE/FACEBOOK

            return strContent.toString();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            System.err.println("Could not read vcard file: [" + e.getClass().getSimpleName() + "]"
                    + e.getMessage());
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    private String getContactLookupKey(Uri contactUri) {
        Cursor cursor = resolver.query(contactUri, new String[] {
            Contacts.LOOKUP_KEY
        }, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } finally {
            cursor.close();
        }
        return null;

    }

    public Drawable getContactDrawable(Uri contactUri) {
        long contactId = -1;
        // Load the display name for the specified person
        Cursor cursor = resolver.query(contactUri, new String[] {
            Contacts._ID
        }, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                contactId = cursor.getLong(0);
            }
        } finally {
            cursor.close();
        }

        // TODO: do this better (default image and some not working?)
        // those which come from facebook do not work.. some way to get those?
        // see contacts app :)
        Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId);
        InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(resolver, uri);
        if (input == null) {
            return context.getResources().getDrawable(R.drawable.vcard_default);
        } else {
            return new BitmapDrawable(BitmapFactory.decodeStream(input));
        }
    }
}
