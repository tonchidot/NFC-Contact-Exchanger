
package com.tonchidot.nfc_contact_exchanger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.tonchidot.nfc_contact_exchanger.lib.AndroidContactExporter;
import com.tonchidot.nfc_contact_exchanger.lib.ApiAccessor;
import com.tonchidot.nfc_contact_exchanger.lib.Contact;
import com.tonchidot.nfc_contact_exchanger.lib.LocationHelper;
import com.tonchidot.nfc_contact_exchanger.lib.MeCardUtils;
import com.tonchidot.nfc_contact_exchanger.lib.PictureUploader;
import com.tonchidot.nfc_contact_exchanger.lib.Preferences;
import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils;
import com.tonchidot.nfc_contact_exchanger.widgets.BusinessCardWidget;

public class ContacterMainActivity extends BaseAnalyticsActivity {
    private static final String TAG = ContacterMainActivity.class.getSimpleName();

    protected static final int REQUEST_CONTACT = 0;
    protected static final int REQUEST_QR_VCARD = 1;

    private Preferences preferences;
    private String selectedVcardString;

    private BusinessCardWidget businessCard;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        preferences = new Preferences(this);

        businessCard = (BusinessCardWidget) findViewById(R.id.businesscard);
        businessCard.setOnEditClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
                startActivityForResult(intent, REQUEST_CONTACT);
            }
        });

        selectedVcardString = getOwnVCard();
        if (selectedVcardString != null) {
            showOwnContact(selectedVcardString);
        }
        setUpHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (selectedVcardString != null) {
            setUpForgroundNdefPush();
        }
        enableForegroundNfcDispatch();
        handlePossibleNfcMessages(getIntent());
    }

    private void setUpForgroundNdefPush() {
        if (!ApiAccessor.hasNfcSupport())
            return;
        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        if (adapter != null) {
            adapter.enableForegroundNdefPush(this, createNdefWithPhotoUrlForSelectedContact());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handlePossibleNfcMessages(intent);
    }

    @Override
    protected void onPause() {
        if (ApiAccessor.hasNfcSupport()) {
            NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
            NfcAdapter adapter = manager.getDefaultAdapter();
            if (adapter != null) {
                adapter.disableForegroundDispatch(this);
            }
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.top, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!nfcAdapterAvailable()) {
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getItemId() == R.id.menu_write_tag)
                    item.setEnabled(false);
            }
        }
        return true;
    }

    private boolean nfcAdapterAvailable() {
        if (ApiAccessor.hasNfcSupport()) {
            NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
            return manager.getDefaultAdapter() != null;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_write_tag:
                intent = new Intent(ContacterMainActivity.this, TagWriteActivity.class);
                intent.putExtra(TagWriteActivity.EXTRA_VCARD, selectedVcardString);
                startActivity(intent);
                return true;
            case R.id.menu_history:
                intent = new Intent(ContacterMainActivity.this, HistoryActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_qrcode_import:
                // TODO: check if app is even installed, if not, show message
                // and allow to install
                intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.setPackage("com.google.zxing.client.android");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, REQUEST_QR_VCARD);
                tracker.trackEvent(EVENT_CATEGORY_CONTACTS, EVENT_ACTION_SHARE, EVENT_LABEL_QRCODE,
                        1);
                return true;
            case R.id.menu_qrcode_export:
                // TODO: check if app is even installed, if not, show message
                // and allow to install
                intent = new Intent("com.google.zxing.client.android.ENCODE");
                intent.putExtra("ENCODE_TYPE", "TEXT_TYPE");
                intent.putExtra("ENCODE_DATA", MeCardUtils.fromVCard(selectedVcardString));
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONTACT:
                if (resultCode == RESULT_OK) {
                    tracker.trackEvent(EVENT_CATEGORY_CONTACTS, EVENT_ACTION_CHANGE_OWN, "", 1);
                    setOwnContact(data.getData());
                    showOwnContact(selectedVcardString);
                }
                break;
            case REQUEST_QR_VCARD:
                if (resultCode == RESULT_OK) {
                    String contents = data.getStringExtra("SCAN_RESULT");
                    // String format =
                    // data.getStringExtra("SCAN_RESULT_FORMAT");
                    if (contents.startsWith("BEGIN:VCARD")) {
                        handleReceivedVCard(contents, EVENT_LABEL_QRCODE);
                    } else if (contents.startsWith("MECARD:")) {
                        handleReceivedVCard(MeCardUtils.toVCard(contents), EVENT_LABEL_QRCODE);
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void handlePossibleNfcMessages(Intent intent) {
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            NdefMessage[] msgs = getNdefMessages(intent);
            if (msgs != null) {
                NdefMessage msg = msgs[0];
                final String vcardPayload = new String(msg.getRecords()[0].getPayload());
                handleReceivedVCard(vcardPayload, EVENT_LABEL_NFC);
            }
        } else if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            // TODO refactor
            Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            try {
                Ndef ndef = Ndef.get(tagFromIntent);
                ndef.connect();
                NdefMessage msg = ndef.getNdefMessage();
                for (int i = 0; i < msg.getRecords().length; i++) {
                    NdefRecord record = msg.getRecords()[i];
                    String mime = new String(record.getType());
                    String payload = new String(record.getPayload());
                    if (VCardUtils.MIME_TYPE_VCARD.toLowerCase().equals(mime.toLowerCase())) {
                        handleReceivedVCard(payload, EVENT_LABEL_NFC_TAG);
                    }
                    Log.d(TAG, record.toString());
                }
            } catch (IOException e) {
            } catch (FormatException e) {
            }
            Log.d(TAG, "tag:" + tagFromIntent);
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            // TODO: read from felica phone (number?)

            // TODO: even make the app work with felica android phones!
            // http://ap.pitsquare.jp/pc/developers/

            // http://code.google.com/p/nfc-felica/source/browse/nfc-felica/branches/nfc-felica-2.3.3/src/net/kazzz/NFCFeliCaReader.java?r=34
            // intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            // intent.getParcelableExtra("android.nfc.extra.TAG");
            // ft.readWithoutEncryption((byte)0)
            // http://code.google.com/p/nfc-felica/source/browse/nfc-felica/trunk/nfc-felica-lib/src/net/kazzz/felica/FeliCaTag.java?r=21
        }
    }

    private void enableForegroundNfcDispatch() {
        if (!ApiAccessor.hasNfcSupport())
            return;

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (adapter == null)
            return;

        PendingIntent intent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);

        try {
            ndef.addDataType("*/*");
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Unable to speciy */* Mime Type", e);
        }
        IntentFilter[] intentFiltersArray = new IntentFilter[] {
            ndef
        };

        String[][] techListsArray = new String[][] {
                new String[] {
                    Ndef.class.getName()
                }, new String[] {
                    NfcF.class.getName()
                // to read felica
                }
        };

        adapter.enableForegroundDispatch(this, intent, intentFiltersArray, techListsArray);
    }

    NdefMessage[] getNdefMessages(Intent intent) {
        // Parse the intent
        NdefMessage[] msgs = null;
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                // Unknown tag type
                byte[] empty = new byte[] {};
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {
                    record
                });
                msgs = new NdefMessage[] {
                    msg
                };
            }
        }
        return msgs;
    }

    private void setUpHistory() {
        Calendar past = Calendar.getInstance();
        past.add(Calendar.MINUTE, -10);

        Cursor cursor = managedQuery(ContactsProvider.CONTENT_URI, null, ContactsProvider.KEY_DATE
                + ">" + past.getTimeInMillis(), null, ContactsProvider.KEY_DATE + " DESC");

        List<Contact> history = new ArrayList<Contact>();
        while (cursor.moveToNext()) {
            history.add(Contact.createFromCursor(cursor));
        }
        final HistoryAdapter adapter = new HistoryAdapter(this, history);
        ListView historyList = (ListView) findViewById(R.id.list_contacts);
        historyList.setAdapter(adapter);
        historyList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Contact entry = (Contact) adapter.getItem(position);
                Intent intent = new Intent(ContacterMainActivity.this,
                        ContactReceivedActivity.class);
                intent.putExtra(ContactReceivedActivity.EXTRA_HISTORY_ID, entry.id);
                startActivity(intent);
            }
        });
        historyList.setOnItemLongClickListener(HistoryActivity.createHistoryLongClickListener(this,
                adapter));
        getContentResolver().registerContentObserver(ContactsProvider.CONTENT_URI, true,
                changedHandler);
    }

    ContentObserver changedHandler = new ContentObserver(new Handler()) {

        @Override
        public void onChange(boolean selfChange) {
            setUpHistory();
        }

    };

    private void handleReceivedVCard(String vcardPayload, String receivedSourceLabel) {
        tracker.trackEvent(EVENT_CATEGORY_CONTACTS, EVENT_ACTION_RECEIVE, receivedSourceLabel, 1);

        // Consume this intent, so it won't do it again on next resume
        setIntent(new Intent());

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(100);

        saveToHistory(vcardPayload);
    }

    private String getOwnVCard() {
        String vcard = preferences.getOwnVCard();
        if (vcard != null) {
            return vcard;
        } else {
            Uri contact = findOwnContactThroughContactManager();
            if (contact != null) {
                setOwnContact(contact);
                return selectedVcardString;
            }
        }
        return null;
    }

    private Uri findOwnContactThroughContactManager() {
        Account[] accounts = AccountManager.get(getApplicationContext()).getAccounts();
        for (Account account : accounts) {
            if (account.name.contains("@")) {
                Uri uri = findContactWithEmail(account.name);
                if (uri != null) {
                    return uri;
                }
            }
        }
        return null;
    }

    private Uri findContactWithEmail(String email) {
        Cursor cursor = managedQuery(Email.CONTENT_URI, new String[] {
                Phone.CONTACT_ID, Email.DATA1, Email.TYPE, Email.LABEL
        }, Email.DATA1 + "='" + email + "'", null, null);
        if (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            return ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        }
        return null;
    }

    private void setOwnContact(Uri contactUri) {
        AndroidContactExporter exporter = new AndroidContactExporter(getApplicationContext());
        selectedVcardString = exporter.getVCardStringFromUri(contactUri);

        preferences.saveOwnVCard(selectedVcardString);
        preferences.saveOwnVCardContactLink(contactUri);
        handlePictureUpload();
    }

    private void handlePictureUpload() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                byte[] pictureData = VCardUtils.getPictureData(selectedVcardString);
                String url = null;
                if (pictureData != null) {
                    PictureUploader uploader = new PictureUploader();
                    url = uploader.uploadPicture(pictureData);
                }
                return url;
            }

            @Override
            protected void onPostExecute(String url) {
                preferences.savePhotoOnlineUrl(url);
                try {
                    setUpForgroundNdefPush();
                } catch (Exception e) {
                    // if not resumed, this might happen...
                }
            }
        }.execute();
    }

    private void showOwnContact(String vcard) {
        businessCard.setVCard(vcard);
        Drawable vcardImg = VCardUtils.getDrawable(vcard);
        if (vcardImg != null) {
            businessCard.setContactImage(vcardImg);
        } else {
            businessCard.setContactImage(getResources().getDrawable(R.drawable.vcard_default));
        }
    }

    private NdefMessage createNdefWithPhotoUrlForSelectedContact() {
        return VCardUtils.createNdefVCard(VCardUtils.getVCardWithPhotoUrl(this,
                selectedVcardString, preferences.getPhotoOnlineUrl()));
    }

    protected void saveToHistory(String vcard) {
        Contact entry = new Contact();
        entry.name = VCardUtils.getName(vcard);
        entry.timestamp = Calendar.getInstance().getTimeInMillis();
        // TODO: do latitude and longitude!
        Location location = new LocationHelper(getApplicationContext()).getLastKnownLocation();
        if (location != null) {
            entry.latitude = location.getLatitude();
            entry.longitude = location.getLongitude();
        }
        entry.vcard = vcard;
        entry.save(getApplicationContext());
    }
}
