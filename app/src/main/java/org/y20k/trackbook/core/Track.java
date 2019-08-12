/**
 * Track.java
 * Implements the Track class
 * A Track stores a list of WayPoints
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-19 - Y20K.org
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

import androidx.annotation.Nullable;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.y20k.trackbook.helpers.LocationHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


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
    private double mMaxAltitude;
    private double mMinAltitude;
    private double mPositiveElevation;
    private double mNegativeElevation;
    private BoundingBox mBoundingBox;


    /* Generic Constructor */
    public Track(int trackFormatVersion, List<WayPoint> wayPoints, float trackLength, long duration, float stepCount, Date recordingStart, Date recordingStop, double maxAltitude, double minAltitude, double positiveElevation, double negativeElevation, BoundingBox boundingBox) {
        mTrackFormatVersion = trackFormatVersion;
        mWayPoints = wayPoints;
        mTrackLength = trackLength;
        mDuration = duration;
        mStepCount = stepCount;
        mRecordingStart = recordingStart;
        mRecordingStop = recordingStop;
        mMaxAltitude = maxAltitude;
        mMinAltitude = minAltitude;
        mPositiveElevation = positiveElevation;
        mNegativeElevation = negativeElevation;
        mBoundingBox = boundingBox;
    }


    /* Copy Constructor */
    public Track(Track track) {
        this(track.getTrackFormatVersion(), track.getWayPoints(), track.getTrackLength(), track.getTrackDuration(), track.getStepCount(), track.getRecordingStart(), track.getRecordingStop(), track.getMaxAltitude(), track.getMinAltitude(), track.getPositiveElevation(), track.getNegativeElevation(), track.getBoundingBox());
    }


    /* Constructor */
    public Track() {
        mTrackFormatVersion = CURRENT_TRACK_FORMAT_VERSION;
        mWayPoints = new ArrayList<WayPoint>();
        mTrackLength = 0f;
        mDuration = 0;
        mStepCount = 0f;
        mRecordingStart = GregorianCalendar.getInstance().getTime();
        mRecordingStop = mRecordingStart;
        mMaxAltitude = 0f;
        mMinAltitude = 0f;
        mPositiveElevation = 0f;
        mNegativeElevation = 0f;
        mBoundingBox = new BoundingBox();
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
        mMaxAltitude = in.readDouble();
        mMinAltitude = in.readDouble();
        mPositiveElevation = in.readDouble();
        mNegativeElevation = in.readDouble();
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
    public boolean addWayPoint(@Nullable Location previousLocation, Location newLocation) {

        // toggle stop over status, if necessary
        boolean isStopOver = LocationHelper.isStopOver(previousLocation, newLocation);
        if (isStopOver) {
            int wayPointCount = mWayPoints.size();
            mWayPoints.get(wayPointCount-1).setIsStopOver(isStopOver);
        }

        // create new WayPoint
        WayPoint wayPoint = new WayPoint(newLocation, false, mTrackLength);

        // add new WayPoint to track
        return mWayPoints.add(wayPoint);
    }


    /* Updates distance */
    public boolean updateDistance(@Nullable Location previousLocation, Location newLocation){
        // two data points needed to calculate distance
        if (previousLocation != null) {
            // add up distance
            mTrackLength = mTrackLength + previousLocation.distanceTo(newLocation);
            return true;
        } else {
            // this was the first waypoint
            return false;
        }
    }


    /* Toggles stop over status of last waypoint */
    public void toggleLastWayPointStopOverStatus(boolean stopOver) {
        int wayPointCount = mWayPoints.size();
        mWayPoints.get(wayPointCount-1).setIsStopOver(stopOver);
    }


    /* Sets end time and date of recording */
    public void setRecordingEnd() {
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


    /* Setter for maximum altitude of recording */
    public void setMaxAltitude(double maxAltitude) {
        mMaxAltitude = maxAltitude;
    }


    /* Setter for lowest altitude of recording */
    public void setMinAltitude(double minAltitude) {
        mMinAltitude = minAltitude;
    }


    /* Setter for positive elevation of recording (cumulative altitude difference) */
    public void setPositiveElevation(double positiveElevation) {
        mPositiveElevation = positiveElevation;
    }


    /* Setter for negative elevation of recording (cumulative altitude difference) */
    public void setNegativeElevation(double negativeElevation) {
        mNegativeElevation = negativeElevation;
    }


    /* Setter for this track's BoundingBox - a data structure describing the edge coordinates of a track */
    public void setBoundingBox(BoundingBox boundingBox) {
        mBoundingBox = boundingBox;
    }


    /* Getter for file/track format version */
    public int getTrackFormatVersion() {
        return mTrackFormatVersion;
    }


    /* Getter for mWayPoints */
    public List<WayPoint> getWayPoints() {
        return mWayPoints;
    }


    /* Getter size of Track / number of WayPoints */
    public int getSize() {
        return mWayPoints.size();
    }


    /* Getter for track length */
    public float getTrackLength() {
        return mTrackLength;
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


    /* Getter for maximum altitude of recording */
    public double getMaxAltitude() {
        return mMaxAltitude;
    }


    /* Getter for lowest altitude of recording */
    public double getMinAltitude() {
        return mMinAltitude;
    }


    /* Getter for positive elevation of recording (cumulative altitude difference) */
    public double getPositiveElevation() {
        return mPositiveElevation;
    }


    /* Getter for negative elevation of recording (cumulative altitude difference) */
    public double getNegativeElevation() {
        return mNegativeElevation;
    }


    /* Getter for this track's BoundingBox - a data structure describing the edge coordinates of a track */
    public BoundingBox getBoundingBox() { return mBoundingBox; }


    /* Getter recorded distance */
    public Double getTrackDistance() {
        int size = mWayPoints.size();
        if (size > 0) {
            return (double)mWayPoints.get(size - 1).getDistanceToStartingPoint();
        } else {
            return (double)0f;
        }
    }


    /* Getter for location of specific WayPoint */
    public Location getWayPointLocation(int index) {
        return mWayPoints.get(index).getLocation();
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
        parcel.writeDouble(mMaxAltitude);
        parcel.writeDouble(mMinAltitude);
        parcel.writeDouble(mPositiveElevation);
        parcel.writeDouble(mNegativeElevation);
    }


}
