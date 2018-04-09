/**
 * NotificationHelper.java
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */

package org.y20k.trackbook.helpers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import org.y20k.trackbook.MainActivity;
import org.y20k.trackbook.R;
import org.y20k.trackbook.TrackerService;
import org.y20k.trackbook.core.Track;


/**
 * NotificationHelper class
 */
public final class NotificationHelper implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = NotificationHelper.class.getSimpleName();


    /* Creates a notification builder */
    public static Notification getNotification(Context context, NotificationCompat.Builder builder, Track track, boolean tracking) {

        // create notification channel
        createNotificationChannel(context);

        // ACTION: NOTIFICATION TAP & BUTTON SHOW
        Intent tapActionIntent = new Intent(context, MainActivity.class);
        tapActionIntent.setAction(ACTION_SHOW_MAP);
        tapActionIntent.putExtra(EXTRA_TRACK, track);
        tapActionIntent.putExtra(EXTRA_TRACKING_STATE, tracking);
        // artificial back stack for started Activity (https://developer.android.com/training/notify-user/navigation.html#DirectEntry)
        TaskStackBuilder tapActionIntentBuilder = TaskStackBuilder.create(context);
        tapActionIntentBuilder.addParentStack(MainActivity.class);
        tapActionIntentBuilder.addNextIntent(tapActionIntent);
        // pending intent wrapper for notification tap
        PendingIntent tapActionPendingIntent = tapActionIntentBuilder.getPendingIntent(10, PendingIntent.FLAG_UPDATE_CURRENT);

        // ACTION: NOTIFICATION BUTTON STOP
        Intent stopActionIntent = new Intent(context, TrackerService.class);
        stopActionIntent.setAction(ACTION_STOP);
        // pending intent wrapper for notification stop action
        PendingIntent stopActionPendingIntent = PendingIntent.getService(context, 14, stopActionIntent, 0);

        // ACTION: NOTIFICATION BUTTON RESUME
        Intent resumeActionIntent = new Intent(context, TrackerService.class);
        resumeActionIntent.setAction(ACTION_RESUME);
        // pending intent wrapper for notification resume action
        PendingIntent resuneActionPendingIntent = PendingIntent.getService(context, 16, resumeActionIntent, 0);

        // construct notification in builder
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setShowWhen(false);
        builder.setContentIntent(tapActionPendingIntent);
        builder.setSmallIcon(R.drawable.ic_notification_small_24dp);
        builder.setLargeIcon(getNotificationIconLarge(context, tracking));
        if (tracking) {
            builder.addAction(R.drawable.ic_stop_white_24dp, context.getString(R.string.notification_stop), stopActionPendingIntent);
            builder.setContentTitle(context.getString(R.string.notification_title_trackbook_running));
            builder.setContentText(getContextString(context, track));
        } else {
            builder.addAction(R.drawable.ic_fiber_manual_record_white_24dp, context.getString(R.string.notification_resume), resuneActionPendingIntent);
            builder.addAction(R.drawable.ic_compass_needle_white_24dp, context.getString(R.string.notification_show), tapActionPendingIntent);
            builder.setContentTitle(context.getString(R.string.notification_title_trackbook_not_running));
            builder.setContentText(getContextString(context, track));
        }

        return builder.build();
    }


    /* Constructs an updated notification */
    public static Notification getUpdatedNotification(Context context, NotificationCompat.Builder builder, Track track) {
        builder.setContentText(getContextString(context, track));
        return builder.build();
    }


    /* Create a notification channel */
    public static boolean createNotificationChannel(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // API level 26 ("Android O") supports notification channels.
            String id = NOTIFICATION_CHANEL_ID_RECORDING_CHANNEL;
            CharSequence name = context.getString(R.string.notification_channel_recording_name);
            String description = context.getString(R.string.notification_channel_recording_description);
            int importance = NotificationManager.IMPORTANCE_LOW;

            // create channel
            NotificationChannel channel = new NotificationChannel(id, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            return true;

        } else {
            return false;
        }
    }


    /* Get station image for notification's large icon */
    private static Bitmap getNotificationIconLarge(Context context, boolean tracking) {

        // get dimensions
        Resources resources = context.getResources();
        int height = (int) resources.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) resources.getDimension(android.R.dimen.notification_large_icon_width);

        Bitmap bitmap;
        if (tracking) {
            bitmap = getBitmap(context, R.drawable.ic_notification_large_tracking_48dp);
        } else {
            bitmap = getBitmap(context, R.drawable.ic_notification_large_not_tracking_48dp);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }


    /* Return a bitmap for a given resource id of a vector drawable */
    private static Bitmap getBitmap(Context context, int resource) {
        VectorDrawableCompat drawable = VectorDrawableCompat.create(context.getResources(), resource, null);
        if (drawable != null) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } else {
            return null;
        }
    }


    /* Build context text for notification builder */
    private static String getContextString(Context context, Track track) {
        return context.getString(R.string.notification_content_distance) + ": " + track.getTrackDistanceString() + " | " +
                context.getString(R.string.notification_content_duration) + ": " + track.getTrackDurationString();
    }

}
