/*
 * WayPoint.kt
 * Implements the WayPoint data class
 * A WayPoint stores a location plus additional metadata
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


package org.y20k.trackbook.core

import android.location.Location
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize


/*
 * WayPoint data class
 */
@Keep
@Parcelize
data class WayPoint(@Expose val provider: String,
                    @Expose val latitude: Double,
                    @Expose val longitude: Double,
                    @Expose val altitude: Double,
                    @Expose val accuracy: Float,
                    @Expose val time: Long,
                    @Expose val distanceToStartingPoint: Float = 0f,
                    @Expose val numberSatellites: Int = 0,
                    @Expose var isStopOver: Boolean = false,
                    @Expose var starred: Boolean = false): Parcelable {

    /* Constructor using just Location */
    constructor(location: Location) : this (location.provider, location.latitude, location.longitude, location. altitude, location.accuracy, location.time)

    /* Constructor using Location plus distanceToStartingPoint and numberSatellites */
    constructor(location: Location, distanceToStartingPoint: Float, numberSatellites: Int) : this (location.provider, location.latitude, location.longitude, location. altitude, location.accuracy, location.time, distanceToStartingPoint, numberSatellites)

    /* Converts WayPoint into Location */
    fun toLocation(): Location {
        val location: Location = Location(provider)
        location.latitude = latitude
        location.longitude = longitude
        location.altitude = altitude
        location.accuracy = accuracy
        location.time = time
        return location
    }

}
