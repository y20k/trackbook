/**
 * LocationHelper.java
 * Implements the LocationHelper class
 * A LocationHelper offers helper methods for dealing with location issues
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

import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import java.util.List;


/**
 * LocationHelper class
 */
public final class LocationHelper {

    /* Define log tag */
    private static final String LOG_TAG = LocationHelper.class.getSimpleName();


    /* Main class variables */
//    private static final int TWO_MINUTES = 1000 * 1000 * 60 * 2;
    private static final long TWO_MINUTES = 2L * 60000000000L; // 2 minutes
    private static final long TWENTY_SECONDS = 20000000000L; // 20 seconds


    /* Determines last known location  */
    public static Location determineLastKnownLocation(LocationManager locationManager) {
        // define variables
        List locationProviders = locationManager.getProviders(true);
        Location gpsLocation = null;
        Location networkLocation = null;

        // set location providers
        String gpsProvider = LocationManager.GPS_PROVIDER;
        String networkProvider = LocationManager.NETWORK_PROVIDER;


        if (locationProviders.contains(gpsProvider)) {
            // get last know location from gps
            try {
                gpsLocation = locationManager.getLastKnownLocation(gpsProvider);
            } catch (SecurityException e) {
                // catches permission problems
                e.printStackTrace();
            }
        }

        if (locationProviders.contains(networkProvider)) {
            // get last known location from wifi and cell
            try {
                networkLocation = locationManager.getLastKnownLocation(networkProvider);
            } catch (SecurityException e) {
                // catches permission problems
                e.printStackTrace();
            }
        }

        // return best estimate location
        if (isBetterLocation(networkLocation, gpsLocation)) {
            LogHelper.v(LOG_TAG, "Best last known location came from: " + networkLocation.getProvider()); // TODO remove
            return networkLocation;
        } else {
            LogHelper.v(LOG_TAG, "Best last known location came from: " + gpsLocation.getProvider()); // TODO remove
            return gpsLocation;
        }
    }


    /* Determines whether one location reading is better than the current location fix */
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        // credit: the isBetterLocation method was sample code from: https://developer.android.com/guide/topics/location/strategies.html

        if (currentBestLocation == null) {
            // a new location is always better than no location
            return true;
        }

        // check whether the new location fix is newer or older
        long timeDelta = location.getElapsedRealtimeNanos() - currentBestLocation.getElapsedRealtimeNanos();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // if it's been more than two minutes since the current location, use the new location because the user has likely moved
        if (isSignificantlyNewer) {
            LogHelper.v(LOG_TAG, "Location isSignificantlyNewer: " + location.getProvider()); // TODO remove
            return true;
        } else if (isSignificantlyOlder) {
            LogHelper.v(LOG_TAG, "Location isSignificantlyOlder: " + location.getProvider()); // TODO remove
            return false;
        }

        // check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            LogHelper.v(LOG_TAG, "Location isMoreAccurate: " + location.getProvider()); // TODO remove
            return true;
        } else if (isNewer && !isLessAccurate) {
            LogHelper.v(LOG_TAG, "Location isNewer && !isLessAccurate: " + location.getProvider()); // TODO remove
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            LogHelper.v(LOG_TAG, "Location isNewer && !isSignificantlyLessAccurate && isFromSameProvider: " + location.getProvider()); // TODO remove
            return true;
        }
        LogHelper.v(LOG_TAG, "Location is not better: " + location.getProvider()); // TODO remove
        return false;
    }


    /* Checks if given location is newer than two minutes */
    public static boolean isNewLocation(Location location) {
        if (location == null) {
            return false;
        } else {
            long locationTime = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
            return locationTime < TWO_MINUTES;
        }
    }


    /* Checks if given location is a new waypoint over */
    public static boolean isNewWayPoint(Location lastWayPoint, Location newLocation) {
        float distance = newLocation.distanceTo(lastWayPoint);
        long timeDifference = newLocation.getElapsedRealtimeNanos() - lastWayPoint.getElapsedRealtimeNanos();

        // distance is bigger than 10 meters and time difference bigger than 20 seconds
        return distance > 10 && timeDifference > TWENTY_SECONDS;
    }


    /* Checks if given location is a stop over */
    public static boolean isStopOver(Location location) {
        // TODO determine, if location is stopover
        return false;
    }


    /* Checks whether two location providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        // credit: the isSameProvider method was sample code from: https://developer.android.com/guide/topics/location/strategies.html
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

}
