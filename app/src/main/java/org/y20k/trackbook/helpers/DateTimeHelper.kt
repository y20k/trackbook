/*
 * DateTimeHelper.kt
 * Implements the DateTimeHelper object
 * A DateTimeHelper provides helper methods for converting Date and Time objects
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-22 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */


package org.y20k.trackbook.helpers

import android.content.Context
import android.location.Location
import org.y20k.trackbook.Keys
import org.y20k.trackbook.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


/*
 * DateTimeHelper object
 */
object DateTimeHelper {

    /* Converts milliseconds to mm:ss or hh:mm:ss */
    fun convertToReadableTime(context: Context, milliseconds: Long, compactFormat: Boolean = false): String {
        val hours: Long = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes: Long = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1)
        val seconds: Long = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1)
        val timeString: String

        when (compactFormat) {

            // Compact tine format
            true -> {
                if (milliseconds < Keys.ONE_HOUR_IN_MILLISECONDS) {
                    // example: 23:45
                    val minutesString: String = minutes.toString()
                    val secondsString: String = seconds.toString().padStart(2, '0')
                    timeString = "$minutesString:$secondsString"
                } else {
                    // example: 1:23
                    val hoursString: String  = hours.toString()
                    val minutesString: String = minutes.toString()
                    timeString = "$hoursString:$minutesString"
                }
            }

            // Long time format
            false -> {
                if (milliseconds < Keys.ONE_HOUR_IN_MILLISECONDS) {
                    // example: 23 min 45 sec
                    val minutesString: String = minutes.toString()
                    val secondsString: String = seconds.toString()
                    val m: String = context.getString(R.string.abbreviation_minutes)
                    val s: String = context.getString(R.string.abbreviation_seconds)
                    timeString = "$minutesString $m $secondsString $s"
                } else {
                    // example: 1 hrs 23 min 45 sec
                    val hoursString: String = hours.toString()
                    val minutesString: String = minutes.toString()
                    val secondsString: String = seconds.toString()
                    val h: String = context.getString(R.string.abbreviation_hours)
                    val m: String = context.getString(R.string.abbreviation_minutes)
                    val s: String = context.getString(R.string.abbreviation_seconds)
                    timeString = "$hoursString $h $minutesString $m $secondsString $s"
                }
            }
        }

        return timeString
    }


    /* Create sortable string for date - used for filenames  */
    fun convertToSortableDateString(date: Date): String {
        val dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        return dateFormat.format(date)
    }


    /* Creates a readable string for date - used in the UI */
    fun convertToReadableDate(date: Date, dateStyle: Int = DateFormat.LONG): String {
        return DateFormat.getDateInstance(dateStyle, Locale.getDefault()).format(date)
    }


    /* Creates a readable string date and time - used in the UI */
    fun convertToReadableDateAndTime(date: Date, dateStyle: Int = DateFormat.SHORT, timeStyle: Int = DateFormat.SHORT): String {
        return "${DateFormat.getDateInstance(dateStyle, Locale.getDefault()).format(date)} ${DateFormat.getTimeInstance(timeStyle, Locale.getDefault()).format(date)}"
    }


    /* Calculates time difference between two locations */
    fun calculateTimeDistance(previousLocation: Location?, location: Location): Long  {
        var timeDifference: Long = 0L
        // two data points needed to calculate time difference
        if (previousLocation != null) {
            // get time difference
            timeDifference = location.time - previousLocation.time
        }
        return timeDifference
    }


}