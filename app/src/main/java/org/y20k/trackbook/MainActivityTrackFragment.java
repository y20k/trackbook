/**
 * MainActivityTrackFragment.java
 * Implements the track fragment used in the track tab of the main activity
 * This fragment displays a saved track
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

package org.y20k.trackbook;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.y20k.trackbook.core.Track;
import org.y20k.trackbook.helpers.DialogHelper;
import org.y20k.trackbook.helpers.DropdownAdapter;
import org.y20k.trackbook.helpers.ExportHelper;
import org.y20k.trackbook.helpers.LengthUnitHelper;
import org.y20k.trackbook.helpers.LocationHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.MapHelper;
import org.y20k.trackbook.helpers.NightModeHelper;
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
    private FragmentActivity mActivity;
    private View mRootView;
    private MapView mMapView;
    private LinearLayout mOnboardingView;
    private IMapController mController;
    private ItemizedIconOverlay mTrackOverlay;
    private DropdownAdapter mDropdownAdapter;
    private ConstraintLayout mTrackManagementLayout;
    private Spinner mDropdown;
    private View mStatisticsSheet;
    private View mStatisticsView;
    private TextView mDistanceView;
    private TextView mStepsView;
    private TextView mWaypointsView;
    private TextView mDurationView;
    private TextView mRecordingStartView;
    private TextView mRecordingStopView;
    private TextView mMaxAltitudeView;
    private TextView mMinAltitudeView;
    private TextView mPositiveElevationView;
    private TextView mNegativeElevationView;
    private Group mElevationDataViews;
    private Group mStatisticsHeaderViews;
    private BottomSheetBehavior mStatisticsSheetBehavior;
    private int mCurrentTrack;
    private Track mTrack;
    private BroadcastReceiver mTrackSavedReceiver;


    /* Return a new Instance of MainActivityTrackFragment */
    public static MainActivityTrackFragment newInstance() {
        return new MainActivityTrackFragment();
    }


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

                    // update dropdown menu (and load track in onItemSelected)
                    mDropdownAdapter.refresh();
                    mDropdownAdapter.notifyDataSetChanged();
                    mDropdown.setAdapter(mDropdownAdapter);
                    mDropdown.setSelection(0, true);

                    // remove onboarding if necessary
                    switchOnboardingLayout();
                }
            }
        };
        IntentFilter trackSavedReceiverIntentFilter = new IntentFilter(ACTION_TRACK_SAVE);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mTrackSavedReceiver, trackSavedReceiverIntentFilter);

    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // inflate root view from xml
        mRootView = inflater.inflate(R.layout.fragment_main_track, container, false);

        // get reference to onboarding layout
        mOnboardingView = (LinearLayout) mRootView.findViewById(R.id.track_tab_onboarding);

        // get reference to basic map
        mMapView = (MapView) mRootView.findViewById(R.id.track_map);

        // get map controller
        mController = mMapView.getController();

        // basic map setup
        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setTilesScaledToDpi(true);

        // set dark map tiles, if necessary
        if (NightModeHelper.getNightMode(mActivity)) {
            mMapView.getOverlayManager().getTilesOverlay().setColorFilter(TilesOverlay.INVERT_COLORS);
        }

        // add multi-touch capability
        mMapView.setMultiTouchControls(true);

        // disable default zoom controls
        mMapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);

        // add compass to map
        CompassOverlay compassOverlay = new CompassOverlay(mActivity, new InternalCompassOrientationProvider(mActivity), mMapView);
        compassOverlay.enableCompass();
        // move the compass overlay down a bit
        compassOverlay.setCompassCenter(35.0f, 96.0f);
        mMapView.getOverlays().add(compassOverlay);

        // initiate map state
        if (savedInstanceState != null) {
            // restore saved instance of map
            GeoPoint position = new GeoPoint(savedInstanceState.getDouble(INSTANCE_LATITUDE_TRACK_MAP, DEFAULT_LATITUDE), savedInstanceState.getDouble(INSTANCE_LONGITUDE_TRACK_MAP, DEFAULT_LONGITUDE));
            mController.setCenter(position);
            mController.setZoom(savedInstanceState.getDouble(INSTANCE_ZOOM_LEVEL_MAIN_MAP, 16f));
        } else {
            mController.setZoom(16f);
        }

        // get views for track selector
        mTrackManagementLayout = (ConstraintLayout) mRootView.findViewById(R.id.track_management_layout);
        mDropdown = (Spinner) mRootView.findViewById(R.id.track_selector);

        // attach listeners to export and delete buttons
        ImageButton shareButton = (ImageButton) mRootView.findViewById(R.id.share_button);
        ImageButton exportButton = (ImageButton) mRootView.findViewById(R.id.export_button);
        ImageButton deleteButton = (ImageButton) mRootView.findViewById(R.id.delete_button);
        shareButton.setOnClickListener(getShareButtonListener());
        exportButton.setOnClickListener(getExportButtonListener());
        deleteButton.setOnClickListener(getDeleteButtonListener());

        // get views for statistics sheet
        mStatisticsView = mRootView.findViewById(R.id.statistics_view);
        mStatisticsSheet = mRootView.findViewById(R.id.statistics_sheet);
        mDistanceView = (TextView) mRootView.findViewById(R.id.statistics_data_distance);
        mStepsView = (TextView) mRootView.findViewById(R.id.statistics_data_steps);
        mWaypointsView = (TextView) mRootView.findViewById(R.id.statistics_data_waypoints);
        mDurationView = (TextView) mRootView.findViewById(R.id.statistics_data_duration);
        mRecordingStartView = (TextView) mRootView.findViewById(R.id.statistics_data_recording_start);
        mRecordingStopView = (TextView) mRootView.findViewById(R.id.statistics_data_recording_stop);
        mMaxAltitudeView = (TextView) mRootView.findViewById(R.id.statistics_data_max_altitude);
        mMinAltitudeView = (TextView) mRootView.findViewById(R.id.statistics_data_min_altitude);
        mPositiveElevationView = (TextView) mRootView.findViewById(R.id.statistics_data_positive_elevation);
        mNegativeElevationView = (TextView) mRootView.findViewById(R.id.statistics_data_negative_elevation);
        mElevationDataViews = (Group) mRootView.findViewById(R.id.elevation_data);
        mStatisticsHeaderViews = (Group) mRootView.findViewById(R.id.statistics_header);


        // display map and statistics
        if (savedInstanceState != null) {
            // get track from saved instance and display map and statistics
            mTrack = savedInstanceState.getParcelable(INSTANCE_TRACK_TRACK_MAP);
            displayTrack();
        } else if (mTrack == null) {
            // load track and display map and statistics
            LoadTrackAsyncHelper loadTrackAsyncHelper = new LoadTrackAsyncHelper();
            loadTrackAsyncHelper.execute();
        } else {
            // just display map and statistics
            displayTrack();
        }

        // set up and show statistics sheet
        mStatisticsSheetBehavior = BottomSheetBehavior.from(mStatisticsSheet);
        mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        mStatisticsSheetBehavior.setBottomSheetCallback(getStatisticsSheetCallback());

        // attach listener for taps on elevation views
        attachTapListenerToElevationViews();

        // attach listener for taps on statistics sheet header
        attachTapListenerToStatisticHeaderViews();

        // attach listener for taps on statistics - for US and other states plagued by Imperial units
        if (LengthUnitHelper.getUnitSystem() == IMPERIAL || Locale.getDefault().getCountry().equals("GB")) {
            attachTapListenerToStatisticsSheet();
        }

        return mRootView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mDropdown.setAdapter(mDropdownAdapter);
        mDropdown.setOnItemSelectedListener(this);
    }


    @Override
    public void onResume() {
        super.onResume();
        // show / hide the onboarding layout
        switchOnboardingLayout();
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

        // load track and display map and statistics
        LoadTrackAsyncHelper loadTrackAsyncHelper = new LoadTrackAsyncHelper();
        loadTrackAsyncHelper.execute(i);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putDouble(INSTANCE_LATITUDE_TRACK_MAP, mMapView.getMapCenter().getLatitude());
        outState.putDouble(INSTANCE_LONGITUDE_TRACK_MAP, mMapView.getMapCenter().getLongitude());
        outState.putDouble(INSTANCE_ZOOM_LEVEL_TRACK_MAP, mMapView.getZoomLevelDouble());
        outState.putParcelable(INSTANCE_TRACK_TRACK_MAP, mTrack);
        outState.putInt(INSTANCE_CURRENT_TRACK, mCurrentTrack);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case RESULT_DELETE_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    deleteCurrentTrack();
                } else if (resultCode == Activity.RESULT_CANCELED){
                    LogHelper.v(LOG_TAG, "Delete dialog result: CANCEL");
                }
                break;
            case RESULT_EXPORT_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    // user chose EXPORT
                    ExportHelper.exportToGpx(mActivity, mTrack);
                } else if (resultCode == Activity.RESULT_CANCELED){
                    // User chose CANCEL
                    LogHelper.v(LOG_TAG, "Export to GPX: User chose CANCEL.");
                }
                break;
        }
    }


    /* Displays map and statistics for track */
    private void displayTrack() {
        GeoPoint position;

        if (mTrack != null && mTrack.getSize() > 0) {
            // set end of track as position
            Location lastLocation = mTrack.getWayPointLocation(mTrack.getSize() -1);
            position = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());

            String recordingStart = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(mTrack.getRecordingStart()) + " " +
                    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(mTrack.getRecordingStart());
            String recordingStop = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(mTrack.getRecordingStop()) + " " +
                    DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(mTrack.getRecordingStop());
            String stepsTaken;
            if (mTrack.getStepCount() == -1) {
                stepsTaken = getString(R.string.statistics_sheet_p_steps_no_pedometer);
            } else {
                stepsTaken = String.valueOf(Math.round(mTrack.getStepCount()));
            }

            // populate length views
            displayCurrentLengthUnits();
            // populate other views
            mStepsView.setText(stepsTaken);
            mWaypointsView.setText(String.valueOf(mTrack.getWayPoints().size()));
            mDurationView.setText(LocationHelper.convertToReadableTime(mTrack.getTrackDuration(), true));
            mRecordingStartView.setText(recordingStart);
            mRecordingStopView.setText(recordingStop);

            // show/hide elevation views depending on file format version
            if (mTrack.getTrackFormatVersion() > 1 && mTrack.getMinAltitude() > 0) {
                // show elevation views
                mElevationDataViews.setVisibility(View.VISIBLE);
            } else {
                // hide elevation views
                mElevationDataViews.setVisibility(View.GONE);
            }

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


    /* show the onboarding layout, if no track has been recorded yet */
    private void switchOnboardingLayout() {
        if (mDropdownAdapter.isEmpty()){
            // show onboarding layout
            mMapView.setVisibility(View.GONE);
            mOnboardingView.setVisibility(View.VISIBLE);
            mTrackManagementLayout.setVisibility(View.GONE);
            mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            mStatisticsSheet.setVisibility(View.GONE);
        } else {
            // show normal layout
            mOnboardingView.setVisibility(View.GONE);
            mMapView.setVisibility(View.VISIBLE);
            mTrackManagementLayout.setVisibility(View.VISIBLE);
            mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            mStatisticsSheet.setVisibility(View.VISIBLE);
        }
    }


    /* Displays views in statistic sheet according to current locale */
    private void displayCurrentLengthUnits() {
        mDistanceView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getTrackDistance()));
        mPositiveElevationView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getPositiveElevation()));
        mNegativeElevationView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getNegativeElevation()));
        mMaxAltitudeView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getMaxAltitude()));
        mMinAltitudeView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getMinAltitude()));
    }


    /* Switches views in statistic sheet between Metric and Imperial */
    private void displayOppositeLengthUnits() {
        int oppositeLengthUnit = LengthUnitHelper.getUnitSystem() * -1;
        mDistanceView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getTrackDistance(), oppositeLengthUnit));
        mPositiveElevationView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getPositiveElevation(), oppositeLengthUnit));
        mNegativeElevationView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getNegativeElevation(), oppositeLengthUnit));
        mMaxAltitudeView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getMaxAltitude(), oppositeLengthUnit));
        mMinAltitudeView.setText(LengthUnitHelper.convertDistanceToString(mTrack.getMinAltitude(), oppositeLengthUnit));
    }


    /* Deletes currently visible track */
    private void deleteCurrentTrack() {

        // delete track file and refresh dropdown adapter
        if (mDropdownAdapter.getItem(mCurrentTrack).getTrackFile().delete()) {
            mDropdownAdapter.refresh();
            mDropdownAdapter.notifyDataSetChanged();
            mDropdown.setAdapter(mDropdownAdapter);
        } else {
            LogHelper.e(LOG_TAG, "Unable to delete recording.");
            return;
        }

        if (mDropdownAdapter.isEmpty()) {
            // show onboarding
            switchOnboardingLayout();
        } else {
            // show next track
            mDropdown.setSelection(0, true);
            mCurrentTrack = 0;
        }

    }


    /* Creates BottomSheetCallback for the statistics sheet - needed in onCreateView */
    private BottomSheetBehavior.BottomSheetCallback getStatisticsSheetCallback() {
        return new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // react to state change
                switch (newState) {
                    case BottomSheetBehavior.STATE_EXPANDED:
                        // statistics sheet expanded
                        mTrackManagementLayout.setVisibility(View.INVISIBLE);
                        mStatisticsSheet.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.statistic_sheet_background_expanded));
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        // statistics sheet collapsed
                        mTrackManagementLayout.setVisibility(View.VISIBLE);
                        mStatisticsSheet.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.statistic_sheet_background_collapsed));
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
                // reset length unit displays
                displayCurrentLengthUnits();
                // react to dragging events
                if (slideOffset < 0.5f) {
                    mTrackManagementLayout.setVisibility(View.VISIBLE);
                } else {
                    mTrackManagementLayout.setVisibility(View.INVISIBLE);
                }
                if (slideOffset < 0.125f) {
                    mStatisticsSheet.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.statistic_sheet_background_collapsed));
                } else {
                    mStatisticsSheet.setBackgroundColor(ContextCompat.getColor(mActivity, R.color.statistic_sheet_background_expanded));
                }
            }
        };
    }


    /* Creates OnClickListener for the export button - needed in onCreateView */
    private View.OnClickListener getShareButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = ExportHelper.getGpxFileIntent(mActivity, mTrack);
                // create intent to show chooser
                String title = getString(R.string.dialog_share_gpx);
