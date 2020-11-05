/*
 * LengthUnitHelper.kt
 * Implements the LengthUnitHelper object
 * A LengthUnitHelper offers helper methods for dealing with unit systems and locales
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

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*


/*
 * LengthUnitHelper object
 */
object LengthUnitHelper {


    /* Converts for the given unit system a distance value to a readable string */
    fun convertDistanceToString(distance: Float, useImperial: Boolean = false): String {
        return convertDistanceToString(distance.toDouble(), useImperial)
    }


    /* Converts for the given unit system a distance value to a readable string */
    fun convertDistanceToString(distance: Double, useImperial: Boolean = false): String {
        val readableDistance: Double
        val unit: String
        val numberFormat = NumberFormat.getNumberInstance()

        // check for locale and set unit system accordingly
        when (useImperial) {
            // CASE: miles and feet
            true -> {
                if (distance > 1610) {
                    // convert distance to miles
                    readableDistance = distance * 0.000621371192f
                    // set measurement unit
                    unit = "mi"
                    // set number precision
                    numberFormat.maximumFractionDigits = 2
                } else {
                    // convert distance to feet
                    readableDistance = distance * 3.28084f
                    // set measurement unit
                    unit = "ft"
                    // set number precision
                    numberFormat.maximumFractionDigits = 0
                }
            }
            // CASE: kilometer and meter
            false -> {
                if (distance >= 1000) {
                    // convert distance to kilometer
                    readableDistance = distance * 0.001f
                    // set measurement unit
                    unit = "km"
                    // set number precision
                    numberFormat.maximumFractionDigits = 2
                } else {
                    // no need to convert
                    readableDistance = distance
                    // set measurement unit
                    unit = "m"
                    // set number precision
                    numberFormat.maximumFractionDigits = 0
                }
            }
        }

        // format distance according to current locale
        return "${numberFormat.format(readableDistance)} $unit"
    }


    /* Determines which unit system the device is using (metric or imperial) */
    fun useImperialUnits(): Boolean {
        // America (US), Liberia (LR), Myanmar(MM) use the imperial system
        val imperialSystemCountries = Arrays.asList("US", "LR", "MM")
        val countryCode = Locale.getDefault().country
        return imperialSystemCountries.contains(countryCode)
    }


    /* Converts for the given unit System distance and duration values to a readable velocity string */
    fun convertToVelocityString(trackDuration: Long, trackRecordingPause: Long, trackLength: Float, useImperialUnits: Boolean = false) : String {
        var speed: String = "0"

        // duration minus pause in seconds
        val duration: Long = (trackDuration - trackRecordingPause) / 1000L

        if (duration > 0L) {
            // speed in km/h / mph
            val velocity: Double = convertMetersPerSecond((trackLength / duration), useImperialUnits)
            // create readable speed string
            var bd: BigDecimal = BigDecimal.valueOf(velocity)
            bd = bd.setScale(1, RoundingMode.HALF_UP)
            speed = bd.toPlainString()
        }

        return when (useImperialUnits) {
            true -> "$speed mph"
            false -> "$speed km/h"
        }
    }


    /* Coverts meters per second to either km/h or mph */
    fun convertMetersPerSecond(metersPerSecond: Float, useImperial: Boolean = false): Double {
        return if (useImperial) {
            // mph
            metersPerSecond * 2.2369362920544
        } else {
            // km/h
            metersPerSecond * 3.6
        }
    }


}