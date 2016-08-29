/**
 * TrackerService.java
 * Implements the app's movement tracker service
 * The TrackerService creates a Track object and displays a notification
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

package org.y20k.trackbook;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.y20k.trackbook.core.Track;
import org.y20k.trackbook.helpers.TrackbookKeys;


/**
 * TrackerService class
 */
public class TrackerService extends Service implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = TrackerService.class.getSimpleName();


    /* Main class variables */
    private Track mTrack;
    private CountDownTimer mTimer;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);

        // checking for empty intent
        if (intent == null) {
            Log.v(LOG_TAG, "Null-Intent received. Stopping self.");
            // remove notification
            stopForeground(true);
            stopSelf();
        }

        // ACTION START
        else if (intent.getAction().equals(ACTION_START)) {
            Log.v(LOG_TAG, "Service received command: START");

            // create a new track
            mTrack = new Track();

            // acquire reference to Location Manager
            mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            // listener that responds to location updates
            mLocationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // add new location to track
                    mTrack.addWayPoint(location, false);
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                    // TODO do something
                }

                public void onProviderEnabled(String provider) {
                    // TODO do something
                }

                public void onProviderDisabled(String provider) {
                    // TODO do something
                }
            };

            try {
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }

            mTimer = new CountDownTimer(CONSTANT_MAXIMAL_DURATION, CONSTANT_TRACKING_INTERVAL) {
                @Override
                public void onTick(long l) {
                    // TODO
                }

                @Override
                public void onFinish() {
                    // TODO
                }
            };

        }

        // ACTION STOP
        else if (intent.getAction().equals(ACTION_STOP)) {
            // Remove the listener you previously added
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            Log.v(LOG_TAG, "Service received command: STOP");
        }

        // START_STICKY is used for services that are explicitly started and stopped as needed
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v(LOG_TAG, "onDestroy called.");

        // Remove the listener you previously added
        try {
            mLocationManager.removeUpdates(mLocationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        // cancel notification
        stopForeground(true);
    }

}
