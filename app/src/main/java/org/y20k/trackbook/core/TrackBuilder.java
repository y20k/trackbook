/**
 * TrackBuilder.java
 * Implements a builder for the Track class
 * A TrackBuilder can build a track object depending on the version of its file format
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

import org.y20k.trackbook.helpers.LogHelper;

import java.util.Date;
import java.util.List;


/**
 * TrackBuilder class
 */
public class TrackBuilder {

    /* Define log tag */
    private static final String LOG_TAG = TrackBuilder.class.getSimpleName();


    /* Main class variables */
    private final int mTrackFormatVersion;
    private final List<WayPoint> mWayPoints;
    private final float mTrackLength;
    private final long mDuration;
    private final float mStepCount;
    private final Date mRecordingStart;
    private final Date mRecordingStop;
    private final double mMaxAltitude;
    private final double mMinAltitude;
    private final double mPositiveElevation;
    private final double mNegativeElevation;


    /* Generic Constructor */
    public TrackBuilder(int trackFormatVersion, List<WayPoint> wayPoints, float trackLength, long duration, float stepCount, Date recordingStart, Date recordingStop, double maxAltitude, double minAltitude, double positiveElevation, double negativeElevation) {
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
    }


    /* Builds and return a Track object */
    public Track toTrack() {
        switch (mTrackFormatVersion) {
            case 1:
                // file format version 1 - does not have elevation data stored
                return new Track(mTrackFormatVersion, mWayPoints, mTrackLength, mDuration, mStepCount, mRecordingStart, mRecordingStop, 0f, 0f, 0f, 0f);
            case 2:
                // file format version 2 (current version)
                return new Track(mTrackFormatVersion, mWayPoints, mTrackLength, mDuration, mStepCount, mRecordingStart, mRecordingStop, mMaxAltitude, mMinAltitude, mPositiveElevation, mNegativeElevation);
            default:
                LogHelper.e(LOG_TAG, "Unknown file format version: " + mTrackFormatVersion);
                return null;
        }
    }

}
