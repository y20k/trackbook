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

import java.util.List;


/**
 * LocationHelper class
 */
public final class LocationHelper {

    /* Main class variables */
    private static final int TWO_MINUTES = 1000 * 60 * 2;


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
        if (isBetterLocation(gpsLocation, networkLocation)) {
            return gpsLocation;
        } else {
            return networkLocation;
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
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // if it's been more than two minutes since the current location, use the new location because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        // check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
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
