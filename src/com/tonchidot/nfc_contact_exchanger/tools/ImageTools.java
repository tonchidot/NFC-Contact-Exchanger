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
 
package com.tonchidot.nfc_contact_exchanger.tools;

import java.io.File;
import java.io.FileOutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.TypedValue;

/**
 * The Class ImageTools which provides different functions for image related
 * processing.
 */
public class ImageTools {
    private static final String TAG = ImageTools.class.getSimpleName();

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

    /**
     * Save bitmap to file.
     * 
     * @param bitmap the bitmap
     * @param saveLocation the save location
     * @return true, if successful
     */
    public static boolean saveBitmap(Bitmap bitmap, String saveLocation) {
        try {
            createDirsOfFile(saveLocation);
            FileOutputStream out = new FileOutputStream(saveLocation);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Error while saving Bitmap (" + saveLocation + ": " + e.getMessage());
            new File(saveLocation).delete();
        }
        return false;

    }

    /**
     * Creates the dirs for a file if they do not yet exist.
     * 
     * @param fileLocation the location for which the directories should be
     *            created
     */
    protected static void createDirsOfFile(String fileLocation) {
        File file = new File(fileLocation);
        file.getParentFile().mkdirs();
    }
}
