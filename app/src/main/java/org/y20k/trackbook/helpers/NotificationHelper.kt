/*
 * NotificationHelper.kt
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
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

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import org.y20k.trackbook.Keys
import org.y20k.trackbook.MainActivity
import org.y20k.trackbook.R
import org.y20k.trackbook.TrackerService


/*
 * NotificationHelper class
 */
class NotificationHelper(private val trackerService: TrackerService) {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(NotificationHelper::class.java)


    /* Main class variables */
    private val notificationManager: NotificationManager = trackerService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


    /* Creates notification */
    fun createNotification(trackingState: Int, trackLength: Float, duration: Long, useImperial: Boolean): Notification {

        // create notification channel if necessary
        if (shouldCreateNotificationChannel()) {
            createNotificationChannel()
        }

        // Build notification
        val builder = NotificationCompat.Builder(trackerService, Keys.NOTIFICATION_CHANNEL_RECORDING)
        builder.setContentIntent(showActionPendingIntent)
        builder.setSmallIcon(R.drawable.ic_notification_icon_small_24dp)
        builder.setContentText(getContentString(trackerService, duration, trackLength, useImperial))

        // add icon and actions for stop, resume and show
        when (trackingState) {
            Keys.STATE_TRACKING_ACTIVE -> {
                builder.setContentTitle(trackerService.getString(R.string.notification_title_trackbook_running))
                builder.addAction(stopAction)
                builder.setLargeIcon(AppCompatResources.getDrawable(trackerService, R.drawable.ic_notification_icon_large_tracking_active_48dp)!!.toBitmap())
            }
            else -> {
                builder.setContentTitle(trackerService.getString(R.string.notification_title_trackbook_not_running))
                builder.addAction(resumeAction)
                builder.addAction(showAction)
                builder.setLargeIcon(AppCompatResources.getDrawable(trackerService, R.drawable.ic_notification_icon_large_tracking_stopped_48dp)!!.toBitmap())
            }
        }

        return builder.build()

    }


    /* Build context text for notification builder */
    private fun getContentString(context: Context, duration: Long, trackLength: Float, useImperial: Boolean): String {
        return "${LengthUnitHelper.convertDistanceToString(trackLength, useImperial)} • ${DateTimeHelper.convertToReadableTime(context, duration)}"
    }


    /* Checks if notification channel should be created */
    private fun shouldCreateNotificationChannel() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()


    /* Checks if notification channel exists */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() = notificationManager.getNotificationChannel(Keys.NOTIFICATION_CHANNEL_RECORDING) != null


    /* Create a notification channel */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(Keys.NOTIFICATION_CHANNEL_RECORDING,
            trackerService.getString(R.string.notification_channel_recording_name),
            NotificationManager.IMPORTANCE_LOW)
            .apply { description = trackerService.getString(R.string.notification_channel_recording_description) }
        notificationManager.createNotificationChannel(notificationChannel)
    }


    /* Notification pending intents */
    private val stopActionPendingIntent = PendingIntent.getService(
        trackerService,14,
        Intent(trackerService, TrackerService::class.java).setAction(Keys.ACTION_STOP),PendingIntent.FLAG_IMMUTABLE)
    private val resumeActionPendingIntent = PendingIntent.getService(
        trackerService, 16,
        Intent(trackerService, TrackerService::class.java).setAction(Keys.ACTION_RESUME),PendingIntent.FLAG_IMMUTABLE)
    private val showActionPendingIntent: PendingIntent? = TaskStackBuilder.create(trackerService).run {
        addNextIntentWithParentStack(Intent(trackerService, MainActivity::class.java))
        getPendingIntent(10, PendingIntent.FLAG_IMMUTABLE)
    }


    /* Notification actions */
    private val stopAction = NotificationCompat.Action(
        R.drawable.ic_notification_action_stop_24dp,
        trackerService.getString(R.string.notification_stop),
        stopActionPendingIntent)
    private val resumeAction = NotificationCompat.Action(
        R.drawable.ic_notification_action_resume_36dp,
        trackerService.getString(R.string.notification_resume),
        resumeActionPendingIntent)
    private val showAction = NotificationCompat.Action(
        R.drawable.ic_notification_action_show_36dp,
        trackerService.getString(R.string.notification_show),
        showActionPendingIntent)

}