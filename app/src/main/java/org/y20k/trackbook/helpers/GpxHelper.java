/**
 * GpxHelper.java
 * Implements the GpxHelper class
 * A GpxHelper can convert Track object into a GPX string
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

import android.location.Location;

import org.y20k.trackbook.core.Track;
import org.y20k.trackbook.core.WayPoint;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * GpxHelper class
 */
public class GpxHelper {

    /* Define log tag */
    private static final String LOG_TAG = GpxHelper.class.getSimpleName();


    /* Main class variables */
    private final Track mTrack;


    /* Constructor */
    public GpxHelper(Track track) {
        mTrack = track;
    }


    /* Creates GPX formatted string */
    public String createGpxString() {
        String gpxString;

        // add header
        gpxString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                    "<gpx version=\"1.1\" creator=\"Transistor App (Android)\">\n";

        // add track
        gpxString = gpxString + addTrack();

        // add closing tag
        gpxString = gpxString + "</gpx>\n";

        // todo remove
        LogHelper.v(LOG_TAG, "GPX output:\n" + gpxString);

        return gpxString;
    }


    /* Creates Track */
    private String addTrack() {
        StringBuilder gpxTrack = new StringBuilder("");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

        // add opening route tag
        gpxTrack.append("\t<rte>\n");

        // add route point
        for (WayPoint wayPoint:mTrack.getWayPoints()) {
            // get location from waypoint
            Location location = wayPoint.getLocation();

            // add longitude and latitude
            gpxTrack.append("\t\t<rtept lat=\"");
            gpxTrack.append(location.getLatitude());
            gpxTrack.append("\" lon=\"");
            gpxTrack.append(location.getLongitude());
            gpxTrack.append("\">\n");

            // add time
            gpxTrack.append("\t\t\t<time>");
            gpxTrack.append(dateFormat.format(new Date(location.getTime())));
            gpxTrack.append("</time>\n");

            // add altitude
            gpxTrack.append("\t\t\t<ele>");
            gpxTrack.append(location.getAltitude());
            gpxTrack.append("</ele>\n");

            // add closing tag
            gpxTrack.append("\t\t</rtept>\n");
        }

        // add closing route tag
        gpxTrack.append("\t</rte>\n");

        return gpxTrack.toString();
    }

}
