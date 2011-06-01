
package com.tonchidot.nfc_contact_exchanger.lib;

import android.os.Build;

public abstract class ApiAccessor {
    public static boolean hasNfcSupport() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1;
    }
}
