/**
 * ExportHelper.java
 * Implements the ExportHelper class
 * A ExportHelper can convert Track object into a GPX string
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
import android.location.Location;
import android.os.Environment;
import android.widget.Toast;

import org.y20k.trackbook.R;
import org.y20k.trackbook.core.Track;
import org.y20k.trackbook.core.WayPoint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ExportHelper class
 */
public class ExportHelper implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = ExportHelper.class.getSimpleName();


    /* Main class variables */
//    private final Track mTrack;
    private final Context mContext;
    private File mFolder;


    /* Constructor */
    public ExportHelper(Context context) {
        mContext = context;
        mFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }


    /* Checks if a GPX file for given track is already present */
    public boolean gpxFileExists(Track track) {
        return createFile(track).exists();
    }


    /* Exports given track to GPX */
    public boolean exportToGpx(Track track) {

        // create "Download" folder if necessary
        if (mFolder != null && !mFolder.exists()) {
            LogHelper.v(LOG_TAG, "Creating new folder: " + mFolder.toString());
            mFolder.mkdirs();
        }

        // get file for given track
        File gpxFile = createFile(track);

        // get GPX string representation for given track
        String gpxString = createGpxString(track);

        // write GPX file
        if (writeGpxToFile(gpxString, gpxFile)) {
            String toastMessage = mContext.getResources().getString(R.string.toast_message_export_success) + " " + gpxFile.toString();
            Toast.makeText(mContext, toastMessage, Toast.LENGTH_LONG).show();
            return true;
        } else {
            String toastMessage = mContext.getResources().getString(R.string.toast_message_export_fail) + " " + gpxFile.toString();
            Toast.makeText(mContext, toastMessage, Toast.LENGTH_LONG).show();
            return false;
        }
    }


    /* Return a GPX filepath for a given track */
    private File createFile(Track track) {
        Date recordingStart = track.getRecordingStart();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
        return new File(mFolder, dateFormat.format(recordingStart) + FILE_TYPE_GPX_EXTENSION);
    }


    /* Writes given GPX string to Download folder */
    private boolean writeGpxToFile (String gpxString, File gpxFile) {
        // write track
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(gpxFile))) {
            LogHelper.v(LOG_TAG, "Saving track to external storage: " + gpxFile.toString());
            bw.write(gpxString);
            return true;
        } catch (IOException e) {
            LogHelper.e(LOG_TAG, "Unable to saving track to external storage (IOException): " + gpxFile.toString());
            return false;
        }
    }


    /* Creates GPX formatted string */
    private String createGpxString(Track track) {
        String gpxString;

        // add header
        gpxString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                    "<gpx version=\"1.1\" creator=\"Transistor App (Android)\"\n" +
                    "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n";

        // add track
        gpxString = gpxString + addTrack(track);

        // add closing tag
        gpxString = gpxString + "</gpx>\n";

        return gpxString;
    }


    /* Creates Track */
    private String addTrack(Track track) {
        StringBuilder gpxTrack = new StringBuilder("");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        // add opening track tag
        gpxTrack.append("\t<trk>\n");

        // add name to track
        gpxTrack.append("\t\t<name>");
        gpxTrack.append("test");
        gpxTrack.append("</name>\n");

        // add opening track segment tag
        gpxTrack.append("\t\t<trkseg>\n");

        // add route point
        for (WayPoint wayPoint:track.getWayPoints()) {
            // get location from waypoint
            Location location = wayPoint.getLocation();

            // add longitude and latitude
            gpxTrack.append("\t\t\t<trkpt lat=\"");
            gpxTrack.append(location.getLatitude());
            gpxTrack.append("\" lon=\"");
            gpxTrack.append(location.getLongitude());
            gpxTrack.append("\">\n");

            // add time
            gpxTrack.append("\t\t\t\t<time>");
            gpxTrack.append(dateFormat.format(new Date(location.getTime())));
            gpxTrack.append("</time>\n");

            // add altitude
            gpxTrack.append("\t\t\t\t<ele>");
            gpxTrack.append(location.getAltitude());
            gpxTrack.append("</ele>\n");

            // add closing tag
            gpxTrack.append("\t\t\t</trkpt>\n");
        }

        // add closing track segment tag
        gpxTrack.append("\t\t<trkseg>\n");

        // add closing track tag
        gpxTrack.append("\t</trk>\n");

        return gpxTrack.toString();
    }

}
