/**
 * TrackbookKeys.java
 * Implements the keys used throughout the app
 * This class hosts all keys used to control Trackbook's state
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


/**
 * TrackbookKeys.class
 */
public interface TrackbookKeys {

    /* ACTIONS */
    public static final String ACTION_START = "org.y20k.transistor.action.START";
    public static final String ACTION_STOP = "org.y20k.transistor.action.STOP";
    public static final String ACTION_WAYPOINT_ADDED = "WAYPOINT_ADDED";

    /* EXTRAS */
    public static final String EXTRA_WAYPOINT_LOCATION = "WAYPOINT_LOCATION";
    public static final String EXTRA_WAYPOINT_IS_STOPOVER = "WAYPOINT_IS_STOPOVER";

    /* ARGS */
    public static final String ARG_PERMISSIONS_GRANTED = "ArgPermissionsGranted";

    /* PREFS */
    public static final String PREFS_NAME = "org.y20k.trackbook.prefs";
    public static final String PREFS_TILE_SOURCE = "tilesource";
    public static final String PREFS_LATITUDE = "latitude";
    public static final String PREFS_LONGITUDE = "longitude";
    public static final String PREFS_ZOOM_LEVEL = "zoomLevel";
    public static final String PREFS_SHOW_LOCATION = "showLocation";
    public static final String PREFS_SHOW_COMPASS = "showCompass";

    /* INSTANCE STATE */
    public static final String INSTANCE_FIRST_START = "firstStart";
    public static final String INSTANCE_LATITUDE = "latitude";
    public static final String INSTANCE_LONGITUDE = "longitude";
    public static final String INSTANCE_ZOOM_LEVEL = "zoomLevel";
    public static final String INSTANCE_CURRENT_LOCATION = "currentLocation";

    /* RESULTS */

    /* CONSTANTS */
    public static final int CONSTANT_MINIMAL_STOP_TIME = 300000; // equals 5 minutes
    public static final long CONSTANT_MAXIMAL_DURATION = 43200000; // equals 8 hours
    public static final long CONSTANT_TRACKING_INTERVAL = 5000; // equals 5 seconds

    /* MISC */
    public static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    public static final int LOCATION_STATUS_OFFLINE = 0;
    public static final int LOCATION_STATUS_OK = 1;
    public static final int LOCATION_STATUS_GPS_ONLY = 2;
    public static final int LOCATION_STATUS_NETWORK_ONLY = 3;

}
