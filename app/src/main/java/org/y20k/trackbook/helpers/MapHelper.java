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
import android.widget.Toast;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.y20k.trackbook.R;
import org.y20k.trackbook.core.Track;
import org.y20k.trackbook.core.WayPoint;

import java.util.ArrayList;
import java.util.List;


/**
 * MapHelper class
 */
public final class MapHelper {

    /* Define log tag */
    private static final String LOG_TAG = MapHelper.class.getSimpleName();


    /* Creates icon overlay for current position (used in MainActivity Fragment) */
    public static ItemizedIconOverlay createMyLocationOverlay(final Context context, Location currentBestLocation, boolean locationIsNew) {

        final ArrayList<OverlayItem> overlayItems = new ArrayList<>();

        // create marker
        Drawable newMarker;
        if (locationIsNew) {
            newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_dot_blue_24dp);
        } else {
            newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_dot_grey_24dp);
        }
        final GeoPoint position = new GeoPoint(currentBestLocation.getLatitude(), currentBestLocation.getLongitude());
        OverlayItem overlayItem = createOverlayItem(context, currentBestLocation);
        overlayItem.setMarker(newMarker);

        // add marker to list of overlay items
        overlayItems.add(overlayItem);

        // create and return overlay for current position
        return new ItemizedIconOverlay<>(overlayItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        Toast.makeText(context, item.getTitle() + " | " + item.getSnippet(), Toast.LENGTH_LONG).show();
                        LogHelper.v(LOG_TAG, "Tap on the My Location dot icon detected.");
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        LogHelper.v(LOG_TAG, "Long press on the My Location dot icon detected.");
                        return true;
                    }
                }, context);
    }


    /* Creates icon overlay for track */
    public static ItemizedIconOverlay createTrackOverlay(final Context context, Track track, boolean trackingActive){

        WayPoint wayPoint;
        boolean currentPosition;
        final int trackSize = track.getSize();
        final List<WayPoint> wayPoints = track.getWayPoints();
        final ArrayList<OverlayItem> overlayItems = new ArrayList<>();

        for (int i = 0 ; i < track.getSize() ; i++) {
            // get WayPoint and check if it is current position
            wayPoint = wayPoints.get(i);
            currentPosition = i == trackSize - 1;

            // create marker
            Drawable newMarker;

            // CASE 1: Tracking active and WayPoint is not current position
            if (trackingActive && !currentPosition) {
                if (wayPoint.getIsStopOver()) {
                    // stop over marker
                    newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_crumb_grey_24dp);
                } else {
                    // default marker for this case
                    newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_crumb_red_24dp);
                }
            }

            // CASE 2: Tracking active and WayPoint is current position
            else if (trackingActive && currentPosition) {
                if (wayPoint.getIsStopOver()) {
                    // stop over marker
                    newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_dot_grey_24dp);
                } else {
                    // default marker for this case
                    newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_dot_red_24dp);
                }
            }

            // CASE 3: Tracking not active and WayPoint is not current position
            else if (!trackingActive && !currentPosition) {
                if (wayPoint.getIsStopOver()) {
                    // stop over marker
                    newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_crumb_grey_24dp);
                } else {
                    // default marker for this case
                    newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_crumb_blue_24dp);
                }
            }

            // CASE 4: Tracking not active and WayPoint is current position
            else {
                // default marker
                newMarker = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_my_location_crumb_blue_24dp);
            }

            // create overlay item
            OverlayItem overlayItem = createOverlayItem(context, wayPoint.getLocation());
            overlayItem.setMarker(newMarker);

            // add marker to list of overlay items
            overlayItems.add(overlayItem);
        }

        // return overlay for current position
        return new ItemizedIconOverlay<>(overlayItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                        Toast.makeText(context, item.getTitle() + " | " + item.getSnippet(), Toast.LENGTH_LONG).show();
                        LogHelper.v(LOG_TAG, "Tap on waypoint. " + item.getTitle());
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(final int index, final OverlayItem item) {
                        LogHelper.v(LOG_TAG, "Long press on waypoint. " + item.getSnippet());
                        return true;
                    }

                }, context);
    }


    /* Creates a marker overlay item */
    private static OverlayItem createOverlayItem(Context context, Location location) {
        // create content of overlay item
        final String title = context.getString(R.string.marker_description_source) + ": " + location.getProvider();
        final String description = context.getString(R.string.marker_description_accuracy) + ": " + location.getAccuracy();
        final GeoPoint position = new GeoPoint(location.getLatitude(),location.getLongitude());

        return new OverlayItem(title, description, position);
    }

}