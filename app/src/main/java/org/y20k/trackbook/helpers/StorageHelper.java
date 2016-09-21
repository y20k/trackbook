/**
 * StorageHelper.java
 * Implements the StorageHelper class
 * A StorageHelper deals with saving and loading recorded tracks
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

import android.app.Activity;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.widget.Toast;

import com.google.gson.Gson;

import org.y20k.trackbook.R;
import org.y20k.trackbook.core.Track;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * StorageHelper class
 */
public class StorageHelper implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = StorageHelper.class.getSimpleName();

    /* Main class variables */
    private final Activity mActivity;
    private File mFolder;


    /* Constructor */
    public StorageHelper(Activity activity) {
        mActivity = activity;
        mFolder  = mActivity.getExternalFilesDir("Tracks");
        // mFolder = getTracksDirectory();

        // create folder if necessary
        if (mFolder != null && !mFolder.exists()) {
            LogHelper.v(LOG_TAG, "Creating new folder: " + mFolder.toString());
            mFolder.mkdir();
        }
    }


    /* Saves track object to file */
    public boolean saveTrack(Track track) {

        Date recordingStart = track.getRecordingStart();

        if (mFolder.exists() && mFolder.isDirectory() && mFolder.canWrite() && recordingStart != null) {
            // construct filename from track recording date
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
            String fileName = dateFormat.format(recordingStart) + ".trackbook";
            File file = new File(mFolder.toString() + "/" +  fileName);

            // convert to JSON
            Gson gson = new Gson();
            String json = gson.toJson(track);

            // write track
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                LogHelper.v(LOG_TAG, "Saving track to external storage: " + file.toString());
                bw.write(json);
            } catch (IOException e) {
                LogHelper.e(LOG_TAG, "Unable to saving track to external storage (IOException): " + file.toString());
            }

            return true;

        } else {
            LogHelper.e(LOG_TAG, "Unable to save track to external storage.");
            return false;
        }

    }


    /* Loads given file into memory */
    public Track loadTrack (File file) {

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            LogHelper.v(LOG_TAG, "Loading track to external storage: " + file.toString());

            String line;
            StringBuilder sb = new StringBuilder("");

            // read until last line reached
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            // get track from JSON
            Gson gson = new Gson();
            return gson.fromJson(sb.toString(), Track.class);

        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to read file from external storage: " + file.toString());
            return null;
        }

    }


    /* Gets the last track from directory */
    public File getLastTrack() {
        if (mFolder != null && mFolder.isDirectory()) {
            File[] files = mFolder.listFiles();
            // TODO
            return files[0];
        }
        // TODO
        return null;
    }


    /* Return a write-able sub-directory from external storage  */
    private File getTracksDirectory() {
        String subDirectory = "Tracks";
        File[] storage = mActivity.getExternalFilesDirs(subDirectory);
        for (File file : storage) {
            if (file != null) {
                String state = EnvironmentCompat.getStorageState(file);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    LogHelper.i(LOG_TAG, "External storage: " + file.toString());
                    return file;
                }
            }
        }
        Toast.makeText(mActivity, R.string.toast_message_no_external_storage, Toast.LENGTH_LONG).show();
        LogHelper.e(LOG_TAG, "Unable to access external storage.");

        return null;
    }

}
