/**
 * StorageHelper.java
 * Implements the StorageHelper class
 * A StorageHelper deals with saving and loading recorded tracks
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

package org.y20k.trackbook.helpers;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.os.EnvironmentCompat;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    private final Context mContext;
    private final File mFolder;
    private final File mTempFile;


    /* Constructor */
    public StorageHelper(Context context) {
        // store activity
        mContext = context;

        // get "tracks" folder
        mFolder  = mContext.getExternalFilesDir(TRACKS_DIRECTORY_NAME);
        // mFolder = getTracksDirectory();

        // create "tracks" folder if necessary
        if (mFolder != null && !mFolder.exists()) {
            LogHelper.v(LOG_TAG, "Creating new folder: " + mFolder.toString());
            mFolder.mkdirs();
        }

        // create temp file object // todo check -> may produce NullPointerException
        mTempFile = new File(mFolder.toString() + "/" + FILE_NAME_TEMP + FILE_TYPE_TRACKBOOK_EXTENSION);

        // delete old track - exclude temp file
        deleteOldTracks(false);
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
    public boolean saveTrack(@Nullable Track track, int fileType) {

        Date recordingStart = null;
        if (track != null) {
            recordingStart = track.getRecordingStart();
        }

        if (mFolder != null && mFolder.exists() && mFolder.isDirectory() && mFolder.canWrite() && recordingStart != null && track != null) {
            // create file object
            String fileName;
            if (fileType == FILE_TEMP_TRACK) {
                // case: temp file
                fileName = FILE_NAME_TEMP + FILE_TYPE_TRACKBOOK_EXTENSION;
            } else {
                // case: regular file
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
                fileName = dateFormat.format(recordingStart) + FILE_TYPE_TRACKBOOK_EXTENSION;
            }
            File file = new File(mFolder.toString() + "/" +  fileName);

            // convert track to JSON
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setDateFormat("M/d/yy hh:mm a");
            Gson gson = gsonBuilder.create();
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
            if (fileType != FILE_TEMP_TRACK) {
                // include temp file if it exists
                deleteOldTracks(true);
            }

            return true;

        } else {
            LogHelper.e(LOG_TAG, "Unable to save track to external storage.");
            return false;
        }

    }


    /* Loads given file into memory */
    public Track loadTrack(int fileType) {

        // get file reference
        File trackFile;
        switch (fileType) {
            case FILE_TEMP_TRACK:
                trackFile = getTempFile();
                break;
            case FILE_MOST_CURRENT_TRACK:
                trackFile = getMostCurrentTrack();
                break;
            default:
                trackFile = null;
                break;
        }

        // read & parse file and return track
        return readTrackFromFile(trackFile);
    }


    /* Loads given file into memory */
    public Track loadTrack(File file) {

        // get file reference
        File trackFile;
        if (file != null) {
            trackFile = file;
        } else {
            // fallback
            trackFile = getMostCurrentTrack();
        }

        // read & parse file and return track
        return readTrackFromFile(trackFile);
    }


    /* Gets a list of .trackbook files - excluding the temp file */
    public File[] getListOfTrackbookFiles() {
        // TODO HANDLE CASE: EMPTY FILE LIST

        // get files and sort them
        return sortFiles(mFolder.listFiles());
    }


//    /* Gets a list of tracks based on their file names */
//    public List<String> getListOfTracks() {
//        List<String> listOfTracks = new ArrayList<String>();
//
//        // get files and sort them
//        File[] files = mFolder.listFiles();
//        files = sortFiles(files);
//
//        for (File file : files) {
//            listOfTracks.add(file.getName());
//        }
//
//        // TODO HANDLE CASE: EMPTY FILE LIST
//        return listOfTracks;
//    }


    // loads file and parses it into a track
    private Track readTrackFromFile(File file) {

        // check if given file was null
        if (file == null) {
            LogHelper.e(LOG_TAG, "Did not receive a file object.");
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

            // TODO implement a format version check before handing the file to GSON

            // get track from JSON
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.setDateFormat("M/d/yy hh:mm a");
            Gson gson = gsonBuilder.create();
            return gson.fromJson(sb.toString(), Track.class);

        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to read file from external storage: " + file.toString());
            return null;
        }
    }


    /* Gets most current track from directory */
    private File getMostCurrentTrack() {

        if (mFolder != null && mFolder.isDirectory()) {
            // get files and sort them
            File[] files = mFolder.listFiles();
            files = sortFiles(files);
            if (files.length > 0 && files[0].getName().endsWith(FILE_TYPE_TRACKBOOK_EXTENSION) && !files[0].equals(mTempFile)){
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
    private void deleteOldTracks(boolean includeTempFile) {

        if (mFolder != null && mFolder.isDirectory()) {
            LogHelper.v(LOG_TAG, "Deleting older recordings.");

            // get files and sort them
            File[] files = mFolder.listFiles();
            files = sortFiles(files);

            // store length of array
            int numberOfFiles = files.length;

            // keep the latest ten (mMaxTrackFiles) track files
            int index = MAXIMUM_TRACK_FILES;
            // iterate through array
            while (index < numberOfFiles && files[index].getName().endsWith(FILE_TYPE_TRACKBOOK_EXTENSION) && !files[index].equals(mTempFile)) {
                files[index].delete();
                index++;
            }
        }

        // delete temp file if it exists
        if (includeTempFile && mTempFile.exists()) {
            mTempFile.delete();
        }

    }


    /* Sorts array of files in a way that the newest files are at the top and non-.trackbook files are at the bottom */
    private File[] sortFiles(File[] files) {
        // sort array
        LogHelper.v(LOG_TAG, "Sorting files.");
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {

                // discard temp file and files not ending with ".trackbook"
                boolean file1IsTrack = file1.getName().endsWith(FILE_TYPE_TRACKBOOK_EXTENSION) && !file1.equals(mTempFile);
                boolean file2IsTrack = file2.getName().endsWith(FILE_TYPE_TRACKBOOK_EXTENSION) && !file2.equals(mTempFile);

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
//        String fileList = "";
//        for (File file : files) {
//            fileList = fileList + file.getName() + "\n";
//        }
//        LogHelper.v(LOG_TAG, "+++ List of files +++\n" + fileList);

        // hand back sorted array of files
        return files;
    }


    /* Return a write-able sub-directory from external storage */
    private File getTracksDirectory() {
        File[] storage = mContext.getExternalFilesDirs(TRACKS_DIRECTORY_NAME);
        for (File file : storage) {
            if (file != null) {
                String state = EnvironmentCompat.getStorageState(file);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    LogHelper.i(LOG_TAG, "External storage: " + file.toString());
                    return file;
                }
            }
        }
        Toast.makeText(mContext, R.string.toast_message_no_external_storage, Toast.LENGTH_LONG).show();
        LogHelper.e(LOG_TAG, "Unable to access external storage.");

        return null;
    }

}
