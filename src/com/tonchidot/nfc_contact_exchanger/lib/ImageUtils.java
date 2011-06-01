
package com.tonchidot.nfc_contact_exchanger.lib;

import android.content.Context;
import android.util.TypedValue;

public class ImageUtils {

    /**
     * Gets the pixels for dip.
     * 
     * @param context the context
     * @param dip the dip
     * @return the pixels for dip
     */
    public static int getPixelsForDip(Context context, int dip) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) dip, context
                .getResources().getDisplayMetrics());
    }
}
