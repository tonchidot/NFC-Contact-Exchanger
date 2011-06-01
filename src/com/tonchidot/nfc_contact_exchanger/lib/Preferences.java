
package com.tonchidot.nfc_contact_exchanger.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.preference.PreferenceManager;

public class Preferences {
    private static final String PREF_OWN_VCARD = "OWN_VCARD";
    private static final String PREF_OWN_VCARD_CONTACT_LINK = "OWN_VCARD_CONTACT_LINK";
    private static final String PREF_PHOTO_URL = "PHOTO_URL";

    private Context context;

    public Preferences(Context context) {
        this.context = context;
    }

    public String getOwnVCard() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_OWN_VCARD, null);
    }

    public void saveOwnVCard(String data) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = prefs.edit();
        edit.putString(PREF_OWN_VCARD, data.toString());
        edit.commit();
    }

    public Uri getOwnVCardContactLink() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String uri = prefs.getString(PREF_OWN_VCARD_CONTACT_LINK, null);
        return (uri != null) ? Uri.parse(uri) : null;
    }

    public void saveOwnVCardContactLink(Uri data) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = prefs.edit();
        edit.putString(PREF_OWN_VCARD_CONTACT_LINK, data.toString());
        edit.commit();
    }

    public void savePhotoOnlineUrl(String url) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        Editor edit = prefs.edit();
        edit.putString(PREF_PHOTO_URL, url);
        edit.commit();
    }

    public String getPhotoOnlineUrl() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_PHOTO_URL, null);
    }
}
