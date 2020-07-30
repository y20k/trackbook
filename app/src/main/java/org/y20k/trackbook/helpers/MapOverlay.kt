/*
 * MapHelper.kt
 * Implements the MapHelper object
 * A MapHelper offers helper methods for manipulating osmdroid maps
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
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Vibrator
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import org.y20k.trackbook.Keys
import org.y20k.trackbook.R
import org.y20k.trackbook.core.Track
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*


/*
 * MapHelper class
 */
class MapOverlay (private var markerListener: MarkerListener)  {

    /* Interface used to communicate back to activity/fragment */
    interface MarkerListener {
        fun onMarkerTapped(latitude: Double, longitude: Double) {
        }
    }

    /* Define log tag */
    private val TAG = MapOverlay::class.java.simpleName


    /* Creates icon overlay for current position (used in MapFragment) */
    fun createMyLocationOverlay(context: Context, location: Location, trackingState: Int): ItemizedIconOverlay<OverlayItem> {

        val overlayItems = ArrayList<OverlayItem>()
        val locationIsOld = LocationHelper.isOldLocation(location)

        // create marker
        val newMarker: Drawable
        when (trackingState) {
            // CASE: Tracking active
            Keys.STATE_TRACKING_ACTIVE -> {
                when (locationIsOld) {
                    true -> newMarker = ContextCompat.getDrawable(context, R.drawable.ic_marker_location_red_grey_24dp)!!
                    false -> newMarker = ContextCompat.getDrawable(context, R.drawable.ic_marker_location_red_24dp)!!
                }
            }
            // CASE. Tracking is NOT active
            else -> {
                when (locationIsOld) {
                    true -> newMarker = ContextCompat.getDrawable(context, R.drawable.ic_marker_location_blue_grey_24dp)!!
                    false -> newMarker = ContextCompat.getDrawable(context, R.drawable.ic_marker_location_blue_24dp)!!
                }
            }
        }

        // add marker to list of overlay items
        val overlayItem = createOverlayItem(context, location.latitude, location.longitude, location.accuracy, location.provider, location.time)
        overlayItem.setMarker(newMarker)
        overlayItems.add(overlayItem)

        // create and return overlay for current position
        return createOverlay(context, overlayItems)
    }


    /* Creates icon overlay for track */
    fun createTrackOverlay(context: Context, track: Track, trackingState: Int): ItemizedIconOverlay<OverlayItem> {

        val overlayItems = ArrayList<OverlayItem>()
        val wayPoints = track.wayPoints

        wayPoints.forEach { wayPoint ->
            // create marker
            val newMarker: Drawable

            // get drawable
            when (trackingState) {
                // CASE: Recording is active
                Keys.STATE_TRACKING_ACTIVE -> {
                    if (wayPoint.starred) {
                        newMarker = ContextCompat.getDrawable(context, R.drawable.ic_star_red_24dp)!!
                    } else if (wayPoint.isStopOver) {
                        newMarker = ContextCompat.getDrawable(context, R.drawable.ic_marker_track_location_grey_24dp)!!
                    } else {
                        newMarker = ContextCompat.getDrawable(context, R.drawable.ic_marker_track_location_red_24dp)!!
                    }
                }
                // CASE: Recording is paused/stopped
                else -> {
                    if (wayPoint.starred) {
                        newMarker = ContextCompat.getDrawable(context, R.drawable.ic_star_blue_24dp)!!
                    } else if (wayPoint.isStopOver) {
                        newMarker = ContextCompat.getDrawable(context, R.drawable.ic_marker_track_location_grey_24dp)!!
                    } else {
                        newMarker = ContextCompat.getDrawable(context, R.drawable.ic_marker_track_location_blue_24dp)!!
                    }
                }
            }

            // create overlay item and add to list of overlay items
            val overlayItem = createOverlayItem(context, wayPoint.latitude, wayPoint.longitude, wayPoint.accuracy, wayPoint.provider, wayPoint.time)
            overlayItem.setMarker(newMarker)
            overlayItems.add(overlayItem)
        }

        // create and return overlay for current position
        return createOverlay(context, overlayItems)
    }


    /* Creates a marker overlay item */
    private fun createOverlayItem(context: Context, latitude: Double, longitude: Double, accuracy: Float, provider: String, time: Long): OverlayItem {
        val title: String = "${context.getString(R.string.marker_description_time)}: ${SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.getDefault()).format(time)}"
        //val description: String = "${context.getString(R.string.marker_description_accuracy)}: ${DecimalFormat("#0.00").format(accuracy)} (${provider})"
        val description: String = "${context.getString(R.string.marker_description_time)}: ${SimpleDateFormat.getTimeInstance(SimpleDateFormat.MEDIUM, Locale.getDefault()).format(time)} | ${context.getString(R.string.marker_description_accuracy)}: ${DecimalFormat("#0.00").format(accuracy)} (${provider})"
        val position: GeoPoint = GeoPoint(latitude, longitude)
        return OverlayItem(title, description, position)
    }


    /* Creates an overlay */
    private fun createOverlay(context: Context, overlayItems: ArrayList<OverlayItem>): ItemizedIconOverlay<OverlayItem> {
        return ItemizedIconOverlay<OverlayItem>(context, overlayItems,
            object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem): Boolean {
                    markerListener.onMarkerTapped(item.point.latitude, item.point.longitude)
                    return true
                }
                override fun onItemLongPress(index: Int, item: OverlayItem): Boolean {
                    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    v.vibrate(50)
                    Toast.makeText(context, item.snippet, Toast.LENGTH_LONG).show()
                    return true
                }
            })
    }

}