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

import android.location.Location;
import android.util.Log;

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
    private List mWayPoints;


    /* Constructor */
    public Track() {
        mWayPoints = new ArrayList<WayPoint>();
    }


    /* Adds new waypoint */
    public void addWayPoint(Location location, boolean isStopOver) {
        // create new waypoint
        WayPoint wayPoint = new WayPoint(location, isStopOver);

        // TODO check if last waypoint is a stopover
        if (CONSTANT_MINIMAL_STOP_TIME != CONSTANT_MINIMAL_STOP_TIME) {
            wayPoint.isStopOver = true;
        } else {
            wayPoint.isStopOver = false;
        }

        // add new waypoint to track
        mWayPoints.add(wayPoint);

        // TODO remove debugging log
        Log.v(LOG_TAG, "!!! new location: " +  wayPoint.location.toString());
    }


    /**
     * Inner class: Defines data type WayPoint ***
     */
    private class WayPoint {

        private Location location;
        private boolean isStopOver;

        /* Constructor */
        public WayPoint(Location location, boolean isStopOver) {
            this.location = location;
            this.isStopOver = isStopOver;
        }

    }
    /**
     * End of inner class
     */

}
