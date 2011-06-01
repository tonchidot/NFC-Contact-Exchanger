
package com.tonchidot.nfc_contact_exchanger.lib;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

public class ImageDownloaderTask extends AsyncTask<String, Void, Drawable> {
    private String url;

    public ImageDownloaderTask(String url) {
        this.url = url;
    }

    @Override
    protected Drawable doInBackground(String... params) {
        try {
            URL theUrl = new URL(url);
            Object content = theUrl.getContent();
            InputStream is = (InputStream) content;
            Drawable d = Drawable.createFromStream(is, "src");
            return d;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
