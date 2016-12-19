/**
 * WayPoint.java
 * Implements the WayPoint class
 * A WayPoint stores a location plus additional metadata
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
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;


/**
 * WayPoint class
 */
public class WayPoint implements Parcelable {

    private Location mLocation;
    private boolean mIsStopOver;
    private float mDistanceToStartingPoint;
    private final int mNumberSatellites;

    /* Constructor */
    public WayPoint(Location location, boolean isStopOver, float distanceToStartingPoint) {
        mLocation = location;
        mIsStopOver = isStopOver;
        mDistanceToStartingPoint = distanceToStartingPoint;

        // save number of satellites
        Bundle extras = location.getExtras();
        if (extras != null && extras.containsKey("satellites")) {
            mNumberSatellites = extras.getInt("satellites", 0);
            mLocation.setExtras(null); // necessary because Location Extras cause cannot be serialized properly by GSON
        } else {
            mNumberSatellites = 0;
        }

    }

    /* Constructor used by CREATOR */
    protected WayPoint(Parcel in) {
//        mLocation = in.readParcelable(Location.class.getClassLoader());
        mLocation = Location.CREATOR.createFromParcel(in);
        mIsStopOver = in.readByte() != 0;
        mDistanceToStartingPoint = in.readFloat();
        mNumberSatellites = in.readInt();
    }


    /* CREATOR for WayPoint object used to do parcel related operations */
    public static final Creator<WayPoint> CREATOR = new Creator<WayPoint>() {
        @Override
        public WayPoint createFromParcel(Parcel in) {
            return new WayPoint(in);
        }

        @Override
        public WayPoint[] newArray(int size) {
            return new WayPoint[size];
        }
    };


    /* Getter for mIsStopOver */
    public Location getLocation() {
        return mLocation;
    }


    /* Getter for mIsStopOver */
    public boolean getIsStopOver() {
        return mIsStopOver;
    }


    /* Getter for mDistanceToStartingPoint */
    public float getDistanceToStartingPoint() {
        return mDistanceToStartingPoint;
    }


    /* Setter for mLocation */
    public void setLocation(Location location) {
        mLocation = location;
    }


    /* Setter for mIsStopOver */
    public void setIsStopOver(boolean isStopOver) {
        mIsStopOver = isStopOver;
    }


    /* Setter for mDistanceToStartingPoint */
    public void setDistanceToStartingPoint(float distanceToStartingPoint) {
        mDistanceToStartingPoint = distanceToStartingPoint;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    @Override
    public void writeToParcel(Parcel parcel, int flags) {
//        parcel.writeParcelable(mLocation, flags);
        mLocation.setExtras(null); // necessary because Location Extras cause cannot be serialized properly by GSON
        mLocation.writeToParcel(parcel, flags);
        parcel.writeByte((byte) (mIsStopOver ? 1 : 0));
        parcel.writeFloat(mDistanceToStartingPoint);
        parcel.writeInt(mNumberSatellites);
    }
}