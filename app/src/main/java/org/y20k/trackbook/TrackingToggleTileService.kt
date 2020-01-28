/*
 * TrackingToggleTileService.kt
 * Implements the TrackingToggleTileService service
 * A TrackingToggleTileService toggles the recording state from a quick settings tile
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


package org.y20k.trackbook

import android.content.*
import android.graphics.drawable.Icon
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.preference.PreferenceManager
import org.y20k.trackbook.helpers.LogHelper
import org.y20k.trackbook.helpers.PreferencesHelper


/*
 * TrackingToggleTileService class
 */
class TrackingToggleTileService(): TileService() {

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
        trackingState = PreferencesHelper.loadTrackingState(this)
    }

    /* Overrides onTileRemoved from TileService */
    override fun onTileRemoved() {
        super.onTileRemoved()
    }


    /* Overrides onStartListening from TileService */
    override fun onStartListening() {
        super.onStartListening()
        // tile becomes visible - bind tracker service
        bindService(Intent(this, TrackerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }


    /* Overrides onClick from TileService */
    override fun onClick() {
        super.onClick()
        when (trackingState) {
            Keys.STATE_TRACKING_ACTIVE -> {
                trackerService.stopTracking()
            }
            else -> {
                trackerService.startTracking(newTrack = false)
            }
        }
    }


    /* Overrides onStopListening from TileService */
    override fun onStopListening() {
        super.onStopListening()
        // tile no longer visible - unbind tracker service
        unbindService(connection)
    }


    /* Overrides onDestroy from Service */
    override fun onDestroy() {
        super.onDestroy()
        if (bound) unbindService(connection)
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


    /*
     * Defines the listener for changes in shared preferences
     */
    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Keys.PREF_TRACKING_STATE -> {
                trackingState = PreferencesHelper.loadTrackingState(this)
                updateTile()
            }
        }
    }
    /*
     * End of declaration
     */


    /*
     * Defines callbacks for service binding, passed to bindService()
     */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as TrackerService.LocalBinder
            trackerService = binder.service
            trackingState = trackerService.trackingState
            bound = true
            // update state of tile
            updateTile()
            // register listener for changes in shared preferences
            PreferenceManager.getDefaultSharedPreferences(this@TrackingToggleTileService).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)

        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            // unregister listener for changes in shared preferences
            PreferenceManager.getDefaultSharedPreferences(this@TrackingToggleTileService).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        }
    }
    /*
     * End of declaration
     */



}