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
    public static final String ACTION_TRACK_UPDATED = "TRACK_UPDATED";
    public static final String ACTION_TRACKING_STOPPED = "TRACKING_STOPPED";

    /* EXTRAS */
    public static final String EXTRA_TRACK = "TRACK";
    public static final String EXTRA_LAST_LOCATION = "LAST_LOCATION";
    public static final String EXTRA_TRACKING_STATE = "TRACKING_STATE";
    public static final String EXTRA_CLEAR_MAP = "CLEAR_MAP";
    public static final String EXTRA_INFOSHEET_TITLE = "EXTRA_INFOSHEET_TITLE";
    public static final String EXTRA_INFOSHEET_CONTENT = "INFOSHEET_CONTENT";

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
    public static final String INSTANCE_TRACKING_STATE = "trackingState";
    public static final String INSTANCE_TRACK = "track";

    /* RESULTS */

    /* CONSTANTS */
    public static final long EIGHT_HOURS_IN_MILLISECONDS = 43200000; // maximum tracking duration
    public static final long FIFTEEN_SECONDS_IN_MILLISECONDS = 15000; // timer interval for tracking
    public static final long FIVE_MINUTES_IN_NANOSECONDS = 5L * 60000000000L; // determines a stop over
    public static final long TWO_MINUTES_IN_NANOSECONDS = 2L * 60000000000L; // defines an old location
    public static final long TWELVE_SECONDS_IN_NANOSECONDS = 12000000000L; // defines a new location


    /* MISC */
    public static final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    public static final int TRACKER_SERVICE_NOTIFICATION_ID = 1;
    public static final int INFOSHEET_CONTENT_ABOUT = 1;

}
