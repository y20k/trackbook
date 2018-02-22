/**
 * ExportHelper.java
 * Implements the ExportHelper class
 * A ExportHelper can convert Track object into a GPX string
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
import java.util.TimeZone;

/**
 * ExportHelper class
 */
public final class ExportHelper implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = ExportHelper.class.getSimpleName();


    /* Checks if a GPX file for given track is already present */
    public static boolean gpxFileExists(Track track) {
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return createFile(track, folder).exists();
    }


    /* Exports given track to GPX */
    public static boolean exportToGpx(Context context, Track track) {
        // get "Download" folder
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        // create "Download" folder if necessary
        if (folder != null && !folder.exists()) {
            LogHelper.v(LOG_TAG, "Creating new folder: " + folder.toString());
            folder.mkdirs();
        }

        // get file for given track
        File gpxFile = createFile(track, folder);

        // get GPX string representation for given track
        String gpxString = createGpxString(track);

        // write GPX file
        if (writeGpxToFile(gpxString, gpxFile)) {
            String toastMessage = context.getResources().getString(R.string.toast_message_export_success) + " " + gpxFile.toString();
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
            return true;
        } else {
            String toastMessage = context.getResources().getString(R.string.toast_message_export_fail) + " " + gpxFile.toString();
            Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
            return false;
        }
    }


    /* Return a GPX filepath for a given track */
    private static File createFile(Track track, File folder) {
        Date recordingStart = track.getRecordingStart();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
        return new File(folder, dateFormat.format(recordingStart) + FILE_TYPE_GPX_EXTENSION);
    }


    /* Writes given GPX string to Download folder */
    private static boolean writeGpxToFile (String gpxString, File gpxFile) {
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
    private static String createGpxString(Track track) {
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
    private static String addTrack(Track track) {
        StringBuilder gpxTrack = new StringBuilder("");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // add opening track tag
        gpxTrack.append("\t<trk>\n");

        // add name to track
        gpxTrack.append("\t\t<name>");
        gpxTrack.append("Trackbook Recording");
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
        gpxTrack.append("\t\t</trkseg>\n");

        // add closing track tag
        gpxTrack.append("\t</trk>\n");

        return gpxTrack.toString();
    }

}
