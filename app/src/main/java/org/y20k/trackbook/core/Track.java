/**
 * Track.java
 * Implements the Track class
 * A Track stores a list of WayPoints
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

package org.y20k.trackbook.core;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.y20k.trackbook.helpers.LocationHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Track class
 */
public class Track implements TrackbookKeys, Parcelable {

    /* Define log tag */
    private static final String LOG_TAG = Track.class.getSimpleName();


    /* Main class variables */
    private final List<WayPoint> mWayPoints;
    private float mTrackLength;
    private long mTrackDuration;


    /* Constructor */
    public Track() {
        mWayPoints = new ArrayList<WayPoint>();
        mTrackLength = 0;
    }


    /* Constructor used by CREATOR */
    protected Track(Parcel in) {
        mWayPoints = in.createTypedArrayList(WayPoint.CREATOR);
        mTrackLength = in.readFloat();
    }


    /* CREATOR for Track object used to do parcel related operations */
    public static final Creator<Track> CREATOR = new Creator<Track>() {
        @Override
        public Track createFromParcel(Parcel in) {
            return new Track(in);
        }

        @Override
        public Track[] newArray(int size) {
            return new Track[size];
        }
    };


    /* Adds new WayPoint */
    public WayPoint addWayPoint(Location location) {
        // add up distance
        mTrackLength = addDistanceToTrack(location);

        int wayPointCount = mWayPoints.size();

        // determine if last WayPoint was a stopover
        boolean stopOver = false;
        if (wayPointCount > 1) {
            Location lastLocation = mWayPoints.get(wayPointCount - 1).getLocation();
            stopOver = LocationHelper.isStopOver(lastLocation, location);
        }
        if (stopOver) {
            // mark last WayPoint as stopover
            LogHelper.v(LOG_TAG, "Last Location was a stop.");
            mWayPoints.get(wayPointCount-1).setIsStopOver(true);
        }

        // create new WayPoint
        WayPoint wayPoint = new WayPoint(location, false, mTrackLength);

        // add new WayPoint to track
        mWayPoints.add(wayPoint);

        return wayPoint;
    }


    /* Setter for duration of track */
    public void setTrackDuration(long trackDuration) {
        mTrackDuration = trackDuration;
    }


    /* Getter for mWayPoints */
    public List<WayPoint> getWayPoints() {
        return mWayPoints;
    }


    /* Getter size of Track / number of WayPoints */
    public int getSize() {
        return mWayPoints.size();
    }


    /* Getter for duration of track */
    public String getTrackDuration() {
        return LocationHelper.convertToReadableTime(mTrackDuration, true);
    }


    /* Getter for distance of track */
    public String getTrackDistance() {
        float trackDistance = mWayPoints.get(mWayPoints.size()-1).getDistanceToStartingPoint();

        return String.format (Locale.ENGLISH, "%.0f", trackDistance)  + "m";
    }


    /* Getter for location of specific WayPoint */
    public Location getWayPointLocation(int index) {
        return mWayPoints.get(index).getLocation();
    }

    /* Adds distance to given location to length of track */
    private float addDistanceToTrack(Location location) {
        // get number of previously recorded WayPoints
        int wayPointCount = mWayPoints.size();

        // at least two data points are needed
        if (wayPointCount >= 1) {
            // add up distance
            Location lastLocation = mWayPoints.get(wayPointCount - 1).getLocation();
            return mTrackLength + lastLocation.distanceTo(location);
        }

        return 0f;
    }


    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(mWayPoints);
        parcel.writeFloat(mTrackLength);
    }


}
