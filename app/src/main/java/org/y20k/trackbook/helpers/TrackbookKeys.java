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
    String ACTION_START = "org.y20k.transistor.action.START";
    String ACTION_STOP = "org.y20k.transistor.action.STOP";
    String ACTION_DEFAULT = "DEFAULT";
    String ACTION_SHOW_MAP = "SHOW_MAP";
    String ACTION_TRACK_UPDATED = "TRACK_UPDATED";
    String ACTION_TRACKING_STOPPED = "TRACKING_STOPPED";
    String ACTION_TRACK_SAVE = "TRACK_SAVE";

    /* EXTRAS */
    String EXTRA_TRACK = "TRACK";
    String EXTRA_LAST_LOCATION = "LAST_LOCATION";
    String EXTRA_TRACKING_STATE = "TRACKING_STATE";
    String EXTRA_CLEAR_MAP = "CLEAR_MAP";
    String EXTRA_INFOSHEET_TITLE = "EXTRA_INFOSHEET_TITLE";
    String EXTRA_INFOSHEET_CONTENT = "INFOSHEET_CONTENT";
    String EXTRA_SAVE_FINISHED = "SAVE_FINISHED";

    /* ARGS */
    String ARG_PERMISSIONS_GRANTED = "ArgPermissionsGranted";
    String ARG_TRACKING_STATE = "ArgTrackingState";
    String ARG_TRACK = "ArgTrack";
    String ARG_TRACK_VISIBLE = "ArgTrackVisible";
    String ARG_TRACK_DISTANCE = "ArgTrackDistance";

    /* TAGS */
    String TAG_MAIN_ACTIVITY_FRAGMENT = "TagMainActivityFragment";

    /* PREFS */
    String PREFS_NAME = "org.y20k.trackbook.prefs";
    String PREFS_TILE_SOURCE = "tileSource";
    String PREFS_LATITUDE = "latitude";
    String PREFS_LONGITUDE = "longitude";
    String PREFS_ZOOM_LEVEL = "zoomLevel";
    String PREFS_SHOW_LOCATION = "showLocation";
    String PREFS_SHOW_COMPASS = "showCompass";

    /* INSTANCE STATE */
    String INSTANCE_FIRST_START = "firstStart";
    String INSTANCE_TRACKING_STATE = "trackingState";
    String INSTANCE_TRACK_VISIBLE = "trackVisible";
    String INSTANCE_SELECTED_TAB = "selectedTab";
    String INSTANCE_FAB_STATE = "fabState";
    String INSTANCE_FAB_SUB_MENU_VISIBLE = "fabSubMenuVisible";
    String INSTANCE_TRACK_MAIN_MAP = "trackMainMap";
    String INSTANCE_LATITUDE_MAIN_MAP = "latitudeMainMap";
    String INSTANCE_LONGITUDE_MAIN_MAP = "longitudeMainMap";
    String INSTANCE_ZOOM_LEVEL_MAIN_MAP = "zoomLevelMainMap";
    String INSTANCE_TRACK_TRACK_MAP = "trackTrackMap";
    String INSTANCE_LATITUDE_TRACK_MAP = "latitudeTrackMap";
    String INSTANCE_LONGITUDE_TRACK_MAP = "longitudeTrackMap";
    String INSTANCE_ZOOM_LEVEL_TRACK_MAP = "zoomLevelTrackMap";
    String INSTANCE_CURRENT_LOCATION = "currentLocation";

    /* FRAGMENT IDS */
    int FRAGMENT_ID_MAP = 0;
    int FRAGMENT_ID_TRACK = 1;

    /* RESULTS */

    /* CONSTANTS */
    long ONE_NANOSECOND = 1000000000L;
    long EIGHT_HOURS_IN_MILLISECONDS = 43200000; // maximum tracking duration
    long FIFTEEN_SECONDS_IN_MILLISECONDS = 15000; // timer interval for tracking
    long FIVE_MINUTES_IN_NANOSECONDS = 5L * 60000000000L; // determines a stop over
    long TWO_MINUTES_IN_NANOSECONDS = 2L * 60000000000L; // defines an old location

    /* MISC */
    int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;
    int TRACKER_SERVICE_NOTIFICATION_ID = 1;
    int INFOSHEET_CONTENT_ABOUT = 1;
    int METRIC = 1;
    int IMPERIAL = 2;
    int FAB_STATE_DEFAULT = 0;
    int FAB_STATE_RECORDING = 1;
    int FAB_STATE_SAVE = 2;

    double DEFAULT_LATITUDE = 49.41667; // latitude Nordkapp, Norway
    double DEFAULT_LONGITUDE = 8.67201; // longitude Nordkapp, Norway
}
