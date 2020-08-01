/*
 * TrackHelper.kt
 * Implements the TrackHelper object
 * A TrackHelper offers helper methods for dealing with track objects
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
import android.widget.Toast
import org.y20k.trackbook.R
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.core.TracklistElement
import org.y20k.trackbook.core.WayPoint
import java.text.SimpleDateFormat
import java.util.*


/*
 * TrackHelper object
 */
object TrackHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TrackHelper::class.java)


    /* Returns unique ID for Track - currently the start date */
    fun getTrackId(track: Track): Long =
        track.recordingStart.time


    /* Returns unique ID for TracklistElement - currently the start date */
    fun getTrackId(tracklistElement: TracklistElement): Long =
        tracklistElement.date.time


    /* Adds given location as waypoint to track */
    fun addWayPointToTrack(
        track: Track,
        location: Location,
        locationAccuracyThreshold: Int,
        resumed: Boolean
    ): Pair<Track, Boolean> {
        // get previous location
        val previousLocation: Location?
        var numberOfWayPoints: Int = track.wayPoints.size

        // CASE: First location
        if (numberOfWayPoints == 0) {
            previousLocation = null
        }
        // CASE: Second location - check if first location was plausible & remove implausible location
        else if (numberOfWayPoints == 1 && !LocationHelper.isFirstLocationPlausible(
                location,
                track
            )
        ) {
            previousLocation = null
            numberOfWayPoints = 0
            track.wayPoints.removeAt(0)
        }
        // CASE: Third location or second location (if first was plausible)
        else {
            previousLocation = track.wayPoints[numberOfWayPoints - 1].toLocation()
        }

        // update duration
        val now: Date = GregorianCalendar.getInstance().time
        val difference: Long = now.time - track.recordingStop.time
        track.duration = track.duration + difference
        track.recordingStop = now

        // add only if recent and accurate and different
        val shouldBeAdded: Boolean = (LocationHelper.isRecentEnough(location) &&
                LocationHelper.isAccurateEnough(location, locationAccuracyThreshold) &&
                LocationHelper.isDifferentEnough(previousLocation, location))

//        // Debugging for shouldBeAdded - remove for production
//        val recentEnough: Boolean = LocationHelper.isRecentEnough(location)
//        val accurateEnough: Boolean = LocationHelper.isAccurateEnough(location, locationAccuracyThreshold)
//        val differentEnough: Boolean = LocationHelper.isDifferentEnough(previousLocation, location)
//        val shouldBeAdded = recentEnough && accurateEnough && differentEnough
//        if (!recentEnough && accurateEnough && differentEnough) { Toast.makeText(context, "Debug: Not recent enough", Toast.LENGTH_LONG).show() }
//        else if (!accurateEnough && recentEnough && differentEnough) { Toast.makeText(context, "Debug: Not accurate enough", Toast.LENGTH_LONG).show() }
//        else if (!differentEnough && recentEnough && accurateEnough) { Toast.makeText(context, "Debug: Not different enough", Toast.LENGTH_LONG).show() }
//        else if (!recentEnough && !accurateEnough && differentEnough) { Toast.makeText(context, "Debug: Not recent and accurate enough", Toast.LENGTH_LONG).show() }
//        else if (!recentEnough && !differentEnough && accurateEnough) { Toast.makeText(context, "Debug: Not recent and different enough", Toast.LENGTH_LONG).show() }
//        else if (!accurateEnough && !differentEnough && recentEnough) { Toast.makeText(context, "Debug: Not accurate and different enough", Toast.LENGTH_LONG).show() }
//        else { Toast.makeText(context, "Debug: bad location.", Toast.LENGTH_LONG).show() }

        if (shouldBeAdded) {
            // update distance (do not update if resumed -> we do not want to add values calculated during a recording pause)
            if (!resumed) {
                track.length =
                    track.length + LocationHelper.calculateDistance(previousLocation, location)
            }

            if (location.altitude != 0.0) {
                // update altitude values
                if (numberOfWayPoints == 0) {
                    track.maxAltitude = location.altitude
                    track.minAltitude = location.altitude
                } else {
                    // calculate elevation values (upwards / downwards movements)
                    val elevationDifferences: Pair<Double, Double> =
                        LocationHelper.calculateElevationDifferences(
                            previousLocation,
                            location,
                            track
                        )
                    // check if any differences were calculated
                    if (elevationDifferences != Pair(
                            track.positiveElevation,
                            track.negativeElevation
                        )
                    ) {
                        // update altitude values
                        if (location.altitude > track.maxAltitude) track.maxAltitude =
                            location.altitude
                        if (location.altitude < track.minAltitude) track.minAltitude =
                            location.altitude
                        // update elevation values (do not update if resumed -> we do not want to add values calculated during a recording pause)
                        if (!resumed) {
                            track.positiveElevation = elevationDifferences.first
                            track.negativeElevation = elevationDifferences.second
                        }
                    }
                }
            }

            // toggle stop over status, if necessary
            if (track.wayPoints.size < 0) {
                track.wayPoints[track.wayPoints.size - 1].isStopOver =
                    LocationHelper.isStopOver(previousLocation, location)
            }

            // save number of satellites
            val numberOfSatellites: Int
            val extras = location.extras
            numberOfSatellites = if (extras != null && extras.containsKey("satellites")) {
                extras.getInt("satellites", 0)
            } else {
                0
            }

            // add current location as point to center on for later display
            track.latitude = location.latitude
            track.longitude = location.longitude

            // add location as new waypoint
            track.wayPoints.add(
                WayPoint(
                    provider = location.provider,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude,
                    accuracy = location.accuracy,
                    time = location.time,
                    distanceToStartingPoint = track.length,
                    numberSatellites = numberOfSatellites
                )
            )
        }

        return Pair(track, shouldBeAdded)
    }


    /* Calculates time passed since last stop of recording */
    fun calculateDurationOfPause(recordingStop: Date): Long =
        GregorianCalendar.getInstance().time.time - recordingStop.time


    /* Creates GPX string for given track */
    fun createGpxString(track: Track): String {

        // add header
        var gpxString: String = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                "<gpx version=\"1.1\" creator=\"Trackbook App (Android)\"\n" +
                "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n"

        // add track
        gpxString += createGpxTrk(track)

        // add closing tag
        gpxString += "</gpx>\n"

        return gpxString
    }


    /* Creates GPX formatted track */
    private fun createGpxTrk(track: Track): String {
        val gpxTrack = StringBuilder("")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        // add opening track tag
        gpxTrack.append("\t<trk>\n")

        // add name to track
        gpxTrack.append("\t\t<name>")
        gpxTrack.append("Trackbook Recording: ${track.name}")
        gpxTrack.append("</name>\n")

        // add opening track segment tag
        gpxTrack.append("\t\t<trkseg>\n")

        // add route point
        track.wayPoints.forEach { wayPoint ->
            // add longitude and latitude
            gpxTrack.append("\t\t\t<trkpt lat=\"")
            gpxTrack.append(wayPoint.latitude)
            gpxTrack.append("\" lon=\"")
            gpxTrack.append(wayPoint.longitude)
            gpxTrack.append("\">\n")

            // add time
            gpxTrack.append("\t\t\t\t<time>")
            gpxTrack.append(dateFormat.format(Date(wayPoint.time)))
            gpxTrack.append("</time>\n")

            // add altitude
            gpxTrack.append("\t\t\t\t<ele>")
            gpxTrack.append(wayPoint.altitude)
            gpxTrack.append("</ele>\n")

            // add closing tag
            gpxTrack.append("\t\t\t</trkpt>\n")
        }

        // add closing track segment tag
        gpxTrack.append("\t\t</trkseg>\n")

        // add closing track tag
        gpxTrack.append("\t</trk>\n")

        return gpxTrack.toString()
    }


    /* Toggles starred flag for given position */
    fun toggleStarred(context: Context, track: Track, latitude: Double, longitude: Double): Track {
        track.wayPoints.forEach { waypoint ->
            if (waypoint.latitude == latitude && waypoint.longitude == longitude) {
                waypoint.starred = !waypoint.starred
                when (waypoint.starred) {
                    true -> Toast.makeText(
                        context,
                        R.string.toast_message_poi_added,
                        Toast.LENGTH_LONG
                    ).show()
                    false -> Toast.makeText(
                        context,
                        R.string.toast_message_poi_removed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        return track
    }

}
