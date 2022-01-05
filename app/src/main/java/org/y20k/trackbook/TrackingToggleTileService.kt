/*
 * TrackingToggleTileService.kt
 * Implements the TrackingToggleTileService service
 * A TrackingToggleTileService toggles the recording state from a quick settings tile
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

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.y20k.trackbook.helpers.LogHelper
import org.y20k.trackbook.helpers.PreferencesHelper


/*
 * TrackingToggleTileService class
 */
class TrackingToggleTileService: TileService() {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TrackingToggleTileService::class.java)


    /* Main class variables */
    private var bound: Boolean = false
    private var trackingState: Int = Keys.STATE_TRACKING_NOT
    private lateinit var trackerService: TrackerService


    /* Overrides onTileAdded from TileService */
    override fun onTileAdded() {
        super.onTileAdded()
        // get saved tracking state
        trackingState = PreferencesHelper.loadTrackingState()
        // set up tile
        updateTile()
    }

    /* Overrides onTileRemoved from TileService */
    override fun onTileRemoved() {
        super.onTileRemoved()
    }


    /* Overrides onStartListening from TileService (tile becomes visible) */
    override fun onStartListening() {
        super.onStartListening()
        // get saved tracking state
        trackingState = PreferencesHelper.loadTrackingState()
        // set up tile
        updateTile()
        // register listener for changes in shared preferences
        PreferencesHelper.registerPreferenceChangeListener(sharedPreferenceChangeListener)
    }


    /* Overrides onClick from TileService */
    override fun onClick() {
        super.onClick()
        when (trackingState) {
            Keys.STATE_TRACKING_ACTIVE -> stopTracking()
            else -> startTracking()
        }
    }


    /* Overrides onStopListening from TileService (tile no longer visible) */
    override fun onStopListening() {
        super.onStopListening()
        // unregister listener for changes in shared preferences
        PreferencesHelper.unregisterPreferenceChangeListener(sharedPreferenceChangeListener)
    }


    /* Overrides onDestroy from Service */
    override fun onDestroy() {
        super.onDestroy()
    }


    /* Update quick settings tile */
    private fun updateTile() {
        val tile: Tile = qsTile
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notification_icon_small_24dp)
        when (trackingState) {
            Keys.STATE_TRACKING_ACTIVE -> {
                tile.label = getString(R.string.quick_settings_tile_title_stop)
                tile.contentDescription = getString(R.string.descr_quick_settings_tile_title_stop)
                tile.state = Tile.STATE_ACTIVE
            }
            else -> {
                tile.label = getString(R.string.quick_settings_tile_title_start)
                tile.contentDescription = getString(R.string.descr_quick_settings_tile_title_start)
                tile.state = Tile.STATE_INACTIVE
            }
        }
        tile.updateTile()
    }


    /* Start tracking */
    private fun startTracking() {
        val intent = Intent(application, TrackerService::class.java)
        intent.action = Keys.ACTION_START
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ... start service in foreground to prevent it being killed on Oreo
            application.startForegroundService(intent)
        } else {
            application.startService(intent)
        }
    }


    /* Stop tracking */
    private fun stopTracking() {
        val intent = Intent(application, TrackerService::class.java)
        intent.action = Keys.ACTION_STOP
        application.startService(intent)
    }


    /*
     * Defines the listener for changes in shared preferences
     */
    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Keys.PREF_TRACKING_STATE -> {
                trackingState = PreferencesHelper.loadTrackingState()
                updateTile()
            }
        }
    }
    /*
     * End of declaration
     */




}