//                String title = getResources().getString(R.string.chooser_title);
                Intent chooser = Intent.createChooser(intent, title);
                if (intent.resolveActivity(mActivity.getPackageManager()) != null) {
                    startActivity(chooser);
                } else {
                    Toast.makeText(mActivity, R.string.toast_message_install_file_helper, Toast.LENGTH_LONG).show();
                }
            }
        };
    }


    /* Creates OnClickListener for the export button - needed in onCreateView */
    private View.OnClickListener getExportButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // dialog text components
                int dialogTitle;
                int dialogPositiveButton;
                int dialogNegativeButton;
                DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
                String recordingStartDate = df.format(mTrack.getRecordingStart());
                String dialogMessage;

                // get text elements for delete dialog
                if (ExportHelper.gpxFileExists(mTrack)) {
                    // CASE: OVERWRITE - GPX file exists
                    dialogTitle = R.string.dialog_export_title_overwrite;
                    dialogMessage = getString(R.string.dialog_export_content_overwrite) + " (" + recordingStartDate + " | " + LengthUnitHelper.convertDistanceToString(mTrack.getTrackDistance()) + ")";
                    dialogPositiveButton = R.string.dialog_export_action_overwrite;
                    dialogNegativeButton = R.string.dialog_default_action_cancel;
                } else {
                    // CASE: EXPORT - GPX file does NOT yet exits
                    dialogTitle = R.string.dialog_export_title_export;
                    dialogMessage = getString(R.string.dialog_export_content_export) + " (" + recordingStartDate + " | " + LengthUnitHelper.convertDistanceToString(mTrack.getTrackDistance()) + ")";
                    dialogPositiveButton = R.string.dialog_export_action_export;
                    dialogNegativeButton = R.string.dialog_default_action_cancel;
                }

                // show delete dialog - results are handles by onActivityResult
                DialogFragment dialogFragment = DialogHelper.newInstance(dialogTitle, dialogMessage, dialogPositiveButton, dialogNegativeButton);
                dialogFragment.setTargetFragment(MainActivityTrackFragment.this, RESULT_EXPORT_DIALOG);
                dialogFragment.show(mActivity.getSupportFragmentManager(), "ExportDialog");
            }
        };
    }


    /* Creates OnClickListener for the delete button - needed in onCreateView */
    private View.OnClickListener getDeleteButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get text elements for delete dialog
                int dialogTitle = R.string.dialog_delete_title;
                int dialogPositiveButton = R.string.dialog_delete_action_delete;
                int dialogNegativeButton = R.string.dialog_default_action_cancel;
                DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
                String recordingStartDate = df.format(mTrack.getRecordingStart());
                String dialogMessage = getString(R.string.dialog_delete_content) + " " + recordingStartDate + " | " + LengthUnitHelper.convertDistanceToString(mTrack.getTrackDistance());

                // show delete dialog - results are handles by onActivityResult
                DialogFragment dialogFragment = DialogHelper.newInstance(dialogTitle, dialogMessage, dialogPositiveButton, dialogNegativeButton);
                dialogFragment.setTargetFragment(MainActivityTrackFragment.this, RESULT_DELETE_DIALOG);
                dialogFragment.show(mActivity.getSupportFragmentManager(), "DeleteDialog");
            }
        };
    }


    /* Add tap listener to elevation data views */
    private void attachTapListenerToElevationViews() {
        int referencedIds[] = mElevationDataViews.getReferencedIds();
        for (int id : referencedIds) {
            mRootView.findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // inform user about possible issues with altitude measurements
                    Toast.makeText(mActivity, R.string.toast_message_elevation_info, Toast.LENGTH_LONG).show();
                }
            });
        }
    }


    /* Add tap listener to statistic header views */
    private void attachTapListenerToStatisticHeaderViews() {
        int referencedIds[] = mStatisticsHeaderViews.getReferencedIds();
        for (int id : referencedIds) {
            mRootView.findViewById(id).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mStatisticsSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                        mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    } else {
                        mStatisticsSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                }
            });
        }
    }


    /* Add tap listener to statistics sheet */
    private void attachTapListenerToStatisticsSheet() {
        mStatisticsView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    displayOppositeLengthUnits();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    displayCurrentLengthUnits();
                }
                return true;
            }
        });
    }


    /**
     * Inner class: Loads track from external storage using AsyncTask
     */
    private class LoadTrackAsyncHelper extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... ints) {
            LogHelper.v(LOG_TAG, "Loading track object in background.");

            StorageHelper storageHelper = new StorageHelper(mActivity);
            if (ints.length > 0) {
                // get track file from dropdown adapter
                int item = ints[0];
                File trackFile = mDropdownAdapter.getItem(item).getTrackFile();
                LogHelper.v(LOG_TAG, "Loading track number " + item);
                mTrack = storageHelper.loadTrack(trackFile);
            } else {
                // load track object from most current file
                LogHelper.v(LOG_TAG, "No specific track specified. Loading most current one.");
                mTrack = storageHelper.loadTrack(FILE_MOST_CURRENT_TRACK);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            // display track on map
            displayTrack();
        }
    }
    /**
     * End of inner class
     */

}