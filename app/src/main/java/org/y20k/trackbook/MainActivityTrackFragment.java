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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * MainActivityTrackFragment class
 */
public class MainActivityTrackFragment extends Fragment {

    /* Define log tag */
    private static final String LOG_TAG = MainActivityTrackFragment.class.getSimpleName();


    /* Main class variables */
    private View mRootView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // action bar has options menu
        setHasOptionsMenu(true);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_main_track, container, false);

        return mRootView;
    }


    @Override
    public void onResume() {
        super.onResume();
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