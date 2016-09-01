/**
 * Track.java
 * Implements the Track class
 * A Track stores a list of waypoints
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

import android.content.Context;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.y20k.trackbook.helpers.LocationHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

import java.util.ArrayList;
import java.util.List;


/**
 * Track class
 */
public class Track implements TrackbookKeys, Parcelable {

    /* Define log tag */
    private static final String LOG_TAG = Track.class.getSimpleName();


    /* Main class variables */
    private Context mContext;
    private List<WayPoint> mWayPoints;
    private float mTrackLength;


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


    /* Set mContext needed by  */
    public void setContext(Context context) {
        mContext = context;
    }


    /* Adds new waypoint */
    public WayPoint addWayPoint(Location location) {
        // add up distance
        mTrackLength = addDistanceToTrack(location);

        // create new waypoint
        WayPoint wayPoint = new WayPoint(location, LocationHelper.isStopOver(location), mTrackLength);

        // add new waypoint to track
        mWayPoints.add(wayPoint);

        // TODO remove log here
        LogHelper.v(LOG_TAG, "Waypoint No. " + mWayPoints.indexOf(wayPoint) + " Location: " + wayPoint.getLocation().toString());

        return wayPoint;
    }


    /* Getter for mWayPoints */
    public List<WayPoint> getWayPoints() {
        return mWayPoints;
    }


    /* Getter size of Track / number of WayPoints */
    public int getSize() {
        return mWayPoints.size();
    }


    /* Getter for location of specific WayPoint */
    public Location getWayPointLocation(int index) {
        return mWayPoints.get(index).getLocation();
    }


    /* Adds distance to given location to length of track */
    private float addDistanceToTrack(Location location) {
        // get number of previously recorded waypoints
        int wayPointCount = mWayPoints.size();

        // at least two data points are needed
        if (wayPointCount >= 2) {
            // add up distance
            Location lastLocation = mWayPoints.get(wayPointCount-2).getLocation();
            mTrackLength = mTrackLength + lastLocation.distanceTo(location);
        }

        return mTrackLength;
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
