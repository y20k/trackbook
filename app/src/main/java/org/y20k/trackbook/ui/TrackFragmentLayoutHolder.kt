/*
 * TrackFragmentLayoutHolder.kt
 * Implements the TrackFragmentLayoutHolder class
 * A TrackFragmentLayoutHolder hold references to the main views of a track fragment
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

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.osmdroid.api.IGeoPoint
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
import org.y20k.trackbook.helpers.*
import kotlin.math.roundToInt


/*
 * TrackFragmentLayoutHolder class
 */
data class TrackFragmentLayoutHolder(var context: Context, var inflater: LayoutInflater, var container: ViewGroup?, var track: Track) {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TrackFragmentLayoutHolder::class.java)


    /* Main class variables */
    val rootView: View
    val shareButton: ImageButton
    val deleteButton: ImageButton
    val editButton: ImageButton
    val trackNameView: MaterialTextView
    private val mapView: MapView
    private var trackOverlay: ItemizedIconOverlay<OverlayItem>?
    private var controller: IMapController
    private var zoomLevel: Double
    private val statisticsSheetBehavior: BottomSheetBehavior<View>
    private val statisticsSheet: NestedScrollView
    private val statisticsView: View
    private val distanceView: MaterialTextView
    private val stepsView: MaterialTextView
    private val waypointsView: MaterialTextView
    private val durationView: MaterialTextView
    private val recordingStartView: MaterialTextView
    private val recordingStopView: MaterialTextView
    private val maxAltitudeView: MaterialTextView
    private val minAltitudeView: MaterialTextView
    private val positiveElevationView: MaterialTextView
    private val negativeElevationView: MaterialTextView
    private val elevationDataViews: Group
    private val trackManagementViews: Group
    private val useImperialUnits: Boolean


    /* Init block */
    init {
        // find views
        rootView = inflater.inflate(R.layout.fragment_track, container, false)
        mapView = rootView.findViewById(R.id.map)
        shareButton = rootView.findViewById(R.id.save_button)
        deleteButton = rootView.findViewById(R.id.delete_button)
        editButton = rootView.findViewById(R.id.edit_button)
        trackNameView = rootView.findViewById(R.id.statistics_track_name_headline)

        // basic map setup
        controller = mapView.controller
        mapView.isTilesScaledToDpi = true
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        zoomLevel = Keys.DEFAULT_ZOOM_LEVEL
        controller.setZoom(zoomLevel)

        // get views for statistics sheet
        statisticsSheet = rootView.findViewById(R.id.statistics_sheet)
        statisticsView = rootView.findViewById(R.id.statistics_view)
        distanceView = rootView.findViewById(R.id.statistics_data_distance)
        stepsView = rootView.findViewById(R.id.statistics_data_steps)
        waypointsView = rootView.findViewById(R.id.statistics_data_waypoints)
        durationView = rootView.findViewById(R.id.statistics_data_duration)
        recordingStartView = rootView.findViewById(R.id.statistics_data_recording_start)
        recordingStopView = rootView.findViewById(R.id.statistics_data_recording_stop)
        maxAltitudeView = rootView.findViewById(R.id.statistics_data_max_altitude)
        minAltitudeView = rootView.findViewById(R.id.statistics_data_min_altitude)
        positiveElevationView = rootView.findViewById(R.id.statistics_data_positive_elevation)
        negativeElevationView = rootView.findViewById(R.id.statistics_data_negative_elevation)
        elevationDataViews = rootView.findViewById(R.id.elevation_data)
        trackManagementViews = rootView.findViewById(R.id.management_icons)

        // get measurement unit system
        useImperialUnits = PreferencesHelper.loadUseImperialUnits(context)

        // set dark map tiles, if necessary
        if (AppThemeHelper.isDarkModeOn(context as Activity)) {
            mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }

        // add compass to map
        val compassOverlay = CompassOverlay(context, InternalCompassOrientationProvider(context), mapView)
        compassOverlay.enableCompass()
        compassOverlay.setCompassCenter(36f, 60f)
        mapView.overlays.add(compassOverlay)

        // create map overlay
        trackOverlay = MapHelper.createTrackOverlay(context, track, Keys.STATE_TRACKING_NOT)
        if (track.wayPoints.isNotEmpty()) {
            mapView.overlays.add(trackOverlay)
        }

        // set up and show statistics sheet
        statisticsSheetBehavior = BottomSheetBehavior.from<View>(statisticsSheet)
        statisticsSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        statisticsSheetBehavior.addBottomSheetCallback(getStatisticsSheetCallback())
        setupStatisticsViews()
    }


    /* Updates zoom level and center of this map */
    fun updateMapView() {
        val position = GeoPoint(track.latitude, track.longitude)
        controller.setCenter(position)
        controller.setZoom(track.zoomLevel)
    }


    /* Saves zoom level and center of this map */
    fun saveViewStateToTrack() {
        val center: IGeoPoint = mapView.mapCenter
        track.latitude = center.latitude
        track.longitude = center.longitude
        track.zoomLevel = mapView.zoomLevelDouble
        GlobalScope.launch { FileHelper.saveTrackSuspended(track, false) }
    }


    /* Sets up the statistics sheet */
    private fun setupStatisticsViews() {

        // get step count string
        val steps: String
        if (track.stepCount == -1f) steps = context.getString(R.string.statistics_sheet_p_steps_no_pedometer)
        else steps = track.stepCount.roundToInt().toString()

        // populate views
        trackNameView.text = track.name
        distanceView.text = LengthUnitHelper.convertDistanceToString(track.length, useImperialUnits)
        stepsView.text = steps
        waypointsView.text = track.wayPoints.size.toString()
        durationView.text = DateTimeHelper.convertToReadableTime(context, track.duration)
        recordingStartView.text = DateTimeHelper.convertToReadableDate(track.recordingStart)
        recordingStopView.text = DateTimeHelper.convertToReadableDate(track.recordingStart)
        maxAltitudeView.text = LengthUnitHelper.convertDistanceToString(track.maxAltitude, useImperialUnits)
        minAltitudeView.text = LengthUnitHelper.convertDistanceToString(track.minAltitude, useImperialUnits)
        positiveElevationView.text = LengthUnitHelper.convertDistanceToString(track.positiveElevation, useImperialUnits)
        negativeElevationView.text = LengthUnitHelper.convertDistanceToString(track.negativeElevation, useImperialUnits)

        // inform user about possible accuracy issues with altitude measurements
        elevationDataViews.referencedIds.forEach { id ->
            (rootView.findViewById(id) as View).setOnClickListener{
                Toast.makeText(context, R.string.toast_message_elevation_info, Toast.LENGTH_LONG).show()
            }
        }
        // make track name on statistics sheet clickable
        trackNameView.setOnClickListener {
            toggleStatisticsSheetVisibility()
        }
    }


    /* Shows/hides the statistics sheet */
    private fun toggleStatisticsSheetVisibility() {
        when (statisticsSheetBehavior.state) {
            BottomSheetBehavior.STATE_EXPANDED -> statisticsSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            else -> statisticsSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }


    /* Defines the behavior of the statistics sheet  */
    private fun getStatisticsSheetCallback(): BottomSheetBehavior.BottomSheetCallback {
        return object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        statisticsSheet.background = context.getDrawable(R.drawable.shape_statistics_background_expanded)
                        trackManagementViews.visibility = View.VISIBLE
                        shareButton.visibility = View.GONE
                        // bottomSheet.setPadding(0,24,0,0)
                    }
                    else -> {
                        statisticsSheet.background = context.getDrawable(R.drawable.shape_statistics_background_collapsed)
                        trackManagementViews.visibility = View.GONE
                        shareButton.visibility = View.VISIBLE
                        // bottomSheet.setPadding(0,0,0,0)
                    }
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset < 0.125f) {
                    statisticsSheet.background = context.getDrawable(R.drawable.shape_statistics_background_collapsed)
                    trackManagementViews.visibility = View.GONE
                    shareButton.visibility = View.VISIBLE
                } else {
                    statisticsSheet.background = context.getDrawable(R.drawable.shape_statistics_background_expanded)
                    trackManagementViews.visibility = View.VISIBLE
                    shareButton.visibility = View.GONE
                }
            }
        }
    }


}