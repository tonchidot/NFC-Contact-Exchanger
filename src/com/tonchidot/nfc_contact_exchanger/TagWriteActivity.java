
package com.tonchidot.nfc_contact_exchanger;

import java.io.IOException;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.widget.Toast;

import com.tonchidot.nfc_contact_exchanger.lib.Preferences;
import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils;

public class TagWriteActivity extends BaseAnalyticsActivity {

    protected static final String EXTRA_VCARD = "EXTRA_VCARD";

    private String mVcard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.write_tag);

        if (getIntent().hasExtra(EXTRA_VCARD)) {
            mVcard = getIntent().getStringExtra(EXTRA_VCARD);
        } else {
            throw new IllegalArgumentException("Needs EXTRA_VCARD extra!");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableTagWriteMode();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            boolean success = writeTag(getVCardAsNdef(), getVCardWithoutPhotoAsNdef(), detectedTag);
            if (success) {
                tracker.trackEvent(EVENT_CATEGORY_CONTACTS, EVENT_ACTION_WRITE_NFC, "", 1);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.write_tag_success).setCancelable(false)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                TagWriteActivity.this.finish();
                            }
                        });
                builder.create().show();
            }
        }
    }

    private NdefMessage getVCardAsNdef() {
        return VCardUtils.createNdefVCard(mVcard);
    }

    private NdefMessage getVCardWithoutPhotoAsNdef() {
        return VCardUtils.createNdefVCard(VCardUtils.getVCardWithPhotoUrl(this, mVcard,
                new Preferences(this).getPhotoOnlineUrl()));
    }

    private void enableTagWriteMode() {
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] mWriteTagFilters = new IntentFilter[] {
            tagDetected
        };
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        NfcAdapter adapter = manager.getDefaultAdapter();
        adapter.enableForegroundDispatch(this, pendingIntent, mWriteTagFilters, null);
    }

    boolean writeTag(NdefMessage message, NdefMessage messageWithoutPhoto, Tag tag) {
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                return writeToTag(message, messageWithoutPhoto, ndef);
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    if (formatAndWrite(format, message)
                            || formatAndWrite(format, messageWithoutPhoto)) {
                        return true;
                    } else {
                        toast(getString(R.string.write_tag_failed_format));
                        return false;
                    }
                } else {
                    toast(getString(R.string.write_tag_failed_ndef));
                    return false;
                }
            }
        } catch (Exception e) {
            toast(getString(R.string.write_tag_failed));
        }

        return false;
    }

    private boolean formatAndWrite(NdefFormatable format, NdefMessage message)
            throws FormatException {
        try {
            format.connect();
            format.format(message);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean writeToTag(NdefMessage message, NdefMessage messageWithoutPhoto, Ndef ndef)
            throws IOException, FormatException {
        int size = message.toByteArray().length;
        int sizeSmall = messageWithoutPhoto.toByteArray().length;

        ndef.connect();

        if (!ndef.isWritable()) {
            toast("Tag is read-only.");
            return false;
        }

        NdefMessage messageToWrite = message;

        if (ndef.getMaxSize() < size) {
            if (ndef.getMaxSize() < sizeSmall) {
                toast(getResources().getString(R.string.write_tag_failed_size, ndef.getMaxSize(),
                        size));
                return false;
            } else {
                messageToWrite = messageWithoutPhoto;
            }
        }

        ndef.writeNdefMessage(messageToWrite);
        return true;
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
