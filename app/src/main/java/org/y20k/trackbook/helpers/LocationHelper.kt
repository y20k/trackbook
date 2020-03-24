/*
 * LocationHelper.kt
 * Implements the LocationHelper object
 * A LocationHelper offers helper methods for dealing with location issues
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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import org.y20k.trackbook.Keys
import org.y20k.trackbook.core.Track
import java.util.*


/*
 * Keys object
 */
object LocationHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(LocationHelper::class.java)


    /* Get default location */
    fun getDefaultLocation(): Location {
        val defaultLocation: Location = Location(LocationManager.NETWORK_PROVIDER)
        defaultLocation.latitude = Keys.DEFAULT_LATITUDE
        defaultLocation.longitude = Keys.DEFAULT_LONGITUDE
        defaultLocation.accuracy = Keys.DEFAULT_ACCURACY
        defaultLocation.altitude = Keys.DEFAULT_ALTITUDE
        defaultLocation.time = Keys.DEFAULT_DATE.time
        return defaultLocation
    }


    /* Checks if a location is older than one minute */
    fun isOldLocation(location: Location): Boolean {
        // check how many milliseconds the given location is old
        return GregorianCalendar.getInstance().time.time - location.time > Keys.SIGNIFICANT_TIME_DIFFERENCE
    }


    /* Tries to return the last location that the system has stored */
    fun getLastKnownLocation(context: Context): Location {
        // get last location that Trackbook has stored
        var lastKnownLocation: Location = PreferencesHelper.loadCurrentBestLocation(context)
        // try to get the last location the system has stored - it is probably more recent
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val lastKnownLocationGps: Location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: lastKnownLocation
            val lastKnownLocationNetwork: Location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: lastKnownLocation
            when (isBetterLocation(lastKnownLocationGps, lastKnownLocationNetwork)) {
                true -> lastKnownLocation = lastKnownLocationGps
                false -> lastKnownLocation = lastKnownLocationNetwork
            }
        }
        return lastKnownLocation
    }


    /* Determines whether one Location reading is better than the current Location fix */
    fun isBetterLocation(location: Location, currentBestLocation: Location?): Boolean {
        // Credit: https://developer.android.com/guide/topics/location/strategies.html#BestEstimate

        if (currentBestLocation == null) {
            // a new location is always better than no location
            return true
        }

        // check whether the new location fix is newer or older
        val timeDelta: Long = location.time - currentBestLocation.time
        val isSignificantlyNewer: Boolean = timeDelta > Keys.SIGNIFICANT_TIME_DIFFERENCE
        val isSignificantlyOlder:Boolean = timeDelta < -Keys.SIGNIFICANT_TIME_DIFFERENCE

        when {
            // if it's been more than two minutes since the current location, use the new location because the user has likely moved
            isSignificantlyNewer -> return true
            // if the new location is more than two minutes older, it must be worse
            isSignificantlyOlder -> return false
        }

        // check whether the new location fix is more or less accurate
        val isNewer: Boolean = timeDelta > 0L
        val accuracyDelta: Float = location.accuracy - currentBestLocation.accuracy
        val isLessAccurate: Boolean = accuracyDelta > 0f
        val isMoreAccurate: Boolean = accuracyDelta < 0f
        val isSignificantlyLessAccurate: Boolean = accuracyDelta > 200f

        // check if the old and new location are from the same provider
        val isFromSameProvider: Boolean = location.provider == currentBestLocation.provider

        // determine location quality using a combination of timeliness and accuracy
        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && !isSignificantlyLessAccurate && isFromSameProvider -> true
            else -> false
        }
    }


    /* Checks if GPS location provider is available and enabled */
    fun isGpsEnabled(locationManager: LocationManager): Boolean {
        if (locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } else {
            return false
        }
    }


    /* Checks if Network location provider is available and enabled */
    fun isNetworkEnabled(locationManager: LocationManager): Boolean {
        if (locationManager.allProviders.contains(LocationManager.NETWORK_PROVIDER)) {
            return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } else {
            return false
        }
    }



    /* Checks if given location is new */
    fun isRecentEnough(location: Location): Boolean {
        val locationAge: Long = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return locationAge < Keys.DEFAULT_THRESHOLD_LOCATION_AGE
    }


    /* Checks if given location is accurate */
    fun isAccurateEnough(location: Location, locationAccuracyThreshold: Int): Boolean {
        val isAccurate: Boolean
        when (location.provider) {
            LocationManager.GPS_PROVIDER -> isAccurate = location.accuracy < locationAccuracyThreshold
            else -> isAccurate = location.accuracy < locationAccuracyThreshold + 10 // a bit more relaxed when location comes from network provider
        }
        return isAccurate
    }


    /* Checks if the first location of track is plausible */
    fun isFirstLocationPlausible(secondLocation: Location, track: Track): Boolean {
        // speed in km/h
        val speed: Double = calculateSpeed(firstLocation = track.wayPoints[0].toLocation(), secondLocation = secondLocation, firstTimestamp = track.recordingStart.time, secondTimestamp = GregorianCalendar.getInstance().time.time)
        // plausible = speed under 250 km/h
        return speed < Keys.IMPLAUSIBLE_TRACK_START_SPEED
    }


    /* Calculates speed */
    private fun calculateSpeed(firstLocation: Location, secondLocation: Location, firstTimestamp: Long, secondTimestamp: Long, useImperial: Boolean = false): Double {
        // time difference in seconds
        val timeDifference: Long = (secondTimestamp - firstTimestamp) / 1000L
        // distance in meters
        val distance: Float = calculateDistance(firstLocation, secondLocation)
        // speed in either km/h (default) or mph
        return LengthUnitHelper.convertMetersPerSecond(distance / timeDifference, useImperial)
    }


    /* Checks if given location is different enough compared to previous location */
    fun isDifferentEnough(previousLocation: Location?, location: Location): Boolean {
        // check if previous location is (not) available
        if (previousLocation == null) return true
        // check if distance between is large enough
        val distanceThreshold: Float
        val averageAccuracy: Float = (previousLocation.accuracy + location.accuracy) / 2
        // increase the distance threshold if one or both locations are
        if (averageAccuracy > Keys.DEFAULT_THRESHOLD_DISTANCE) {
            distanceThreshold = averageAccuracy
        } else {
            distanceThreshold = Keys.DEFAULT_THRESHOLD_DISTANCE
        }
        // location is different when far enough away from previous location
        return calculateDistance(previousLocation, location) > distanceThreshold
    }


    /* Calculates distance in meters between two locations */
    fun calculateDistance(previousLocation: Location?, location: Location): Float  {
        var distance: Float = 0f
        // two data points needed to calculate distance
        if (previousLocation != null) {
            // add up distance
            distance = previousLocation.distanceTo(location)
        }
        return distance
    }


    /* Calculate elevation differences */
    fun calculateElevationDifferences(previousLocation: Location?, location: Location, track: Track): Pair<Double, Double> {
        // store current values
        var positiveElevation: Double = track.positiveElevation
        var negativeElevation: Double = track.negativeElevation
        if (previousLocation != null) {
            // factor is bigger than 1 if the time stamp difference is larger than the movement recording interval
            val timeDifferenceFactor: Long = (location.time - previousLocation.time) / Keys.ADD_WAYPOINT_TO_TRACK_INTERVAL
            // get elevation difference and sum it up
            val altitudeDifference: Double = location.altitude - previousLocation.altitude
            if (altitudeDifference > 0 && altitudeDifference < Keys.ALTITUDE_MEASUREMENT_ERROR_THRESHOLD * timeDifferenceFactor && location.altitude != Keys.DEFAULT_ALTITUDE) {
                positiveElevation = track.positiveElevation + altitudeDifference // upwards movement
            }
            if (altitudeDifference < 0 && altitudeDifference > -Keys.ALTITUDE_MEASUREMENT_ERROR_THRESHOLD * timeDifferenceFactor && location.altitude != Keys.DEFAULT_ALTITUDE) {
                negativeElevation = track.negativeElevation + altitudeDifference // downwards movement
            }
        }
        return Pair(positiveElevation, negativeElevation)
    }


    /* Checks if given location is a stop over */
    fun isStopOver(previousLocation: Location?, location: Location): Boolean {
        if (previousLocation == null) return false
        // check how many milliseconds the given locations are apart
        return location.time - previousLocation.time > Keys.STOP_OVER_THRESHOLD
    }


}