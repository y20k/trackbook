/*
 * MapFragmentLayoutHolder.kt
 * Implements the MapFragmentLayoutHolder class
 * A MapFragmentLayoutHolder hold references to the main views of a map fragment
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


package org.y20k.trackbook.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.y20k.trackbook.Keys
import org.y20k.trackbook.R
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.helpers.AppThemeHelper
import org.y20k.trackbook.helpers.LogHelper
import org.y20k.trackbook.helpers.MapHelper
import org.y20k.trackbook.helpers.PreferencesHelper


/*
 * MapFragmentLayoutHolder class
 */
data class MapFragmentLayoutHolder(var context: Context, var inflater: LayoutInflater, var container: ViewGroup?, val startLocation: Location, val trackingState: Int) {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(MapFragmentLayoutHolder::class.java)


    /* Main class variables */
    val rootView: View
    val mapView: MapView
    val currentLocationButton: FloatingActionButton
    val recordingButton: FloatingActionButton
    val recordingButtonSubMenu: Group
    val saveButton: FloatingActionButton
    val clearButton: FloatingActionButton
    val resumeButton: FloatingActionButton
    var userInteraction: Boolean = false
    private var currentPositionOverlay: ItemizedIconOverlay<OverlayItem>
    private var currentTrackOverlay: ItemizedIconOverlay<OverlayItem>?
    private var locationErrorBar: Snackbar
    private var controller: IMapController
    private var zoomLevel: Double


    /* Init block */
    init {
        // find views
        rootView = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = rootView.findViewById(R.id.map)
        currentLocationButton = rootView.findViewById(R.id.fab_location_button)
        recordingButton = rootView.findViewById(R.id.fab_main_button)
        recordingButtonSubMenu = rootView.findViewById(R.id.fab_sub_menu)
        saveButton = rootView.findViewById(R.id.fab_sub_menu_button_save)
        clearButton = rootView.findViewById(R.id.fab_sub_menu_button_clear)
        resumeButton = rootView.findViewById(R.id.fab_sub_menu_button_resume)
        locationErrorBar = Snackbar.make(mapView, String(), Snackbar.LENGTH_INDEFINITE)

        // basic map setup
        controller = mapView.controller
        mapView.isTilesScaledToDpi = true
        mapView.setTilesScaledToDpi(true)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        zoomLevel = PreferencesHelper.loadZoomLevel(context)
        controller.setZoom(zoomLevel)

        // set dark map tiles, if necessary
        if (AppThemeHelper.isDarkModeOn(context as Activity)) {
            mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }

        // add compass to map
        val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), mapView)
        compassOverlay.enableCompass()
        compassOverlay.setCompassCenter(36f, 60f)

        mapView.overlays.add(compassOverlay)

        // add my location overlay
        currentPositionOverlay = MapHelper.createMyLocationOverlay(context, startLocation, trackingState)
        mapView.overlays.add(currentPositionOverlay)
        centerMap(startLocation)

        // initialize track overlay
        currentTrackOverlay = null

        // initialize recording button state
        updateRecordingButton(trackingState)

        // listen for user interaction
        addInteractionListener()
    }


    /* Listen for user interaction */
    @SuppressLint("ClickableViewAccessibility")
    private fun addInteractionListener() {
        mapView.setOnTouchListener { v, event ->
            userInteraction = true
            false
        }
    }


    /* Set map center */
    fun centerMap(location: Location, animated: Boolean = false) {
        val position = GeoPoint(location.latitude, location.longitude)
        when (animated) {
            true -> controller.animateTo(position)
            false -> controller.setCenter(position)
        }
        userInteraction = false
    }


    /* Save current best location and state of map to shared preferences */
    fun saveState(currentBestLocation: Location) {
        PreferencesHelper.saveCurrentBestLocation(context, currentBestLocation)
        PreferencesHelper.saveZoomLevel(context, mapView.getZoomLevelDouble())
        // reset user interaction state
        userInteraction = false
    }


    /* Mark current position on map */
    fun markCurrentPosition(location: Location, trackingState: Int = Keys.STATE_TRACKING_NOT) {
        mapView.overlays.remove(currentPositionOverlay)
        currentPositionOverlay = MapHelper.createMyLocationOverlay(context, location, trackingState)
        mapView.overlays.add(currentPositionOverlay)
    }


    /* Overlay current track on map */
    fun overlayCurrentTrack(track: Track, trackingState: Int) {
        if (currentTrackOverlay != null) {
            mapView.overlays.remove(currentTrackOverlay)
        }
        if (track.wayPoints.isNotEmpty()) {
            currentTrackOverlay = MapHelper.createTrackOverlay(context, track, trackingState)
            mapView.overlays.add(currentTrackOverlay)
        }
    }


    /* Toggles state of recording button and sub menu_bottom_navigation */
    fun updateRecordingButton(trackingState: Int) {
        when (trackingState) {
            Keys.STATE_TRACKING_NOT -> {
                recordingButton.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp)
                recordingButtonSubMenu.visibility = View.GONE
            }
            Keys.STATE_TRACKING_ACTIVE -> {
                recordingButton.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp)
                recordingButtonSubMenu.visibility = View.GONE
            }
            Keys.STATE_TRACKING_STOPPED -> {
                recordingButton.setImageResource(R.drawable.ic_save_white_24dp)
            }
        }
    }


    /* Toggles visibility of recording button sub menu_bottom_navigation */
    fun toggleRecordingButtonSubMenu() {
        when (recordingButtonSubMenu.visibility) {
            View.VISIBLE -> recordingButtonSubMenu.visibility = View.GONE
            else -> recordingButtonSubMenu.visibility = View.VISIBLE
        }
    }



    /* Toggles content and visibility of the location error snackbar */
    fun toggleLocationErrorBar(gpsProviderActive: Boolean, networkProviderActive: Boolean) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // CASE: Location permission not granted
            locationErrorBar.setText(R.string.snackbar_message_location_permission_denied)
            locationErrorBar.show()
        } else if (!gpsProviderActive && !networkProviderActive) {
            // CASE: Location setting is off
            locationErrorBar.setText(R.string.snackbar_message_location_offline)
            locationErrorBar.show()
        } else if (locationErrorBar.isShown) {
            // CASE: Snackbar is visible but unnecessary
            locationErrorBar.dismiss()
        }
    }

}