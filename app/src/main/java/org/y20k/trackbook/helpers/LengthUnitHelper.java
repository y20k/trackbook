/**
 * LengthUnitHelper.java
 * Implements the LengthUnitHelper class
 * A LengthUnitHelper offers helper methods for dealing with unit systems and locales
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

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


/**
 * LengthUnitHelper class
 */
public final class LengthUnitHelper implements TrackbookKeys {


    /* Converts for the default locale a distance value to a readable string */
    public static String convertDistanceToString(double distance) {
        return convertDistanceToString(distance, getUnitSystem());
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
        // format distance according to current locale
        NumberFormat numberFormat =  NumberFormat.getNumberInstance();
        numberFormat.setMaximumFractionDigits(0);
        return numberFormat.format(distance) + " " + unit;
    }


    /* Determines which unit system the device is using (metric or imperial) */
    public static int getUnitSystem() {
        // America (US), Liberia (LR), Myanmar(MM) use the imperial system
        List<String> imperialSystemCountries = Arrays.asList("US", "LR", "MM");
        String countryCode = Locale.getDefault().getCountry();
        if (imperialSystemCountries.contains(countryCode)){
            return IMPERIAL;
        } else {
            return METRIC;
        }
    }

}
