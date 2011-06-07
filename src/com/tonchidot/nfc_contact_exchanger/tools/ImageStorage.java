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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import com.tonchidot.nfc_contact_exchanger.lib.Contact;
import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils;

/**
 * This class provides easy access to images. Giving the model object it will
 * return either the defined image in the app, or already downloaded image. If
 * the image is not yet downloaded, it will be downloaded and set to the
 * ImageView after download. It will consider, that the same ImageView can be
 * recycled to be used with another object and won't set the picture if that is
 * the case.
 */
public class ImageStorage {

    private static final String TAG = ImageStorage.class.getSimpleName();

    /** The Constant FOLDER_DOMO. */
    public static final String APP_FOLDER = Environment.getExternalStorageDirectory()
            + "/nfc_contact_exchanger/";

    /** The Constant FOLDER_TEMP. */
    public static final String FOLDER_TEMP = APP_FOLDER + ".tmp/";

    private static final String FOLDER_CONTACTS = APP_FOLDER + ".contacts/";
    private final Context context;

    /**
     * Instantiates a new image storage.
     * 
     * @param context the context
     */
    public ImageStorage(Context context) {
        this.context = context;
    }

    /**
     * Display image for a given Contact.
     * 
     * @param imageView the ImageView in which the image should be displayed.
     * @param contact the contact for which the image is requested
     */
    public void displayImageFor(ImageView imageView, Contact contact) {
        displayImageFor(imageView, FOLDER_CONTACTS + contact.id + ".png",
                VCardUtils.getPhotoUrl(contact.vcard));
    }

    /**
     * Display image for the given path or downloadUrl. If image is already
     * available, show immediatly. Otherwise download and show it after
     * download.
     * 
     * @param imageView the ImageView in which the image should be displayed.
     * @param path the path where the image should be stored
     * @param downloadUrl the url to the image on the web
     */
    private void displayImageFor(ImageView imageView, String path, String downloadUrl) {
        ImageDownloader.stopTasksFor(imageView);

        if (fileExistsOnSdCard(path)) {
            setFromSdCardImage(imageView, path);
        } else {
            if (!TextUtils.isEmpty(downloadUrl)) {
                downloadImage(imageView, path, downloadUrl);
            }
        }
    }

    /**
     * Checks if the file exists.
     * 
     * @param path the path to the file
     * @return true, if the file exists
     */
    private boolean fileExistsOnSdCard(String path) {
        String filePath = path;
        return new File(filePath).exists();
    }

    /**
     * Sets the ImageView with the image from sd card.
     * 
     * @param imageView the ImageView in which the image should be displayed.
     * @param path the path to the image
     */
    private void setFromSdCardImage(ImageView imageView, String path) {
        String filePath = path;
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(View.VISIBLE);
    }

    /**
     * Download image from the URL, save it and show it in the given ImageView.
     * 
     * @param imageView the ImageView in which the image should be displayed.
     * @param path the path where the image should be stored
     * @param downloadUrl the url from which the image should be downloaded
     * @param doSquare true, if the image should be squared before saving
     */
    private void downloadImage(final ImageView imageView, final String path,
            final String downloadUrl) {
        ImageDownloader.runInQueue(imageView, path, downloadUrl);
    }

}
