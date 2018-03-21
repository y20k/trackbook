/**
 * TrackbookKeys.java
 * Implements the keys used throughout the app
 * This interface hosts all keys used to control Trackbook's state
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-18 - Y20K.org
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
    String ACTION_START = "org.y20k.trackbook.action.START";
    String ACTION_STOP = "org.y20k.trackbook.action.STOP";
    String ACTION_DISMISS = "org.y20k.transistor.action.DISMISS";
    String ACTION_RESUME = "org.y20k.transistor.action.RESUME";
    String ACTION_CLEAR = "org.y20k.transistor.action.CLEAR";
    String ACTION_SAVE = "org.y20k.transistor.action.SAVE";
    String ACTION_DEFAULT = "DEFAULT";
    String ACTION_SHOW_MAP = "SHOW_MAP";
    String ACTION_TRACK_UPDATED = "TRACK_UPDATED";
    String ACTION_TRACK_REQUEST = "TRACK_REQUEST";
    String ACTION_TRACKING_STATE_CHANGED = "TRACKING_STATE_CHANGED";
    String ACTION_TRACK_SAVE = "TRACK_SAVE";

    /* EXTRAS */
    String EXTRA_TRACK = "TRACK";
    String EXTRA_LAST_LOCATION = "LAST_LOCATION";
    String EXTRA_TRACKING_STATE = "TRACKING_STATE";
    String EXTRA_INFOSHEET_TITLE = "EXTRA_INFOSHEET_TITLE";
    String EXTRA_INFOSHEET_CONTENT = "INFOSHEET_CONTENT";
    String EXTRA_SAVE_FINISHED = "SAVE_FINISHED";

    /* ARGS */
    String ARG_DIALOG_TITLE = "ArgDialogTitle";
    String ARG_DIALOG_MESSAGE = "ArgDialogMessage";
    String ARG_DIALOG_BUTTON_POSITIVE = "ArgDialogButtonPositive";
    String ARG_DIALOG_BUTTON_NEGATIVE = "ArgDialogButtonNegative";

    /* PREFS */
    String PREFS_FAB_STATE = "fabStatePrefs";
    String PREFS_TRACKER_SERVICE_RUNNING = "trackerServiceRunning";
    String PREFS_CURRENT_TRACK_DURATION = "currentTrackDuration";
    String PREF_NIGHT_MODE_STATE = "prefNightModeState";

    /* INSTANCE STATE */
    String INSTANCE_FIRST_START = "firstStart";
    String INSTANCE_TRACKING_STATE = "trackingState";
    String INSTANCE_SELECTED_TAB = "selectedTab";
    String INSTANCE_FAB_SUB_MENU_VISIBLE = "fabSubMenuVisible";
    String INSTANCE_LATITUDE_MAIN_MAP = "latitudeMainMap";
    String INSTANCE_LONGITUDE_MAIN_MAP = "longitudeMainMap";
    String INSTANCE_ZOOM_LEVEL_MAIN_MAP = "zoomLevelMainMap";
    String INSTANCE_TRACK_TRACK_MAP = "trackTrackMap";
    String INSTANCE_LATITUDE_TRACK_MAP = "latitudeTrackMap";
    String INSTANCE_LONGITUDE_TRACK_MAP = "longitudeTrackMap";
    String INSTANCE_ZOOM_LEVEL_TRACK_MAP = "zoomLevelTrackMap";
    String INSTANCE_CURRENT_LOCATION = "currentLocation";
    String INSTANCE_CURRENT_TRACK = "currentTrack";

    /* FRAGMENT IDS */
    int FRAGMENT_ID_MAP = 0;
    int FRAGMENT_ID_TRACKS = 1;

    String FRAGMENT_TAG_MAP = "fragmentTagMap";
    String FRAGMENT_TAG_TRACKS = "fragmentTagTracks";


    /* RESULTS */
    int RESULT_SAVE_DIALOG = 1;
    int RESULT_CLEAR_DIALOG = 2;
    int RESULT_DELETE_DIALOG = 3;
    int RESULT_EXPORT_DIALOG = 4;

    /* CONSTANTS */
    long ONE_NANOSECOND = 1000000000L;
    long EIGHT_HOURS_IN_MILLISECONDS = 43200000; // maximum tracking duration
    long FIFTEEN_SECONDS_IN_MILLISECONDS = 15000; // timer interval for tracking
    long FIVE_MINUTES_IN_NANOSECONDS = 5L * 60000000000L; // determines a stop over
    long TWO_MINUTES_IN_NANOSECONDS = 2L * 60000000000L; // defines an old location
    int MAXIMUM_TRACK_FILES = 25;

    /* FILE */
    String FILE_TYPE_GPX_EXTENSION = ".gpx";
    String FILE_TYPE_TRACKBOOK_EXTENSION = ".trackbook";
    String FILE_NAME_TEMP = "temp";
    String TRACKS_DIRECTORY_NAME = "tracks";
    int FILE_TEMP_TRACK = 0;
    int FILE_MOST_CURRENT_TRACK = 1;

    /* UNITS */
    int METRIC = 1;
    int IMPERIAL = 2;

    /* FLOATING ACTION BUTTON */
    int FAB_STATE_DEFAULT = 0;
    int FAB_STATE_RECORDING = 1;
    int FAB_STATE_SAVE = 2;

    /* NOTIFICATION */
    int TRACKER_SERVICE_NOTIFICATION_ID = 1;
    String NOTIFICATION_CHANEL_ID_RECORDING_CHANNEL ="notificationChannelIdRecordingChannel";

    /* MISC */
    int CURRENT_TRACK_FORMAT_VERSION = 2; // incremental version number to prevent issues in case the Track format evolves
    double DEFAULT_LATITUDE = 49.41667; // latitude Nordkapp, Norway
    double DEFAULT_LONGITUDE = 8.67201; // longitude Nordkapp, Norway
    int MEASUREMENT_ERROR_THRESHOLD = 10; // altitude changes of 10 meter or more (per 15 seconds) are being discarded
    int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

}
