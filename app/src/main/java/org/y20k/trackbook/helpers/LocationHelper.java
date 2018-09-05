/**
 * LocationHelper.java
 * Implements the LocationHelper class
 * A LocationHelper offers helper methods for dealing with location issues
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

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.SystemClock;
import android.provider.Settings;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;


/**
 * LocationHelper class
 */
public final class LocationHelper implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = LocationHelper.class.getSimpleName();


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

        if (gpsLocation == null) {
            return networkLocation;
        } else if (networkLocation == null) {
            return gpsLocation;
        } else if (isBetterLocation(gpsLocation, networkLocation)) {
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
        long timeDelta = location.getElapsedRealtimeNanos() - currentBestLocation.getElapsedRealtimeNanos();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES_IN_NANOSECONDS;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES_IN_NANOSECONDS;
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
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

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


    /* Checks accuracy of given location */
    public static boolean isAccurate(Location location) {
        return location.getAccuracy() < FIFTY_METER_RADIUS;
    }


    /* Checks if given location is newer than two minutes */
    public static boolean isCurrent(Location location) {
        if (location == null) {
            return false;
        } else {
            long locationAge = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
            return locationAge < TWO_MINUTES_IN_NANOSECONDS;
        }
    }


    /* Checks if given location is a new WayPoint */
    public static boolean isNewWayPoint(Location lastLocation, Location newLocation, float averageSpeed) {
        float distance = newLocation.distanceTo(lastLocation);
        long timeDifference = newLocation.getElapsedRealtimeNanos() - lastLocation.getElapsedRealtimeNanos();

        if (newLocation.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
            // calculate speed difference
            float speedDifference;
            float currentSpeed = distance / ((float)timeDifference / ONE_SECOND_IN_NANOSECOND);
            if (currentSpeed > averageSpeed) {
                speedDifference = currentSpeed / averageSpeed;
            } else {
                speedDifference = averageSpeed / currentSpeed;
            }

            // SPECIAL CASE network: plausibility check for network provider. looking for sudden location jump errors
            if (averageSpeed != 0f && currentSpeed > 10f && speedDifference > 2f) {
                // implausible location (speed is high (10 m/s == 36km/h) and has doubled)
                return false;
            }

            // SPECIAL CASE network: if last location came from gps. only accept location fixes with decent accuracy
            if (lastLocation.getProvider().equals(LocationManager.GPS_PROVIDER) && newLocation.getAccuracy() < 66) {
                // network locations tend to be too in accurate
                return false;
            }

            // DEFAULT network: distance is bigger than 30 meters and time difference bigger than 12 seconds
            return distance > 30 && timeDifference >= 12 * ONE_SECOND_IN_NANOSECOND; // TODO add minimal accuracy

        } else {
            // DEFAULT GPS: distance is bigger than 10 meters and time difference bigger than 12 seconds
            return distance > 10 && timeDifference >= 12 * ONE_SECOND_IN_NANOSECOND;
        }

    }


    /* Checks if given location is a stop over */
    public static boolean isStopOver(@Nullable Location previousLocation, Location newLocation) {
        if (previousLocation != null) {
            long timeDifference =  newLocation.getElapsedRealtimeNanos() - previousLocation.getElapsedRealtimeNanos();
            return timeDifference >= FIVE_MINUTES_IN_NANOSECONDS;
        } else {
            return false;
        }
    }


    /* Registers gps and network location listeners */
    public static void registerLocationListeners(LocationManager locationManager, LocationListener gpsListener, LocationListener networkListener) {
        LogHelper.v(LOG_TAG, "Registering location listeners.");

        // get location providers
        List locationProviders = locationManager.getAllProviders();

        // got GPS location provider?
        if (gpsListener != null && locationProviders.contains(LocationManager.GPS_PROVIDER)) {
            try {
                // register GPS location listener and request updates
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);
                LogHelper.v(LOG_TAG, "Registering gps listener.");
            } catch (SecurityException e) {
                // catches permission problems
                e.printStackTrace();
            }
        }

        // got network location provider?
        if (networkListener != null && locationProviders.contains(LocationManager.NETWORK_PROVIDER)) {
            try {
                // register network location listener and request updates
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);
                LogHelper.v(LOG_TAG, "Registering network listener.");
            } catch (SecurityException e) {
                // catches permission problems
                e.printStackTrace();
            }
        }
    }


    /* Removes gps and network location listeners */
    public static void removeLocationListeners(LocationManager locationManager, LocationListener gpsListener, LocationListener networkListener) {
        LogHelper.v(LOG_TAG, "Removing location listeners.");

        // get location providers
        List locationProviders = locationManager.getAllProviders();

        // got GPS location provider?
        if (locationProviders.contains(LocationManager.GPS_PROVIDER) && gpsListener != null) {
            try {
                // remove GPS listener
                locationManager.removeUpdates(gpsListener);
                LogHelper.v(LOG_TAG, "Removing gps listener.");
            } catch (SecurityException e) {
                // catches permission problems
                e.printStackTrace();
            }
        }

        // got network location provider?
        if (locationProviders.contains(LocationManager.NETWORK_PROVIDER) && networkListener != null) {
            try {
                // remove network listener
                locationManager.removeUpdates(networkListener);
                LogHelper.v(LOG_TAG, "Removing network listener.");
            } catch (SecurityException e) {
                // catches permission problems
                e.printStackTrace();
            }
        }

    }


    /* Converts milliseconds to mm:ss or hh:mm:ss */
    public static String convertToReadableTime(long milliseconds, boolean includeHours) {

        if (includeHours) {
            // format hh:mm:ss
            return String.format(Locale.ENGLISH, "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(milliseconds),
                    TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1));
        } else if (TimeUnit.MILLISECONDS.toHours(milliseconds) < 1) {
            // format mm:ss
            return String.format(Locale.ENGLISH, "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(milliseconds) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(milliseconds) % TimeUnit.MINUTES.toSeconds(1));
        } else {
            return null;
        }

    }


    /* Check if any location provider is enabled */
    public static boolean checkLocationSystemSetting(Context context) {
        int locationSettingState = 0;
        try {
            locationSettingState = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return locationSettingState != Settings.Secure.LOCATION_MODE_OFF;
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
