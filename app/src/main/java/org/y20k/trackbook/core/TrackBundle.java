/**
 * TrackBundle.java
 * Implements a TrackBundle
 * TrackBundle is a container for file and corresponding name of a track
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-17 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */


package org.y20k.trackbook.core;

import org.y20k.trackbook.helpers.LogHelper;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * TrackBundle class
 */
public class TrackBundle {

    /* Define log tag */
    private static final String LOG_TAG = TrackBundle.class.getSimpleName();

    /* Main class variables */
    private File mTrackFile;
    private String mTrackName;


    /* Constructor */
    public TrackBundle(File file) {
        mTrackFile = file;
        mTrackName = buildTrackName(file);
    }


    /* Getter for track file */
    public File getTrackFile() {
        return mTrackFile;
    }


    /* Getter for track name */
    public String getTrackName() {
        return mTrackName;
    }


    /* Builds a readable track name from the track's file name */
    private String buildTrackName(File file) {

        // get file name without extension
        String readableTrackName = file.getName();
        readableTrackName = readableTrackName.substring(0, readableTrackName.indexOf(".trackbook"));

        try {
            // convert file name to date
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
            Date trackDate = dateFormat.parse(readableTrackName);

            // convert date to track name string according to current locale
            readableTrackName = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(trackDate) + " - " +
                    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(trackDate);

        } catch (ParseException e) {
            LogHelper.w(LOG_TAG, "Unable to parse file name into date object (yyyy-MM-dd-HH-mm-ss): " + e);
        }

        return readableTrackName;
    }

}