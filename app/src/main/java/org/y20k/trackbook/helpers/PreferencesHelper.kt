/*
 * PreferencesHelper.kt
 * Implements the PreferencesHelper object
 * A PreferencesHelper provides helper methods for the saving and loading values from shared preferences
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */


package org.y20k.trackbook.helpers

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.y20k.trackbook.Keys
import org.y20k.trackbook.extensions.getDouble
import org.y20k.trackbook.extensions.putDouble


/*
 * PreferencesHelper object
 */
object PreferencesHelper {

    /* The sharedPreferences object to be initialized */
    private lateinit var sharedPreferences: SharedPreferences

    /* Initialize a single sharedPreferences object when the app is launched */
    fun Context.initPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(PreferencesHelper::class.java)


    /* Loads zoom level of map */
    fun loadZoomLevel(): Double {
        // load zoom level
        return sharedPreferences.getDouble(Keys.PREF_MAP_ZOOM_LEVEL, Keys.DEFAULT_ZOOM_LEVEL)
    }


    /* Saves zoom level of map */
    fun saveZoomLevel(zoomLevel: Double) {
        // save zoom level
        sharedPreferences.edit { putDouble(Keys.PREF_MAP_ZOOM_LEVEL, zoomLevel) }
    }


    /* Loads tracking state */
    fun loadTrackingState(): Int {
        // load tracking state
        return sharedPreferences.getInt(Keys.PREF_TRACKING_STATE, Keys.STATE_TRACKING_NOT)
    }


    /* Saves tracking state */
    fun saveTrackingState(trackingState: Int) {
        // save tracking state
        sharedPreferences.edit { putInt(Keys.PREF_TRACKING_STATE, trackingState) }
    }


    /* Loads length unit system - metric or imperial */
    fun loadUseImperialUnits(): Boolean {
        // load length unit system
        return sharedPreferences.getBoolean(Keys.PREF_USE_IMPERIAL_UNITS, LengthUnitHelper.useImperialUnits())
    }


    /* Loads length unit system - metric or imperial */
    fun loadGpsOnly(): Boolean {
        // load length unit system
        return sharedPreferences.getBoolean(Keys.PREF_GPS_ONLY, false)
    }

    /* Loads accuracy threshold used to determine if location is good enough */
    fun loadAccuracyThreshold(): Int {
        // load tracking state
        return sharedPreferences.getInt(Keys.PREF_LOCATION_ACCURACY_THRESHOLD, Keys.DEFAULT_THRESHOLD_LOCATION_ACCURACY)
    }



    /* Loads state of recording accuracy */
    fun loadRecordingAccuracyHigh(): Boolean {
        // load current setting
        return sharedPreferences.getBoolean(Keys.PREF_RECORDING_ACCURACY_HIGH, false)
    }


    /* Loads current accuracy multiplier */
    fun loadAccuracyMultiplier(): Int {
        // load current setting
        val recordingAccuracyHigh: Boolean = sharedPreferences.getBoolean(Keys.PREF_RECORDING_ACCURACY_HIGH, false)
        // return multiplier based on state
        return if (recordingAccuracyHigh) 2 else 1
    }


    /* Load altitude smoothing value */
    fun loadAltitudeSmoothingValue(): Int {
        // load current setting
        return sharedPreferences.getInt(Keys.PREF_ALTITUDE_SMOOTHING_VALUE, Keys.DEFAULT_ALTITUDE_SMOOTHING_VALUE)
    }


    /* Loads the state of a map */
    fun loadCurrentBestLocation(): Location {
        val provider: String = sharedPreferences.getString(Keys.PREF_CURRENT_BEST_LOCATION_PROVIDER, LocationManager.NETWORK_PROVIDER) ?: LocationManager.NETWORK_PROVIDER
        // create location
        return Location(provider).apply {
            // load location attributes
            latitude = sharedPreferences.getDouble(Keys.PREF_CURRENT_BEST_LOCATION_LATITUDE, Keys.DEFAULT_LATITUDE)
            longitude = sharedPreferences.getDouble(Keys.PREF_CURRENT_BEST_LOCATION_LONGITUDE, Keys.DEFAULT_LONGITUDE)
            accuracy = sharedPreferences.getFloat(Keys.PREF_CURRENT_BEST_LOCATION_ACCURACY, Keys.DEFAULT_ACCURACY)
            altitude = sharedPreferences.getDouble(Keys.PREF_CURRENT_BEST_LOCATION_ALTITUDE, Keys.DEFAULT_ALTITUDE)
            time = sharedPreferences.getLong(Keys.PREF_CURRENT_BEST_LOCATION_TIME, Keys.DEFAULT_TIME)
        }
    }


    /* Saves the state of a map */
    fun saveCurrentBestLocation(currentBestLocation: Location) {
        sharedPreferences.edit {
            // save location
            putDouble(Keys.PREF_CURRENT_BEST_LOCATION_LATITUDE, currentBestLocation.latitude)
            putDouble(Keys.PREF_CURRENT_BEST_LOCATION_LONGITUDE, currentBestLocation.longitude)
            putFloat(Keys.PREF_CURRENT_BEST_LOCATION_ACCURACY, currentBestLocation.accuracy)
            putDouble(Keys.PREF_CURRENT_BEST_LOCATION_ALTITUDE, currentBestLocation.altitude)
            putLong(Keys.PREF_CURRENT_BEST_LOCATION_TIME, currentBestLocation.time)
        }
    }


    /* Load currently selected app theme */
    fun loadThemeSelection(): String {
        return sharedPreferences.getString(Keys.PREF_THEME_SELECTION, Keys.STATE_THEME_FOLLOW_SYSTEM) ?: Keys.STATE_THEME_FOLLOW_SYSTEM
    }


    /* Checks if housekeeping work needs to be done - used usually in DownloadWorker "REQUEST_UPDATE_COLLECTION" */
    fun isHouseKeepingNecessary(): Boolean {
        return sharedPreferences.getBoolean(Keys.PREF_ONE_TIME_HOUSEKEEPING_NECESSARY, true)
    }


    /* Saves state of housekeeping */
    fun saveHouseKeepingNecessaryState(state: Boolean = false) {
        sharedPreferences.edit { putBoolean(Keys.PREF_ONE_TIME_HOUSEKEEPING_NECESSARY, state) }
    }

    /* Start watching for changes in shared preferences - context must implement OnSharedPreferenceChangeListener */
    fun registerPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    /* Stop watching for changes in shared preferences - context must implement OnSharedPreferenceChangeListener */
    fun unregisterPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
