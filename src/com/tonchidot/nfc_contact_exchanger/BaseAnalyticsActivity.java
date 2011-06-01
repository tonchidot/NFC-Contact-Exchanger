
package com.tonchidot.nfc_contact_exchanger;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

public class BaseAnalyticsActivity extends Activity {

    protected final static String EVENT_CATEGORY_CONTACTS = "contacts";

    protected final static String EVENT_ACTION_RECEIVE = "receive";
    protected static final String EVENT_ACTION_ADD = "add";
    protected static final String EVENT_ACTION_SHARE = "share";
    protected static final String EVENT_ACTION_WRITE_NFC = "write_nfc";
    protected static final String EVENT_ACTION_CHANGE_OWN = "change_own_contact";

    protected final static String EVENT_LABEL_NFC = "nfc";
    protected final static String EVENT_LABEL_NFC_TAG = "tag";
    protected final static String EVENT_LABEL_QRCODE = "qrcode";

    protected GoogleAnalyticsTracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.start(getString(R.string.ga_api_key), 20, this);
        // tracker.start(getString(R.string.ga_api_key), 1, this);
        // tracker.setDebug(true);
        // tracker.setDryRun(true);

        tracker.trackPageView("/" + this.getLocalClassName());
    }

}
