/**
 * Track.java
 * Implements the Track class
 * A Track stores a list of WayPoints
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

package org.y20k.trackbook.core;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import org.y20k.trackbook.helpers.LocationHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;


/**
 * Track class
 */
public class Track implements TrackbookKeys, Parcelable {

    /* Define log tag */
    private static final String LOG_TAG = Track.class.getSimpleName();


    /* Main class variables */
    private final int mTrackFormatVersion;
    private final List<WayPoint> mWayPoints;
    private float mTrackLength;
    private long mDuration;
    private float mStepCount;
    private final Date mRecordingStart;
    private Date mRecordingStop;


    /* Constructor */
    public Track() {
        mTrackFormatVersion = CURRENT_TRACK_FORMAT_VERSION;
        mWayPoints = new ArrayList<WayPoint>();
        mTrackLength = 0f;
        mDuration = 0;
        mStepCount = 0f;
        mRecordingStart = GregorianCalendar.getInstance().getTime();
        mRecordingStop = mRecordingStart;
    }


    /* Constructor used by CREATOR */
    protected Track(Parcel in) {
        mTrackFormatVersion = in.readInt();
        mWayPoints = in.createTypedArrayList(WayPoint.CREATOR);
        mTrackLength = in.readFloat();
        mDuration = in.readLong();
        mStepCount = in.readFloat();
        mRecordingStart = new Date(in.readLong());
        mRecordingStop = new Date(in.readLong());
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


    /* Sets end time and date of recording */
    public void setRecordingEnd () {
        mRecordingStop = GregorianCalendar.getInstance().getTime();
    }


    /* Setter for duration of track */
    public void setDuration(long duration) {
        mDuration = duration;
    }


    /* Setter for step count of track */
    public void setStepCount(float stepCount) {
        mStepCount = stepCount;
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
    public long getTrackDuration() {
        return mDuration;
    }

    /* Getter for start date of recording */
    public Date getRecordingStart() {
        return mRecordingStart;
    }


    /* Getter for stop date of recording */
    public Date getRecordingStop() {
        return mRecordingStop;
    }


    /* Getter for step count of recording */
    public float getStepCount() {
        return mStepCount;
    }


    /* Getter for string representation of track duration */
    public String getTrackDurationString() {
        return LocationHelper.convertToReadableTime(mDuration, true);
    }

    /* Getter for string representation of track distance */
    public String getTrackDistanceString() {
        float trackDistance;
        String unit;

        if (getUnitSystem(Locale.getDefault()) == IMPERIAL) {
            // get track distance and convert to feet
            trackDistance = mWayPoints.get(mWayPoints.size()-1).getDistanceToStartingPoint() * 3.28084f;
            unit = "ft";
        } else {
            // get track distance
            trackDistance = mWayPoints.get(mWayPoints.size()-1).getDistanceToStartingPoint();
            unit = "m";
        }
        return String.format (Locale.ENGLISH, "%.0f", trackDistance) + unit;
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


    /* Determines which unit system the device is using (metric or imperial) */
    private int getUnitSystem(Locale locale) {
        // America (US), Liberia (LR), Myanmar(MM) use the imperial system
        List<String> imperialSystemCountries = Arrays.asList("US", "LR", "MM");
        String countryCode = locale.getCountry();

        if (imperialSystemCountries.contains(countryCode)){
            return IMPERIAL;
        } else {
            return METRIC;
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(mTrackFormatVersion);
        parcel.writeTypedList(mWayPoints);
        parcel.writeFloat(mTrackLength);
        parcel.writeLong(mDuration);
        parcel.writeFloat(mStepCount);
        parcel.writeLong(mRecordingStart.getTime());
        parcel.writeLong(mRecordingStop.getTime());
    }



}
