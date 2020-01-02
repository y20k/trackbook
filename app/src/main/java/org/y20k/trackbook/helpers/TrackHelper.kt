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

import android.location.Location
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
    fun getTrackId(track: Track): Long {
        return track.recordingStart.time
    }


    /* Returns unique ID for TracklistElement - currently the start date */
    fun getTrackId(tracklistElement: TracklistElement): Long {
        return tracklistElement.date.time
    }


    /* Adds given locatiom as waypoint to track */
    fun addWayPointToTrack(track: Track, location: Location, locationAccuracyThreshold: Int): Track {

        // get previous location
        val previousLocation: Location?
        val numberOfWayPoints: Int = track.wayPoints.size
        if (numberOfWayPoints == 0) {
            previousLocation = null
        } else {
            previousLocation = track.wayPoints.get(numberOfWayPoints - 1).toLocation()
        }

        // update duration
        val now: Date = GregorianCalendar.getInstance().time
        val difference: Long = now.time - track.recordingStop.time
        track.duration = track.duration + difference
        track.recordingStop = now

        // add only if recent and accurate
        val shouldBeAdded: Boolean
        shouldBeAdded = (LocationHelper.isRecentEnough(location)
                && LocationHelper.isAccurateEnough(location, locationAccuracyThreshold)
                && LocationHelper.isDifferentEnough(previousLocation, location))

        if (shouldBeAdded) {
            // update distance
            track.length = track.length + LocationHelper.calculateDistance(previousLocation, location)

            if (location.altitude != 0.0) {
                // update altitude values
                if (numberOfWayPoints == 0) {
                    track.maxAltitude = location.altitude
                    track.minAltitude = location.altitude
                } else {
                    // calculate elevation values
                    val elevationDifferences: Pair<Double, Double> = LocationHelper.calculateElevationDifferences(previousLocation, location, track)
                    // check if significant differences were calculated
                    if (elevationDifferences != Pair(track.positiveElevation, track.negativeElevation)) {
                        // update altitude values
                        if (location.altitude > track.maxAltitude) track.maxAltitude = location.altitude
                        if (location.altitude < track.minAltitude) track.minAltitude = location.altitude
                        // update elevation values
                        track.positiveElevation = elevationDifferences.first
                        track.negativeElevation = elevationDifferences.second
                    }
                }
            }

            // toggle stop over status, if necessary
            if (track.wayPoints.size < 0) {
                track.wayPoints[track.wayPoints.size - 1].isStopOver = LocationHelper.isStopOver(previousLocation, location)
            }

            // save number of satellites
            val numberOfSatellites: Int
            val extras = location.extras
            if (extras != null && extras.containsKey("satellites")) {
                numberOfSatellites = extras.getInt("satellites", 0)
            } else {
                numberOfSatellites = 0
            }

            // add current location as point to center on for later display
            track.latitude = location.latitude
            track.longitude = location.longitude

            // add location as new waypoint
            track.wayPoints.add(WayPoint(location.provider, location.latitude, location.longitude, location.altitude, location.accuracy, location.time, track.length, numberOfSatellites))
        }

        return track
    }


    /* Creates GPX string for given track */
    fun createGpxString(track: Track): String {
        var gpxString: String

        // add header
        gpxString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                    "<gpx version=\"1.1\" creator=\"Transistor App (Android)\"\n" +
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



}