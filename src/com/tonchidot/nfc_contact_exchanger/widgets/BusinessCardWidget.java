/**
 * Copyright 2010 Tonchidot Corporation. All rights reserved.
 */

package com.tonchidot.nfc_contact_exchanger.widgets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import a_vcard.android.syncml.pim.PropertyNode;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.tonchidot.nfc_contact_exchanger.R;
import com.tonchidot.nfc_contact_exchanger.lib.ImageDownloaderTask;
import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils;
import com.tonchidot.nfc_contact_exchanger.lib.VCardUtils.NodeHandler;
import com.tonchidot.nfc_contact_exchanger.widgets.IconContextMenu.IconContextMenuOnClickListener;

/**
 * The Class BusinessCardRelativeLayout.
 */
public class BusinessCardWidget extends RelativeLayout implements OnClickListener,
        IconContextMenuOnClickListener {

    private IconContextMenu iconContextMenu;
    private List<Intent> contextMenuIntents;
    private LinearLayout mDataList;

    private boolean showEditButton;

    /**
     * Instantiates a new business card relative layout.
     * 
     * @param context the context
     */
    public BusinessCardWidget(Context context) {
        super(context);
    }

    /**
     * Instantiates a new business card relative layout.
     * 
     * @param context the context
     * @param attrs the attrs
     */
    public BusinessCardWidget(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BusinessCardWidget);
        showEditButton = a.getBoolean(R.styleable.BusinessCardWidget_showEditButton, false);
        a.recycle();
    }

    /**
     * Instantiates a new aspect ratio image view.
     * 
     * @param context the context
     * @param attrs the attrs
     * @param defStyle the def style
     */
    public BusinessCardWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /*
     * (non-Javadoc)
     * @see android.view.View#onFinishInflate()
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
                R.layout.businesscard_layout, this);
        mDataList = (LinearLayout) findViewById(R.id.layout_profile_data);
        if (!showEditButton) {
            findViewById(R.id.button_contact).setVisibility(View.GONE);
        }

        findViewById(R.id.layout_profile_data).setOnClickListener(this);
        findViewById(R.id.image_contact).setOnClickListener(this);

        iconContextMenu = new IconContextMenu(getContext());
        iconContextMenu.setOnClickListener(this);
        contextMenuIntents = new ArrayList<Intent>();
    }

    @Override
    public void onClick(View v) {
        iconContextMenu.createMenu(null).show();
    }

    @Override
    public void onClick(int menuId) {
        getContext().startActivity(contextMenuIntents.get(menuId));
    }

    public void setOnEditClickListener(OnClickListener listener) {
        ImageButton contactChooserButton = (ImageButton) findViewById(R.id.button_contact);
        contactChooserButton.setOnClickListener(listener);
    }

    public void setVCard(String vcard) {
        mDataList.removeAllViews();
        iconContextMenu.clearItems();
        contextMenuIntents.clear();

        addNameView(vcard);

        VCardUtils.doForEachEntry(vcard, new NodeHandler<Void>() {

            @Override
            public Void handle(PropertyNode node) {
                // TODO: ListView? :) with types and different designs and
                // reactions on clicking :)
                if ("TEL".equals(node.propName)) {
                    if (node.paramMap_TYPE.contains("CELL")) {
                        addPhone(R.drawable.icon_cell, node.propValue);
                    } else {
                        addPhone(R.drawable.icon_phone, node.propValue);
                    }
                } else if ("EMAIL".equals(node.propName)) {
                    addEmail(node.propValue);
                } else if ("ADR".equals(node.propName)) {
                    addAddress(node.propValue);
                } else if ("BDAY".equals(node.propName)) {
                    java.text.DateFormat dateFormat = DateFormat.getLongDateFormat(getContext());
                    Calendar calendar = Calendar.getInstance();
                    String[] dates = node.propValue.split("-");
                    calendar.set(Integer.parseInt(dates[0]), Integer.parseInt(dates[1]) - 1,
                            Integer.parseInt(dates[2]));
                    addEntry(R.drawable.icon_bday, dateFormat.format(calendar.getTime()));
                } else if ("URL".equals(node.propName)) {
                    if (node.propValue.contains("twitter.com/")) {
                        int pos = node.propValue.lastIndexOf('/');
                        addTwitter(node.propValue.substring(pos + 1));
                    } else {
                        addUrl(node.propValue);
                    }
                } else if ("PHOTO".equals(node.propName)) {
                    if ("URL".equals(node.paramMap.getAsString("VALUE"))) {
                        showPicture(node.propValue);
                    }
                } else if ("NOTE".equals(node.propName)) {
                    addEntry(R.drawable.icon_note, node.propValue);
                } else if ("X-SKYPE-USERNAME".equals(node.propName)) {
                    addEntry(R.drawable.icon_skype, node.propValue);
                }
                return null;
            }
        }, false);
    }

    private void showPicture(final String url) {
        new ImageDownloaderTask(url) {
            @Override
            protected void onPostExecute(Drawable result) {
                if (result != null) {
                    setContactImage(result);
                }
            }
        }.execute();
    }

    public Object fetch(String address) throws MalformedURLException, IOException {
        URL url = new URL(address);
        Object content = url.getContent();
        return content;
    }

    public void setContactImage(Drawable drawable) {
        ImageView image = (ImageView) findViewById(R.id.image_contact);
        image.setImageDrawable(drawable);
    }

    private void addNameView(String vcard) {
        TextView nameText = new TextView(getContext());
        nameText.setTextAppearance(getContext(), R.style.TextTitle);
        nameText.setText(VCardUtils.getName(vcard));
        mDataList.addView(nameText);
    }

    private void addPhone(int drawableResource, String number) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + number.replace(" ", "")));

        addButtonEntry(drawableResource, number, intent);
    }

    private void addAddress(String address) {
        String[] parts = address.split(";");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                sb.append(part + "\n");
            }
        }
        if (sb.toString().endsWith("\n")) {
            sb.delete(sb.length() - 1, sb.length());
        }

        address = sb.toString();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://maps.google.com/?q=" + address));

        addButtonEntry(R.drawable.icon_address, address, intent);
    }

    private void addTwitter(String twitterUser) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://twitter.com/" + twitterUser));

        addButtonEntry(R.drawable.icon_twitter, twitterUser, intent);
    }

    private void addEmail(String email) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
            email
        });

        addButtonEntry(R.drawable.icon_email, email, emailIntent);
    }

    private void addUrl(String url) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));

        addButtonEntry(R.drawable.icon_url, url, intent);
    }

    private void addEntry(int iconResource, String text) {
        addButtonEntry(iconResource, text, null);
    }

    private void addButtonEntry(int iconResource, String text, Intent intent) {
        LayoutInflater inflater = ((LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE));
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.businesscard_line, null);

        ImageView image = (ImageView) layout.findViewById(R.id.image);
        image.setImageResource(iconResource);

        TextView textView = (TextView) layout.findViewById(R.id.text);
        textView.setText(text);
        if (!text.contains("\n")) {
            textView.setMaxLines(1);
            textView.setLines(1);
            textView.setHorizontallyScrolling(true);
            textView.setEllipsize(TruncateAt.END);
        }
        mDataList.addView(layout);

        if (intent != null) {
            iconContextMenu.addItem(text, iconResource, contextMenuIntents.size());
            contextMenuIntents.add(intent);
        }
    }

    public byte[] getJpegBytes() {
        ImageView imageView = (ImageView) findViewById(R.id.image_contact);
        Drawable drawable = imageView.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();
        } else {
            return null;
        }

    }
}
