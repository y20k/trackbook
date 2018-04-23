/**
 * LocaleUnitHelper.java
 * Implements the LocaleUnitHelper class
 * A LocaleUnitHelper offers helper methods for dealing with unit systems and locales
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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;


/**
 * LocaleUnitHelper class
 */
public final class LocaleUnitHelper implements TrackbookKeys {


    /* Converts for the default locale a distance value to a readable string */
    public static String convertDistanceToString(double distance) {
        return convertDistanceToString(distance, getUnitSystem(Locale.getDefault()));
    }


    /* Converts for the given uni System a distance value to a readable string */
    public static String convertDistanceToString(double distance, int unitSystem) {
        // check for locale and set unit system accordingly
        String unit;
        if (unitSystem == IMPERIAL) {
            // convert distance to feet
            distance = distance * 3.28084f;
            // set measurement unit
            unit = "ft";
        } else {
            // set measurement unit
            unit = "m";
        }
        return String.format (Locale.ENGLISH, "%.0f", distance) + unit;
    }


    /* Determines which unit system the device is using (metric or imperial) */
    private static int getUnitSystem(Locale locale) {
        // America (US), Liberia (LR), Myanmar(MM) use the imperial system
        List<String> imperialSystemCountries = Arrays.asList("US", "LR", "MM");
        String countryCode = locale.getCountry();

        if (imperialSystemCountries.contains(countryCode)){
            return IMPERIAL;
        } else {
            return METRIC;
        }
    }


    /* Returns the opposite uni system based on the current locale */
    public static int getOppositeUnitSystem() {
        int unitSystem = getUnitSystem(Locale.getDefault());
        if (unitSystem == METRIC){
            return IMPERIAL;
        } else {
            return METRIC;
        }
    }

}
