/**
 * NotificationHelper.java
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */

package org.y20k.trackbook.helpers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import org.y20k.trackbook.MainActivity;
import org.y20k.trackbook.R;
import org.y20k.trackbook.TrackerService;
import org.y20k.trackbook.core.Track;


/**
 * NotificationHelper class
 */
public class NotificationHelper implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = NotificationHelper.class.getSimpleName();


    /* Main class variables */
    private static Notification mNotification;
    private static Service mService;

    /* Create and put up notification */
    public static void show(final Service service, Track track) {
        // save service
        mService = service;

        // build notification
        mNotification = getNotificationBuilder(track, true).build();

        // display notification
        mService.startForeground(TRACKER_SERVICE_NOTIFICATION_ID, mNotification);
    }


    /* Updates the notification */
    public static void update(Track track, boolean tracking) {

        // build notification
        mNotification = getNotificationBuilder(track, tracking).build();

        // display updated notification
        NotificationManager notificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(TRACKER_SERVICE_NOTIFICATION_ID, mNotification);

        if (!tracking) {
            // make notification swipe-able
            mService.stopForeground(false);
        }


    }


    /* Stop displaying notification */
    public static void stop() {
        if (mService != null) {
            mService.stopForeground(true);
        }
    }


    /* Creates a notification builder */
    private static NotificationCompat.Builder getNotificationBuilder(Track track, boolean tracking) {

        String contentText = mService.getString(R.string.notification_content_distance) + ": " + track.getTrackDistance() + " | " +
                mService.getString(R.string.notification_content_duration) + " : " +  track.getTrackDuration();

        // explicit intent for notification tap
        Intent tapActionIntent = new Intent(mService, MainActivity.class);
        tapActionIntent.setAction(Intent.ACTION_MAIN);
        tapActionIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        tapActionIntent.putExtra(EXTRA_TRACK, track);
        tapActionIntent.putExtra(EXTRA_TRACKING_STATE, true);

        // explicit intent for stopping playback
        Intent stopActionIntent = new Intent(mService, TrackerService.class);
        stopActionIntent.setAction(ACTION_STOP);

        // artificial back stack for started Activity.
        // -> navigating backward from the Activity leads to Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mService);
//        // backstack: adds back stack for Intent (but not the Intent itself)
//        stackBuilder.addParentStack(MainActivity.class);
        // backstack: add explicit intent for notification tap
        stackBuilder.addNextIntent(tapActionIntent);

        // pending intent wrapper for notification tap
        PendingIntent tapActionPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // pending intent wrapper for notification stop action
        PendingIntent stopActionPendingIntent = PendingIntent.getService(mService, 0, stopActionIntent, 0);

        // construct notification in builder
        NotificationCompat.Builder builder;
        builder = new NotificationCompat.Builder(mService);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setShowWhen(false);
        builder.setContentIntent(tapActionPendingIntent);
        builder.setSmallIcon(R.drawable.ic_notification_small_24dp);
        builder.setLargeIcon(getNotificationIconLarge(tracking));
        if (tracking) {
            builder.addAction(R.drawable.ic_stop_white_36dp, mService.getString(R.string.notification_stop), stopActionPendingIntent);
            builder.setContentTitle(mService.getString(R.string.notification_title_trackbook_running));
            builder.setContentText(contentText);
            // third line of text - only appears in expanded view
            // builder.setSubText();
        } else {
            builder.setContentTitle(mService.getString(R.string.notification_title_trackbook_not_running));
            builder.setContentText(contentText);
            // third line of text - only appears in expanded view
            // builder.setSubText();
        }

        return builder;
    }


    /* Get station image for notification's large icon */
    private static Bitmap getNotificationIconLarge(boolean tracking) {

        // get dimensions
        Resources resources = mService.getResources();
        int height = (int) resources.getDimension(android.R.dimen.notification_large_icon_height);
        int width = (int) resources.getDimension(android.R.dimen.notification_large_icon_width);

        Bitmap bitmap;
         if (tracking) {
             bitmap = getBitmap(R.drawable.ic_notification_large_tracking_48dp);
         } else {
             bitmap = getBitmap(R.drawable.ic_notification_large_not_tracking_48dp);
         }

        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }


    /* Return a bitmap for a given resource id of a vector drawable */
    private static Bitmap getBitmap(int resource) {
        VectorDrawableCompat drawable = VectorDrawableCompat.create(mService.getResources(), resource, null);
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



}
