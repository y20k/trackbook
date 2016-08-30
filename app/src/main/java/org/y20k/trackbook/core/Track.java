/**
 * Track.java
 * Implements the Track class
 * A Track stores a list of waypoints
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

package org.y20k.trackbook.core;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;

import org.y20k.trackbook.helpers.LocationHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

import java.util.ArrayList;
import java.util.List;


/**
 * Track class
 */
public class Track implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = Track.class.getSimpleName();


    /* Main class variables */
    private Context mContext;
    private List<WayPoint> mWayPoints;


    /* Constructor */
    public Track(Context context) {
        mContext = context;
        mWayPoints = new ArrayList<WayPoint>();
    }


    /* Adds new waypoint */
    public void addWayPoint(Location location) {
        // create new waypoint
        WayPoint wayPoint = new WayPoint();
        wayPoint.location = location;
        wayPoint.isStopOver = LocationHelper.isStopOver(location);

        // add new waypoint to track
        mWayPoints.add(wayPoint);

        // send local broadcast: new WayPoint added
        Intent i = new Intent();
        i.setAction(ACTION_WAYPOINT_ADDED);
        i.putExtra(EXTRA_WAYPOINT_LOCATION, location);
        i.putExtra(EXTRA_WAYPOINT_IS_STOPOVER, wayPoint.isStopOver);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);

        LogHelper.v(LOG_TAG, "!!! Waypoint No. " + mWayPoints.indexOf(wayPoint) + " Location: " + wayPoint.location.toString()); // TODO remove
    }


    /* Getter for mWayPoints */
    public List<WayPoint> getWayPoints() {
        return mWayPoints;
    }


    /* Getter for location of specific WayPoint */
    public Location getWayPointLocation(int index) {
        return mWayPoints.get(index).location;
    }


    /**
     * Inner class: Defines data type WayPoint
     */
    private class WayPoint {

        private Location location;
        private boolean isStopOver;

    }
    /**
     * End of inner class
     */

}
