/**
 * MainActivityTrackFragment.java
 * Implements the track fragment used in the track tab of the main activity
 * This fragment displays a saved track
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

package org.y20k.trackbook;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.y20k.trackbook.core.Track;
import org.y20k.trackbook.helpers.DropdownAdapter;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.MapHelper;
import org.y20k.trackbook.helpers.StorageHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

import java.io.File;
import java.text.DateFormat;
import java.util.Locale;


/**
 * MainActivityTrackFragment class
 */
public class MainActivityTrackFragment extends Fragment implements AdapterView.OnItemSelectedListener, TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = MainActivityTrackFragment.class.getSimpleName();


    /* Main class variables */
    private Activity mActivity;
    private View mRootView;
    private MapView mMapView;
    private IMapController mController;
    private ItemizedIconOverlay mTrackOverlay;
    private ArrayAdapter<String> mTrackSelectorAdapter;
    private DropdownAdapter mDropdownAdapter;
    private Spinner mDropdown;
    private TextView mDistanceView;
    private TextView mStepsView;
    private TextView mWaypointsView;
    private TextView mDurationView;
    private TextView mRecordingStartView;
    private TextView mRecordingStopView;
    private BottomSheetBehavior mStatisticsSheetBehavior;
    private int mCurrentTrack;
    private Track mTrack;
    private BroadcastReceiver mTrackSavedReceiver;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // action bar has options menu
        setHasOptionsMenu(true);

        // store activity
        mActivity = getActivity();

        // get current track
        if (savedInstanceState != null) {
            mCurrentTrack = savedInstanceState.getInt(INSTANCE_CURRENT_TRACK, 0);
        } else {
            mCurrentTrack = 0;
        }

        // create drop-down adapter
        mDropdownAdapter = new DropdownAdapter(mActivity);

        // listen for finished save operation
        mTrackSavedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_SAVE_FINISHED) && intent.getBooleanExtra(EXTRA_SAVE_FINISHED, false)) {
                    LogHelper.v(LOG_TAG, "Save operation detected. Start loading the new track.");

                    // load track and display map and statistics
                    LoadTrackAsyncHelper loadTrackAsyncHelper = new LoadTrackAsyncHelper();
                    loadTrackAsyncHelper.execute();

                    mDropdownAdapter.refresh();
                    mDropdownAdapter.notifyDataSetChanged();
                    mDropdown.setSelection(0);
                }
            }
        };
        IntentFilter trackSavedReceiverIntentFilter = new IntentFilter(ACTION_TRACK_SAVE);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mTrackSavedReceiver, trackSavedReceiverIntentFilter);

    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_main_track, container, false);

        // create basic map
        mMapView = (MapView) mRootView.findViewById(R.id.track_map);

        // get map controller
        mController = mMapView.getController();

        // basic map setup
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setTilesScaledToDpi(true);

        // add multi-touch capability
        mMapView.setMultiTouchControls(true);

        // add compass to map
        CompassOverlay compassOverlay = new CompassOverlay(mActivity, new InternalCompassOrientationProvider(mActivity), mMapView);
        compassOverlay.enableCompass();
        // move the compass overlay down a bit
        compassOverlay.setCompassCenter(35.0f, 80.0f);
        mMapView.getOverlays().add(compassOverlay);

        // initiate map state
        if (savedInstanceState != null) {
            // restore saved instance of map
            GeoPoint position = new GeoPoint(savedInstanceState.getDouble(INSTANCE_LATITUDE_TRACK_MAP, DEFAULT_LATITUDE), savedInstanceState.getDouble(INSTANCE_LONGITUDE_TRACK_MAP, DEFAULT_LONGITUDE));
            mController.setCenter(position);
            mController.setZoom(savedInstanceState.getInt(INSTANCE_ZOOM_LEVEL_MAIN_MAP, 16));
        } else {
            mController.setZoom(16);
        }

        // get views for track selector
        mDropdown = (Spinner) mRootView.findViewById(R.id.track_selector);

        // get views for statistics sheet
        View statisticsView = mRootView.findViewById(R.id.statistics_view);
        mDistanceView = (TextView) mRootView.findViewById(R.id.statistics_data_distance);
        mStepsView = (TextView) mRootView.findViewById(R.id.statistics_data_steps);
        mWaypointsView = (TextView) mRootView.findViewById(R.id.statistics_data_waypoints);
        mDurationView = (TextView) mRootView.findViewById(R.id.statistics_data_duration);
        mRecordingStartView = (TextView) mRootView.findViewById(R.id.statistics_data_recording_start);
        mRecordingStopView = (TextView) mRootView.findViewById(R.id.statistics_data_recording_stop);
        View mStatisticsSheet = mRootView.findViewById(R.id.statistics_sheet);

        if (savedInstanceState != null) {
            // get track from saved instance and display map and statistics
            mTrack = savedInstanceState.getParcelable(INSTANCE_TRACK_TRACK_MAP);
            displayTrack();
        } else if (mTrack == null) {
            // load track and display map and statistics // todo get via mCurrentTrack
            LoadTrackAsyncHelper loadTrackAsyncHelper = new LoadTrackAsyncHelper();
            loadTrackAsyncHelper.execute();
        } else {
            // just display map and statistics
            displayTrack();
        }

        // show statistics sheet
        mStatisticsSheetBehavior = BottomSheetBehavior.from(mStatisticsSheet);
        mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mStatisticsSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // react to state change
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        // statistics sheet expanded
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        // statistics sheet collapsed
                        mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        // statistics sheet hidden
                        mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // react to dragging events
            }
        });

        // react to tap on sheet heading
        statisticsView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStatisticsSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });


        return mRootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDropdown.setAdapter(mDropdownAdapter);
        mDropdown.setOnItemSelectedListener(this);
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
    public void onDestroyView(){
        super.onDestroyView();

        // deactivate map
        mMapView.onDetach();
    }


    @Override
    public void onDestroy() {
        LogHelper.v(LOG_TAG, "onDestroy called.");

        // remove listener
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mTrackSavedReceiver);

        super.onDestroy();
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        // update current track
        mCurrentTrack = i;

        // get track file
        File trackFile = mDropdownAdapter.getItem(i).getTrackFile();

        // load track and display map and statistics
        LoadTrackAsyncHelper loadTrackAsyncHelper = new LoadTrackAsyncHelper();
        loadTrackAsyncHelper.execute(trackFile);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putDouble(INSTANCE_LATITUDE_TRACK_MAP, mMapView.getMapCenter().getLatitude());
        outState.putDouble(INSTANCE_LONGITUDE_TRACK_MAP, mMapView.getMapCenter().getLongitude());
        outState.putInt(INSTANCE_ZOOM_LEVEL_TRACK_MAP, mMapView.getZoomLevel());
        outState.putParcelable(INSTANCE_TRACK_TRACK_MAP, mTrack);
        outState.putInt(INSTANCE_CURRENT_TRACK, mCurrentTrack);
        super.onSaveInstanceState(outState);
    }


    /* Displays map and statistics for track */
    private void displayTrack() {
        GeoPoint position;

        if (mTrack != null) {
            // set end of track as position
            Location lastLocation = mTrack.getWayPointLocation(mTrack.getSize() -1);
            position = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());

            String recordingStart = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(mTrack.getRecordingStart()) + " " +
                    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(mTrack.getRecordingStart());
            String recordingStop = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(mTrack.getRecordingStop()) + " " +
                    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(mTrack.getRecordingStop());

            // populate views
            mDistanceView.setText(mTrack.getTrackDistance());
            mStepsView.setText(String.valueOf(Math.round(mTrack.getStepCount())));
            mWaypointsView.setText(String.valueOf(mTrack.getWayPoints().size()));
            mDurationView.setText(mTrack.getTrackDuration());
            mRecordingStartView.setText(recordingStart);
            mRecordingStopView.setText(recordingStop);

            // draw track on map
            drawTrackOverlay(mTrack);
        } else {
            position = new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
        }

        // center map over position
        mController.setCenter(position);

    }


    /* Draws track onto overlay */
    private void drawTrackOverlay(Track track) {
        mMapView.getOverlays().remove(mTrackOverlay);
        mTrackOverlay = MapHelper.createTrackOverlay(mActivity, track, false);
        mMapView.getOverlays().add(mTrackOverlay);
    }


    /**
     * Inner class: Loads track from external storage using AsyncTask
     */
    private class LoadTrackAsyncHelper extends AsyncTask<File, Void, Void> {

        @Override
        protected Void doInBackground(File... files) {
            LogHelper.v(LOG_TAG, "Loading track object in background.");

            StorageHelper storageHelper = new StorageHelper(mActivity);

            if (files.length > 0) {
                // load track object from given file
                mTrack = storageHelper.loadTrack(files[0]);
            } else {
                // load track object from most current file
                mTrack = storageHelper.loadTrack(FILE_MOST_CURRENT_TRACK);
            }


            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            LogHelper.v(LOG_TAG, "Loading finished. Displaying map and statistics of track.");

            // display track on map
            displayTrack();
        }
    }
    /**
     * End of inner class
     */

}