
package com.tonchidot.nfc_contact_exchanger.lib;

import java.util.ArrayList;

import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils.NodeHandler;

import a_vcard.android.provider.Contacts;
import a_vcard.android.syncml.pim.PropertyNode;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.VCardComposer;
import a_vcard.android.syncml.pim.vcard.VCardException;

public class MeCardUtils {
    private static final String REPLACEMENT = "=*.,..";

    public static String fromVCard(String vcard) {
        final StringBuilder meCard = new StringBuilder("MECARD:");
        VCardUtils.doForEachEntry(vcard, new NodeHandler<Void>() {

            @Override
            public Void handle(PropertyNode node) {
                if ("N".equals(node.propName)) {
                    String[] names = node.propValue.split(";");
                    if (names.length > 1) {
                        addLine("N:" + names[0] + "," + names[1]);
                    } else {
                        addLine("N:" + names[0]);
                    }
                } else if ("TEL".equals(node.propName)) {
                    addLine("TEL:" + node.propValue);
                } else if ("EMAIL".equals(node.propName)) {
                    addLine("EMAIL:" + node.propValue);
                } else if ("NOTE".equals(node.propName)) {
                    addLine("NOTE:" + node.propValue);
                } else if ("BDAY".equals(node.propName)) {
                    addLine("BDAY:" + node.propValue.replace("-", "").substring(0, 8));
                    // } else if ("ADR".equals(node.propName)) {
                    // addLine("ADR:" + node.propValue);
                } else if ("URL".equals(node.propName)) {
                    addLine("URL:" + node.propValue.replace(":", "\\:"));
                } else if ("X-ANDROID-CUSTOM".equals(node.propName)) {
                    String[] values = node.propValue.split(";");
                    if (values.length > 1 && "vnd.android.cursor.item/nickname".equals(values[0])) {
                        addLine("NICKNAME:" + values[1]);
                    }
                }
                return null;
            }

            public void addLine(String line) {
                meCard.append(line + ";");
            }
        }, false);
        return meCard.toString() + ";";
    }

    public static String toVCard(String meCard) {
        VCardComposer composer = new VCardComposer();

        ContactStruct contact = new ContactStruct();
        if (!meCard.startsWith("MECARD:")) {
            return null;
        }
        String[] commands = meCard.substring(7).replace("\n", "").split(";");
        for (String string : commands) {
            string = string.replace("\\:", REPLACEMENT);

            String[] parts = string.split(":");
            if (parts.length < 2) {
                continue;
            }
            parts[1] = parts[1].replace(REPLACEMENT, ":");

            if ("N".equals(parts[0])) {
                String[] names = parts[1].split(",");
                if (names.length > 1) {
                    contact.name = names[1] + " " + names[0];
                } else {
                    contact.name = names[0];
                }
            } else if ("TEL".equals(parts[0])) {
                contact.addPhone(Contacts.Phones.TYPE_OTHER, parts[1], null, true);
            } else if ("EMAIL".equals(parts[0])) {
                contact.addContactmethod(Contacts.KIND_EMAIL, Contacts.ContactMethods.TYPE_OTHER,
                        parts[1], "", false);
            } else if ("NOTE".equals(parts[0])) {
                contact.notes.add(parts[1]);
            } else if ("URL".equals(parts[0])) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(parts[1]);
                contact.addExtension(new PropertyNode("URL", parts[1], list, null, null, null, null));
            } else if ("BDAY".equals(parts[0])) {
                String bday = parts[1].substring(0, 4) + "-" + parts[1].substring(4, 6) + "-"
                        + parts[1].substring(6, 8);
                ArrayList<String> list = new ArrayList<String>();
                list.add(bday);
                contact.addExtension(new PropertyNode("BDAY", bday, list, null, null, null, null));
            } else if ("NICKNAME".equals(parts[0])) {
                ArrayList<String> list = new ArrayList<String>();
                list.add(parts[1]);
                contact.addExtension(new PropertyNode("X-ANDROID-CUSTOM", parts[1], list, null,
                        null, null, null));
            }
        }

        try {
            return composer.createVCard(contact, VCardComposer.VERSION_VCARD30_INT);
        } catch (VCardException e) {
            return null;
        }

    }
}
