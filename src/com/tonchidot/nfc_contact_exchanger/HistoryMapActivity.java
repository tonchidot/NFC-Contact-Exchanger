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
 
package com.tonchidot.nfc_contact_exchanger;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.tonchidot.nfc_contact_exchanger.lib.Contact;
import com.tonchidot.nfc_contact_exchanger.lib.LocationHelper;

public class HistoryMapActivity extends MapActivity {
    List<Overlay> mapOverlays;
    Drawable drawable;
    ContactItemizedOverlay itemizedOverlay;
    GoogleAnalyticsTracker tracker;

    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.start(getString(R.string.ga_api_key), 20, this);

        setContentView(R.layout.history_map);

        MapView mapView = (MapView) findViewById(R.id.mapview);
        mapView.setBuiltInZoomControls(true);
        MapController controller = mapView.getController();
        controller.setZoom(8);
        Location lastLocation = new LocationHelper(this).getLastKnownLocation();
        controller.setCenter(new GeoPoint((int) (lastLocation.getLatitude() * 1E6),
                (int) (lastLocation.getLongitude() * 1E6)));

        mapOverlays = mapView.getOverlays();
        drawable = this.getResources().getDrawable(R.drawable.geopin);
        itemizedOverlay = new ContactItemizedOverlay(drawable);

        Cursor cursor = managedQuery(ContactsProvider.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            Contact entry = Contact.createFromCursor(cursor);
            GeoPoint point = new GeoPoint((int) (entry.latitude * 1E6),
                    (int) (entry.longitude * 1E6));
            OverlayItem overlayitem = new OverlayItem(point, entry.name, "" + entry.id);
            itemizedOverlay.addOverlay(overlayitem);
        }
        cursor.close();

        mapOverlays.add(itemizedOverlay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        tracker.trackPageView("/" + this.getLocalClassName());
    }

    private class ContactItemizedOverlay extends ItemizedOverlay<OverlayItem> {

        private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

        @Override
        protected boolean onTap(int arg0) {
            long id = Long.parseLong(mOverlays.get(arg0).getSnippet());
            Intent intent = new Intent(HistoryMapActivity.this, ContactReceivedActivity.class);
            intent.putExtra(ContactReceivedActivity.EXTRA_HISTORY_ID, id);
            startActivity(intent);
            return super.onTap(arg0);
        }

        public ContactItemizedOverlay(Drawable defaultMarker) {
            super(boundCenterBottom(defaultMarker));
        }

        public void addOverlay(OverlayItem overlay) {
            mOverlays.add(overlay);
            populate();
        }

        @Override
        protected OverlayItem createItem(int i) {
            return mOverlays.get(i);
        }

        @Override
        public int size() {
            return mOverlays.size();
        }

    }
}
