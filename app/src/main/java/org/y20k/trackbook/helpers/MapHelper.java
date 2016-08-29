/**
 * MapHelper.java
 * Implements the MapHelper class
 * A MapHelper offers helper methods for dealing with Trackbook's map
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */

package org.y20k.trackbook.helpers;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.support.v7.widget.AppCompatDrawableManager;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.y20k.trackbook.R;

import java.util.ArrayList;


/**
 * MapHelper class
 */
public final class MapHelper {

    /* Define log tag */
    private static final String LOG_TAG = MapHelper.class.getSimpleName();


    /* Creates icon overlay for current position */
    public static ItemizedIconOverlay createMyLocationOverlay(Context context, Location currentBestLocation) {

        final GeoPoint position = new GeoPoint(currentBestLocation.getLatitude(), currentBestLocation.getLongitude());
        final ArrayList<OverlayItem> overlayItems = new ArrayList<>();

        // create marker
        Drawable newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_loacation_dot_blue_24dp);
        OverlayItem overlayItem = new OverlayItem(context.getString(R.string.marker_my_location_title), context.getString(R.string.marker_my_location_description), position);
        overlayItem.setMarker(newMarker);
        overlayItems.add(overlayItem);

        // create overlay
        ItemizedIconOverlay myLocationOverlay = new ItemizedIconOverlay<>(overlayItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        LogHelper.v(LOG_TAG, "Tap on the My Location dot icon detected.");
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        LogHelper.v(LOG_TAG, "Long press on the My Location dot icon detected.");
                        return true;
                    }
                }, context);

        // return overlay for current position
        return myLocationOverlay;
    }

}