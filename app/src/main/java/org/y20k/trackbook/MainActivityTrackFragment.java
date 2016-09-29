/**
 * MainActivityTrackFragment.java
 * Implements the track fragment used in the track tab of the main activity
 * This fragment displays a saved track
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

package org.y20k.trackbook;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.y20k.trackbook.core.Track;
import org.y20k.trackbook.helpers.StorageHelper;


/**
 * MainActivityTrackFragment class
 */
public class MainActivityTrackFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = MainActivityTrackFragment.class.getSimpleName();


    /* Main class variables */
    private Activity mActivity;
    private View mRootView;
    private TextView mDistanceView;
    private TextView mStepsView;
    private TextView mWaypointsView;
    private TextView mDurationView;
    private TextView mRecordingStartView;
    private TextView mRecordingStopView;
    private View mStatisticsSheet;
    private BottomSheetBehavior mStatisticsSheetBehavior;
    private Track mTrack;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // action bar has options menu
        setHasOptionsMenu(true);

        // store activity
        mActivity = getActivity();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_main_track, container, false);

        // get views
        mDistanceView = (TextView) mRootView.findViewById(R.id.statistics_data_distance);
        mStepsView = (TextView) mRootView.findViewById(R.id.statistics_data_steps);
        mWaypointsView = (TextView) mRootView.findViewById(R.id.statistics_data_waypoints);
        mDurationView = (TextView) mRootView.findViewById(R.id.statistics_data_duration);
        mRecordingStartView = (TextView) mRootView.findViewById(R.id.statistics_data_recording_start);
        mRecordingStopView = (TextView) mRootView.findViewById(R.id.statistics_data_recording_stop);
        mStatisticsSheet = mRootView.findViewById(R.id.statistic_sheet);

        mStatisticsSheetBehavior = BottomSheetBehavior.from(mStatisticsSheet);

        return mRootView;
    }


    @Override
    public void onResume() {
        super.onResume();

        // get most current track from storage
        StorageHelper storageHelper = new StorageHelper(mActivity);
        mTrack = storageHelper.loadTrack(storageHelper.getMostCurrentTrack());

        // populate views
        if (mTrack != null) {
            mDistanceView.setText(mTrack.getTrackDistance());
            mStepsView.setText(String.valueOf(mTrack.getStepCount()));
            mWaypointsView.setText(String.valueOf(mTrack.getWayPoints().size()));
            mDurationView.setText(mTrack.getTrackDuration());
            mRecordingStartView.setText(mTrack.getRecordingStart().toString());
            mRecordingStopView.setText(mTrack.getRecordingStop().toString());
        }

        mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);


    }


    @Override
    public void onPause() {
        super.onPause();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


}