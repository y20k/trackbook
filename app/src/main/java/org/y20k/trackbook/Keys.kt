/*
 * Keys.kt
 * Implements the keys used throughout the app
 * This object hosts all keys used to control Trackbook's state
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */


package org.y20k.trackbook

import java.util.*


/*
 * Keys object
 */
object Keys {

    // application name
    const val APPLICATION_NAME: String = "Trackbook"

    // version numbers
    const val CURRENT_TRACK_FORMAT_VERSION: Int = 4
    const val CURRENT_TRACKLIST_FORMAT_VERSION: Int = 0

    // intent actions
    const val ACTION_START: String = "org.y20k.trackbook.action.START"
    const val ACTION_STOP: String = "org.y20k.trackbook.action.STOP"
    const val ACTION_RESUME: String = "org.y20k.trackbook.action.RESUME"

    // args
    const val ARG_TRACK_TITLE: String = "ArgTrackTitle"
    const val ARG_TRACK_FILE_URI: String = "ArgTrackFileUri"
    const val ARG_GPX_FILE_URI: String = "ArgGpxFileUri"
    const val ARG_TRACK_ID: String = "ArgTrackId"

    // preferences
    const val PREF_ONE_TIME_HOUSEKEEPING_NECESSARY = "ONE_TIME_HOUSEKEEPING_NECESSARY_VERSIONCODE_38" // increment to current app version code to trigger housekeeping that runs only once
    const val PREF_THEME_SELECTION: String= "prefThemeSelection"
    const val PREF_CURRENT_BEST_LOCATION_PROVIDER: String = "prefCurrentBestLocationProvider"
    const val PREF_CURRENT_BEST_LOCATION_LATITUDE: String = "prefCurrentBestLocationLatitude"
    const val PREF_CURRENT_BEST_LOCATION_LONGITUDE: String = "prefCurrentBestLocationLongitude"
    const val PREF_CURRENT_BEST_LOCATION_ACCURACY: String = "prefCurrentBestLocationAccuracy"
    const val PREF_CURRENT_BEST_LOCATION_ALTITUDE: String = "prefCurrentBestLocationAltitude"
    const val PREF_CURRENT_BEST_LOCATION_TIME: String = "prefCurrentBestLocationTime"
    const val PREF_MAP_ZOOM_LEVEL: String = "prefMapZoomLevel"
    const val PREF_TRACKING_STATE: String = "prefTrackingState"
    const val PREF_USE_IMPERIAL_UNITS: String = "prefUseImperialUnits"
    const val PREF_GPS_ONLY: String = "prefGpsOnly"
    const val PREF_RECORDING_ACCURACY_HIGH: String = "prefRecordingAccuracyHigh"
    const val PREF_ALTITUDE_SMOOTHING_VALUE: String = "prefAltitudeSmoothingValue"
    const val PREF_LOCATION_ACCURACY_THRESHOLD: String = "prefLocationAccuracyThreshold"
    const val PREF_LOCATION_AGE_THRESHOLD: String = "prefLocationAgeThreshold"

    // states
    const val STATE_TRACKING_NOT: Int = 0
    const val STATE_TRACKING_ACTIVE: Int = 1
    const val STATE_TRACKING_PAUSED: Int = 2
    const val STATE_THEME_FOLLOW_SYSTEM: String = "stateFollowSystem"
    const val STATE_THEME_LIGHT_MODE: String = "stateLightMode"
    const val STATE_THEME_DARK_MODE: String = "stateDarkMode"

    // dialog types
    const val DIALOG_EMPTY_RECORDING: Int = 0
    const val DIALOG_DELETE_TRACK: Int = 1
    const val DIALOG_DELETE_NON_STARRED: Int = 2
    const val DIALOG_DELETE_CURRENT_RECORDING: Int = 3

    // dialog results
    const val DIALOG_EMPTY_PAYLOAD_STRING: String = ""
    const val DIALOG_EMPTY_PAYLOAD_INT: Int = -1

    // folder names
    const val FOLDER_TEMP: String  = "temp"
    const val FOLDER_TRACKS: String = "tracks"
    const val FOLDER_GPX: String = "gpx"

    // file names and extensions
    const val MIME_TYPE_GPX: String = "application/gpx+xml"
    const val GPX_FILE_EXTENSION: String = ".gpx"
    const val TRACKBOOK_LEGACY_FILE_EXTENSION: String = ".trackbook"
    const val TRACKBOOK_FILE_EXTENSION: String = ".json"
    const val TEMP_FILE: String = "temp.json"
    const val TRACKLIST_FILE: String = "tracklist.json"

    // view types
    const val VIEW_TYPE_STATISTICS: Int = 1
    const val VIEW_TYPE_TRACK: Int = 2

    // default values
    val DEFAULT_DATE: Date = Date(0L)
    const val DEFAULT_RFC2822_DATE: String = "Thu, 01 Jan 1970 01:00:00 +0100"  // --> Date(0)
    const val ONE_HOUR_IN_MILLISECONDS: Int = 3600000
    const val EMPTY_STRING_RESOURCE: Int = 0
    const val REQUEST_CURRENT_LOCATION_INTERVAL: Long = 1000L                   // 1 second in milliseconds
    const val ADD_WAYPOINT_TO_TRACK_INTERVAL: Long = 1000L                      // 1 second in milliseconds
    const val SAVE_TEMP_TRACK_INTERVAL: Long = 9000L                            // 9 seconds in milliseconds
    const val SIGNIFICANT_TIME_DIFFERENCE: Long = 120000L                       // 2 minutes in milliseconds
    const val STOP_OVER_THRESHOLD: Long = 300000L                               // 5 minutes in milliseconds
    const val IMPLAUSIBLE_TRACK_START_SPEED: Double = 250.0                     // 250 km/h
    const val DEFAULT_LATITUDE: Double = 71.172500                              // latitude Nordkapp, Norway
    const val DEFAULT_LONGITUDE: Double = 25.784444                             // longitude Nordkapp, Norway
    const val DEFAULT_ACCURACY: Float = 300f                                    // in meters
    const val DEFAULT_ALTITUDE: Double = 0.0
    const val DEFAULT_TIME: Long = 0L
    const val DEFAULT_ALTITUDE_SMOOTHING_VALUE: Int = 13
    const val DEFAULT_THRESHOLD_LOCATION_ACCURACY: Int = 30                     // 30 meters
    const val DEFAULT_THRESHOLD_LOCATION_AGE: Long = 60000000000L               // one minute in nanoseconds
    const val DEFAULT_THRESHOLD_DISTANCE: Float = 15f                           // 15 meters
    const val DEFAULT_ZOOM_LEVEL: Double = 16.0
    const val MIN_NUMBER_OF_WAYPOINTS_FOR_ELEVATION_CALCULATION: Int = 5
    const val MAX_NUMBER_OF_WAYPOINTS_FOR_ELEVATION_CALCULATION: Int = 20
    const val ALTITUDE_MEASUREMENT_ERROR_THRESHOLD = 10 // altitude changes of 10 meter or more (per 15 seconds) are being discarded

    // notification
    const val TRACKER_SERVICE_NOTIFICATION_ID: Int = 1
    const val NOTIFICATION_CHANNEL_RECORDING: String = "notificationChannelIdRecordingChannel"
}
