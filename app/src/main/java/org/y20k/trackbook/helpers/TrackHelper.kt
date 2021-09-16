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
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.y20k.trackbook.Keys
import org.y20k.trackbook.R
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.core.Tracklist
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
    fun getTrackId(track: Track): Long = track.recordingStart.time


    /* Returns unique ID for TracklistElement - currently the start date */
    fun getTrackId(tracklistElement: TracklistElement): Long = tracklistElement.date.time


    /* Adds given locatiom as waypoint to track */
    fun addWayPointToTrack(track: Track, location: Location, accuracyMultiplier: Int, resumed: Boolean): Pair<Boolean, Track> {
        // Step 1: Get previous location
        val previousLocation: Location?
        var numberOfWayPoints: Int = track.wayPoints.size

        // CASE: First location
        if (numberOfWayPoints == 0) {
            previousLocation = null
        }
        // CASE: Second location - check if first location was plausible & remove implausible location
        else if (numberOfWayPoints == 1 && !LocationHelper.isFirstLocationPlausible(location, track)) {
            previousLocation = null
            numberOfWayPoints = 0
            track.wayPoints.removeAt(0)
        }
        // CASE: Third location or second location (if first was plausible)
        else {
            previousLocation = track.wayPoints[numberOfWayPoints - 1].toLocation()
        }

        // Step 2: Update duration
        val now: Date = GregorianCalendar.getInstance().time
        val difference: Long = now.time - track.recordingStop.time
        track.duration = track.duration + difference
        track.recordingStop = now

        // Step 3: Add waypoint, ifrecent and accurate and different enough
        val shouldBeAdded: Boolean = (LocationHelper.isRecentEnough(location) &&
                                      LocationHelper.isAccurateEnough(location, Keys.DEFAULT_THRESHOLD_LOCATION_ACCURACY) &&
                                      LocationHelper.isDifferentEnough(previousLocation, location, accuracyMultiplier))
        if (shouldBeAdded) {
            // Step 3.1: Update distance (do not update if resumed -> we do not want to add values calculated during a recording pause)
            if (!resumed) {
                track.length = track.length + LocationHelper.calculateDistance(previousLocation, location)
            }
            // Step 3.2: Update altitude values
            val altitude: Double = location.altitude
            if (altitude != 0.0) {
                if (numberOfWayPoints == 0) {
                    track.maxAltitude = altitude
                    track.minAltitude = altitude
                }
                else {
                    if (altitude > track.maxAltitude) track.maxAltitude = altitude
                    if (altitude < track.minAltitude) track.minAltitude = altitude
                }
            }
            // Step 3.3: Toggle stop over status, if necessary
            if (track.wayPoints.size < 0) {
                track.wayPoints[track.wayPoints.size - 1].isStopOver = LocationHelper.isStopOver(previousLocation, location)
            }

            // Step 3.4: Add current location as point to center on for later display
            track.latitude = location.latitude
            track.longitude = location.longitude

            // Step 3.5: Add location as new waypoint
            track.wayPoints.add(WayPoint(location = location, distanceToStartingPoint = track.length))
        }

        return Pair(shouldBeAdded, track)
    }


    /* Calculates time passed since last stop of recording */
    fun calculateDurationOfPause(recordingStop: Date): Long = GregorianCalendar.getInstance().time.time - recordingStop.time


    /* Creates GPX string for given track */
    fun createGpxString(track: Track): String {
        var gpxString: String

        // add header
        gpxString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                    "<gpx version=\"1.1\" creator=\"Trackbook App (Android)\"\n" +
                    "     xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                    "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n"

        // add name
        gpxString += createGpxName(track)

        // add POIs
        gpxString += createGpxPois(track)

        // add track
        gpxString += createGpxTrk(track)

        // add closing tag
        gpxString += "</gpx>\n"

        return gpxString
    }


    /* Creates name for GPX file */
    private fun createGpxName(track: Track): String {
        val gpxName = StringBuilder("")
        gpxName.append("\t<metadata>\n")
        gpxName.append("\t\t<name>")
        gpxName.append("Trackbook Recording: ${track.name}")
        gpxName.append("</name>\n")
        gpxName.append("\t</metadata>\n")
        return gpxName.toString()
    }


    /* Creates GPX formatted points of interest */
    private fun createGpxPois(track: Track): String {
        val gpxPois = StringBuilder("")
        val poiList: List<WayPoint> =  track.wayPoints.filter { it.starred }
        poiList.forEach { poi ->
            gpxPois.append("\t<wpt lat=\"")
            gpxPois.append(poi.latitude)
            gpxPois.append("\" lon=\"")
            gpxPois.append(poi.longitude)
            gpxPois.append("\">\n")

            // add name to waypoint
            gpxPois.append("\t\t<name>")
            gpxPois.append("Point of interest")
            gpxPois.append("</name>\n")

            // add altitude
            gpxPois.append("\t\t<ele>")
            gpxPois.append(poi.altitude)
            gpxPois.append("</ele>\n")

            // add closing tag
            gpxPois.append("\t</wpt>\n")
        }
        return gpxPois.toString()
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
        gpxTrack.append("Track")
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

            // add altitude
            gpxTrack.append("\t\t\t\t<ele>")
            gpxTrack.append(wayPoint.altitude)
            gpxTrack.append("</ele>\n")

            // add time
            gpxTrack.append("\t\t\t\t<time>")
            gpxTrack.append(dateFormat.format(Date(wayPoint.time)))
            gpxTrack.append("</time>\n")

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
                    true -> Toast.makeText(context, R.string.toast_message_poi_added, Toast.LENGTH_LONG).show()
                    false -> Toast.makeText(context, R.string.toast_message_poi_removed, Toast.LENGTH_LONG).show()
                }
            }
        }
        return track
    }


    /* Calculates total distance, duration and pause */
    fun calculateAndSaveTrackTotals(context: Context, tracklist: Tracklist) {
        CoroutineScope(IO).launch {
            var totalDistanceAll: Float = 0f
//            var totalDurationAll: Long = 0L
//            var totalRecordingPausedAll: Long = 0L
//            var totalStepCountAll: Float = 0f
            tracklist.tracklistElements.forEach { tracklistElement ->
                val track: Track = FileHelper.readTrack(context, tracklistElement.trackUriString.toUri())
                totalDistanceAll += track.length
//                totalDurationAll += track.duration
//                totalRecordingPausedAll += track.recordingPaused
//                totalStepCountAll += track.stepCount
            }
            tracklist.totalDistanceAll = totalDistanceAll
//            tracklist.totalDurationAll = totalDurationAll
//            tracklist.totalRecordingPausedAll = totalRecordingPausedAll
//            tracklist.totalStepCountAll = totalStepCountAll
            FileHelper.saveTracklistSuspended(context, tracklist, GregorianCalendar.getInstance().time)
        }
    }

}
