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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import a_vcard.android.syncml.pim.PropertyNode;
import a_vcard.android.syncml.pim.VDataBuilder;
import a_vcard.android.syncml.pim.VNode;
import a_vcard.android.syncml.pim.vcard.VCardException;
import a_vcard.android.syncml.pim.vcard.VCardParser;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.text.TextUtils;
import android.util.Base64;

public class VCardUtils {
    public static final String MIME_TYPE_VCARD = "text/x-vCard";

    public static String removePhotoFromVCard(String vcard) {
        String line;
        StringBuilder newVCard = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(vcard));
        try {
            String photoLineStart = "PHOTO;ENCODING=";
            boolean removingPhotoLines = false;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(photoLineStart)) {
                    removingPhotoLines = true;
                }

                if (removingPhotoLines
                        && (line.startsWith(" ") || line.startsWith(photoLineStart) || TextUtils
                                .isEmpty(line))) {
                    // do not add this line
                    continue;
                }

                newVCard.append(line + "\r\n");
            }

        } catch (IOException e) {
            return vcard;
        }
        return newVCard.toString();
    }

    public static NdefMessage createNdefVCard(String vcard) {
        NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA, MIME_TYPE_VCARD.getBytes(),
                new byte[] {}, vcard.getBytes());
        return new NdefMessage(new NdefRecord[] {
            record
        });
    }

    public static String getName(String vcard) {
        return doForEachEntry(vcard, new NodeHandler<String>() {

            @Override
            public String handle(PropertyNode node) {
                if ("FN".equals(node.propName)) {
                    return node.propValue;
                }
                return null;
            }
        });
    }

    public static byte[] getPictureData(String vcard) {
        return doForEachEntry(vcard, new NodeHandler<byte[]>() {

            @Override
            public byte[] handle(PropertyNode node) {
                if ("PHOTO".equals(node.propName)) {
                    if (node.paramMap.containsKey("ENCODING")
                            && "BASE64".equals(node.paramMap.getAsString("ENCODING"))) {
                        return Base64.decode(node.propValue.replace(" ", ""), 0);
                    }
                }
                return null;
            }
        });
    }

    public static String getPhotoUrl(String vcard) {
        return doForEachEntry(vcard, new NodeHandler<String>() {

            @Override
            public String handle(PropertyNode node) {
                if ("PHOTO".equals(node.propName)) {
                    if ("URL".equals(node.paramMap.getAsString("VALUE"))) {
                        return node.propValue;
                    }
                }
                return null;
            }
        });
    }

    public static <T> T doForEachEntry(String vcard, NodeHandler<T> handler) {
        return doForEachEntry(vcard, handler, true);
    }

    public static <T> T doForEachEntry(String vcard, NodeHandler<T> handler,
            boolean stopAfterValidReturn) {
        try {
            VCardParser parser = new VCardParser();
            VDataBuilder builder = new VDataBuilder();

            boolean parsed;
            parsed = parser.parse(vcard, "UTF-8", builder);
            if (!parsed) {
                throw new VCardException("Could not parse vcard!");
            }

            // get all parsed contacts
            List<VNode> pimContacts = builder.vNodeList;

            // do something for all the contacts
            for (VNode contact : pimContacts) {
                ArrayList<PropertyNode> props = contact.propList;

                // contact name - FN property
                for (PropertyNode prop : props) {
                    T result = handler.handle(prop);
                    if (stopAfterValidReturn && result != null) {
                        return result;
                    }
                }
            }

        } catch (VCardException e) {
        } catch (IOException e) {
        }
        return null;
    }

    public static interface NodeHandler<T> {
        T handle(PropertyNode node);
    }

    public static Drawable getDrawable(String vcard) {
        byte[] data = getPictureData(vcard);
        if (data != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap != null) {
                return new BitmapDrawable(bitmap);
            }
        }
        return null;
    }

    public static String getVCardWithPhotoUrl(Context context, String vcard, String photoUrl) {
        String newVcard = VCardUtils.removePhotoFromVCard(vcard);
        if (photoUrl != null) {
            newVcard = newVcard.replace("END:VCARD", "PHOTO;VALUE=URL:" + photoUrl + "\nEND:VCARD");
        }
        return newVcard;
    }
}
