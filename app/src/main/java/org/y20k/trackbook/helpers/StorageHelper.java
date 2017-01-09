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

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;


/**
 * StorageHelper class
 */
public class StorageHelper implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = StorageHelper.class.getSimpleName();

    /* Main class variables */
    private final int mFileType;
    private final String mDirectoryName = "tracks";
    private final String mFileExtension = ".trackbook";
    private final Context mActivity;
    private File mFolder;
    private File mTempFile;


    /* Constructor */
    public StorageHelper(Context activity, int fileType) {
        // store activity
        mActivity = activity;
        mFileType = fileType;

        // get "tracks" folder
        mFolder  = mActivity.getExternalFilesDir(mDirectoryName);
        // mFolder = getTracksDirectory();

        // create folder if necessary
        if (mFolder != null && !mFolder.exists()) {
            LogHelper.v(LOG_TAG, "Creating new folder: " + mFolder.toString());
            mFolder.mkdir();
        }

        // create temp file object
        mTempFile = new File(mFolder.toString() + "/" +  FILENAME_TEMP + mFileExtension);
    }


    /* Checks if a temp file exits */
    public boolean tempFileExists() {
        return mTempFile.exists();
    }


    /* Deletes temp file - if it exits */
    public boolean deleteTempFile() {
        return mTempFile.exists() && mTempFile.delete();
    }


    /* Saves track object to file */
    public boolean saveTrack(@Nullable Track track) {

        // get "tracks" folder
        mFolder  = mActivity.getExternalFilesDir(mDirectoryName);

        Date recordingStart = null;
        if (track != null) {
            recordingStart = track.getRecordingStart();
        }

        if (mFolder != null && mFolder.exists() && mFolder.isDirectory() && mFolder.canWrite() && recordingStart != null && track != null) {
            // construct filename from track recording date
            String fileName;
            if (mFileType == FILETYPE_TEMP) {
                fileName = FILENAME_TEMP + mFileExtension;
            } else {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
                fileName = dateFormat.format(recordingStart) + mFileExtension;
            }
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
                return false;
            }

            // if write was successful delete old track files - only if not a temp file
            if (mFileType != FILETYPE_TEMP) {
                deleteOldTracks();
            }

            return true;

        } else {
            LogHelper.e(LOG_TAG, "Unable to save track to external storage.");
            return false;
        }

    }


    /* Loads given file into memory */
    public Track loadTrack () {

        // get file reference
        File file;
        if (mFileType == FILETYPE_TEMP) {
            file = getTempFile();
        } else {
            file = getMostCurrentTrack();
        }

        // check if given file was null
        if (file == null) {
            LogHelper.e(LOG_TAG, "Did not receive file object.");
            return null;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            LogHelper.v(LOG_TAG, "Loading track from external storage: " + file.toString());

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


    /* Gets most current track from directory */
    private File getMostCurrentTrack() {

        // get "tracks" folder
        mFolder  = mActivity.getExternalFilesDir(mDirectoryName);

        if (mFolder != null && mFolder.isDirectory()) {
            // get files and sort them
            File[] files = mFolder.listFiles();
            files = sortFiles(files);
            if (files.length > 0 && files[0].getName().endsWith(mFileExtension) && !files[0].equals(mTempFile)){
                // return latest track
                return files[0];
            }
        }
        LogHelper.e(LOG_TAG, "Unable to get files from given folder. Folder is probably empty.");
        return null;
    }


    /* Gets temp file - if it exists */
    private File getTempFile() {
        if (mTempFile.exists()) {
            return mTempFile;
        } else {
            return null;
        }
    }


    /* Gets the last track from directory */
    private void deleteOldTracks() {

        // get "tracks" folder
        mFolder  = mActivity.getExternalFilesDir(mDirectoryName);

        if (mFolder != null && mFolder.isDirectory()) {
            LogHelper.v(LOG_TAG, "Deleting old Track files.");

            // get files and sort them
            File[] files = mFolder.listFiles();
            files = sortFiles(files);

            // store length of array
            int numberOfFiles = files.length;

            // keep the latest ten (mMaxTrackFiles) track files
            int index = MAXIMUM_TRACK_FILES;
            // iterate through array
            while (index < numberOfFiles && files[index].getName().endsWith(mFileExtension) && !files[index].equals(mTempFile)) {
                files[index].delete();
                index++;
            }
        }
    }


    /* Sorts array of files in a way that the newest files are at the top and non-.trackbook files are at the bottom */
    private File[] sortFiles(File[] files) {
        // sort array
        LogHelper.v(LOG_TAG, "Sorting files.");
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {

                // discard files not ending with ".trackbook"
                boolean file1IsTrack = file1.getName().endsWith(mFileExtension);
                boolean file2IsTrack = file2.getName().endsWith(mFileExtension);

                // note: "greater" means higher index in array
                if (!file1IsTrack && file2IsTrack) {
                    // file1 is not a track, file1 is greater
                    return 1;
                } else if (!file2IsTrack && file1IsTrack) {
                    // file2 is not a track, file2 is greater
                    return -1;
                } else {
                    // "compareTo" compares abstract path names lexicographically | 0 == equal | -1 == file2 less than file1 | 1 == file2 greater than file1
                    return file2.compareTo(file1);
                }

            }
        });

        // log sorting result // TODO comment out for release
        String fileList = "";
        for (File file : files) {
            fileList = fileList + file.getName() + "\n";
        }
        LogHelper.v(LOG_TAG, "+++ List of files +++\n" + fileList);

        // hand back sorted array of files
        return files;
    }


    /* Return a write-able sub-directory from external storage  */
    private File getTracksDirectory() {
        File[] storage = mActivity.getExternalFilesDirs(mDirectoryName);
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
