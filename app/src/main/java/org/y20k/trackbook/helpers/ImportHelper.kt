/*
 * ImportHelper.kt
 * Implements the ImportHelper object
 * A ImportHelper manages the one-time import of old .trackbook files
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
import androidx.annotation.Keep
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.y20k.trackbook.Keys
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.core.WayPoint
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*


/*
 * ImportHelper data class
 */
object ImportHelper {


    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(ImportHelper::class.java)



    /* Converts older tracks of type .trackbook into the new format  */
    fun convertOldTracks(context: Context) {
        val oldTracks: ArrayList<Track> = arrayListOf()
        val trackFolder: File? = context.getExternalFilesDir(Keys.FOLDER_TRACKS)

        if (trackFolder != null && trackFolder.exists() && trackFolder.isDirectory) {
            trackFolder.listFiles()?.forEach { file ->
                if (file.name.endsWith(".trackbook")) {
                    // read until last line reached
                    val stream: InputStream = file.inputStream()
                    val reader: BufferedReader = BufferedReader(InputStreamReader(stream))
                    val builder: StringBuilder = StringBuilder()
                    reader.forEachLine {
                        builder.append(it)
                        builder.append("\n") }
                    stream.close()
                    // get content of file
                    val fileContent: String = builder.toString()
                    // get LegacyTrack from JSON
                    val gsonBuilder = GsonBuilder()
                    gsonBuilder.setDateFormat("M/d/yy hh:mm a")
                    val oldTrack: LegacyTrack = gsonBuilder.create().fromJson(fileContent, LegacyTrack::class.java)
                    oldTracks.add(oldTrack.toTrack())
                }
            }
        }

        // save track using "deferred await"
        if (oldTracks.isNotEmpty()) {
            GlobalScope.launch {
                oldTracks.forEach { oldTrack ->
                    // step 1: create and store filenames for json and gpx files
                    oldTrack.trackUriString = FileHelper.getTrackFileUri(context, oldTrack).toString()
                    oldTrack.gpxUriString = FileHelper.getGpxFileUri(context, oldTrack).toString()
                    // step 2: save track
                    FileHelper.saveTrackSuspended(oldTrack, saveGpxToo = true)
                    // step 3: save tracklist
                    FileHelper.addTrackAndSaveTracklistSuspended(context, oldTrack)
                }
            }
        }

    }


    /*
     * Inner class: Legacy version of Track - used for one-time import only
     * Warning: Works only as long as targetSdkVersion < 28
     */
    @Keep
    private data class LegacyTrack (
        @SerializedName("b") var mTrackFormatVersion: Int = 0,
        @SerializedName("c") var mWayPoints: List<LegacyWayPoint>,
        @SerializedName("d") var mTrackLength: Float = 0f,
        @SerializedName("e") var mDuration: Long = 0,
        @SerializedName("f") var mStepCount: Float = 0f,
        @SerializedName("g") var mRecordingStart: Date = GregorianCalendar.getInstance().time,
        @SerializedName("h") var mRecordingStop: Date = mRecordingStart,
        @SerializedName("i") var mMaxAltitude: Double = 0.0,
        @SerializedName("j") var mMinAltitude: Double = 0.0,
        @SerializedName("k") var mPositiveElevation: Double = 0.0,
        @SerializedName("l") var mNegativeElevation: Double = 0.0) {


        /* Converts */
        fun toTrack():Track {
            val track: Track = Track()
            track.trackFormatVersion = mTrackFormatVersion
            mWayPoints.forEach { legacyWayPoint ->
                val wayPoint: WayPoint= WayPoint(
                    provider = legacyWayPoint.mLocation.provider,
                    latitude = legacyWayPoint.mLocation.latitude,
                    longitude = legacyWayPoint.mLocation.longitude,
                    altitude = legacyWayPoint.mLocation.altitude,
                    accuracy = legacyWayPoint.mLocation.accuracy,
                    time = legacyWayPoint.mLocation.time,
                    distanceToStartingPoint = legacyWayPoint.mDistanceToStartingPoint,
                    numberSatellites = legacyWayPoint.mNumberSatellites,
                    isStopOver = legacyWayPoint.mIsStopOver
                )
                track.wayPoints.add(wayPoint)
            }
            track.length = mTrackLength
            track.duration = mDuration
            track.stepCount = mStepCount
            track.recordingStart = mRecordingStart
            track.recordingStop = mRecordingStop
            track.maxAltitude = mMaxAltitude
            track.minAltitude = mMinAltitude
            track.positiveElevation = mPositiveElevation
            track.negativeElevation = mNegativeElevation
            track.latitude = track.wayPoints[0].latitude
            track.longitude = track.wayPoints[0].longitude
            track.name = DateTimeHelper.convertToReadableDate(mRecordingStart)
            return track
        }

    }
    /*
     * End of inner class
     */


    /*
     * Inner class: Legacy version of WayPoint - used for one-time import only
     * Warning: Works only as long as targetSdkVersion < 28
     */
    @Keep
    private data class LegacyWayPoint (
        @SerializedName("a") var mLocation: Location,
        @SerializedName("b") var mIsStopOver: Boolean = false,
        @SerializedName("c") var mDistanceToStartingPoint: Float = 0f,
        @SerializedName("d") var mNumberSatellites: Int = 0) {
    }
    /*
     * End of inner class
     */



}