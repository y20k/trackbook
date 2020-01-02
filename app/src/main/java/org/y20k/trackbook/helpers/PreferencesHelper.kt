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
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import org.y20k.trackbook.Keys
import org.y20k.trackbook.extensions.getDouble
import org.y20k.trackbook.extensions.putDouble


/*
 * PreferencesHelper object
 */
object PreferencesHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(PreferencesHelper::class.java)


    /* Loads zoom level of map */
    fun loadZoomLevel(context: Context): Double {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        // load zoom level
        return settings.getDouble(Keys.PREF_MAP_ZOOM_LEVEL, Keys.DEFAULT_ZOOM_LEVEL)
    }


    /* Saves zoom level of map */
    fun saveZoomLevel(context: Context, zoomLevel: Double) {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        // save zoom level
        editor.putDouble(Keys.PREF_MAP_ZOOM_LEVEL, zoomLevel)
        editor.apply()
    }


    /* Loads tracking state */
    fun loadTrackingState(context: Context): Int {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        // load tracking state
        return settings.getInt(Keys.PREF_TRACKING_STATE, Keys.STATE_NOT_TRACKING)
    }


    /* Saves tracking state */
    fun saveTrackingState(context: Context, trackingState: Int) {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        // save tracking state
        editor.putInt(Keys.PREF_TRACKING_STATE, trackingState)
        editor.apply()
    }


    /* Loads length unit system - metric or imperial */
    fun loadUseImperialUnits(context: Context): Boolean {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        // load length unit system
        return settings.getBoolean(Keys.PREF_USE_IMPERIAL_UNITS, LengthUnitHelper.useImperialUnits())
    }


    /* Loads length unit system - metric or imperial */
    fun loadGpsOnly(context: Context): Boolean {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        // load length unit system
        return settings.getBoolean(Keys.PREF_GPS_ONLY, false)
    }

    /* Loads accuracy threshold used to determine if location is good enough */
    fun loadAccuracyThreshold(context: Context): Int {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        // load tracking state
        return settings.getInt(Keys.PREF_LOCATION_ACCURACY_THRESHOLD, Keys.DEFAULT_THRESHOLD_LOCATION_ACCURACY)
    }


    /* Loads the state of a map */
    fun loadCurrentBestLocation(context: Context): Location {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val provider: String = settings.getString(Keys.PREF_CURRENT_BEST_LOCATION_PROVIDER, LocationManager.NETWORK_PROVIDER) ?: LocationManager.NETWORK_PROVIDER
        // create location
        val currentBestLocation: Location = Location(provider)
        // load location attributes
        currentBestLocation.latitude = settings.getDouble(Keys.PREF_CURRENT_BEST_LOCATION_LATITUDE, Keys.DEFAULT_LATITUDE)
        currentBestLocation.longitude = settings.getDouble(Keys.PREF_CURRENT_BEST_LOCATION_LONGITUDE, Keys.DEFAULT_LONGITUDE)
        currentBestLocation.accuracy = settings.getFloat(Keys.PREF_CURRENT_BEST_LOCATION_ACCURACY, Keys.DEFAULT_ACCURACY)
        currentBestLocation.altitude = settings.getDouble(Keys.PREF_CURRENT_BEST_LOCATION_ALTITUDE, Keys.DEFAULT_ALTITUDE)
        currentBestLocation.time = settings.getLong(Keys.PREF_CURRENT_BEST_LOCATION_TIME, Keys.DEFAULT_TIME)
        return currentBestLocation
    }


    /* Saves the state of a map */
    fun saveCurrentBestLocation(context: Context, currentBestLocation: Location) {
        // get preferences
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        // save location
        editor.putDouble(Keys.PREF_CURRENT_BEST_LOCATION_LATITUDE, currentBestLocation.latitude)
        editor.putDouble(Keys.PREF_CURRENT_BEST_LOCATION_LONGITUDE, currentBestLocation.longitude)
        editor.putFloat(Keys.PREF_CURRENT_BEST_LOCATION_ACCURACY, currentBestLocation.accuracy)
        editor.putDouble(Keys.PREF_CURRENT_BEST_LOCATION_ALTITUDE, currentBestLocation.altitude)
        editor.putLong(Keys.PREF_CURRENT_BEST_LOCATION_TIME, currentBestLocation.time)
        editor.apply()
    }


    /* Load state of Night Mode */
    fun loadNightModeState(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(Keys.PREF_NIGHT_MODE_STATE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }


    /* Save state of night mode */
    fun saveNightModeState(context: Context, currentState: Int) {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        editor.putInt(Keys.PREF_NIGHT_MODE_STATE, currentState)
        editor.apply()
    }


    /* Checks if housekeeping work needs to be done - used usually in DownloadWorker "REQUEST_UPDATE_COLLECTION" */
    fun isHouseKeepingNecessary(context: Context): Boolean {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        return settings.getBoolean(Keys.PREF_ONE_TIME_HOUSEKEEPING_NECESSARY, true)
    }


    /* Saves state of housekeeping */
    fun saveHouseKeepingNecessaryState(context: Context, state: Boolean = false) {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = settings.edit()
        editor.putBoolean(Keys.PREF_ONE_TIME_HOUSEKEEPING_NECESSARY, state)
        editor.apply()
    }

}