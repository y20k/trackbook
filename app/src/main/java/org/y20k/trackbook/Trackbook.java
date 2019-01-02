/**
 * Trackbook.java
 * Implements the Trackbook class
 * Trackbook starts up the app and sets up the basic theme (Day / Night)
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-19 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */

package org.y20k.trackbook;

import android.app.Application;
import android.os.Build;

import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.NightModeHelper;

import androidx.appcompat.app.AppCompatDelegate;


/**
 * Trackbook.class
 */
public class Trackbook extends Application {

    /* Define log tag */
    private static final String LOG_TAG = Trackbook.class.getSimpleName();


    @Override
    public void onCreate() {
        super.onCreate();

        // set Day / Night theme state
        if (Build.VERSION.SDK_INT >= 28) {
            // Android P might introduce a system wide theme option - in that case: follow system (28 = Build.VERSION_CODES.P)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            // try t0 get last state the user chose
            NightModeHelper.restoreSavedState(this);
        }

    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        LogHelper.v(LOG_TAG, "Trackbook application terminated.");
    }

}
