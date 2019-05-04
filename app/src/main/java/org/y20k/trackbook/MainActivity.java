/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view
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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.osmdroid.config.Configuration;
import org.y20k.trackbook.helpers.DialogHelper;
import org.y20k.trackbook.helpers.ExportHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.NightModeHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;
import org.y20k.trackbook.layout.NonSwipeableViewPager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * MainActivity class
 */
public class MainActivity extends AppCompatActivity implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    /* Main class variables */
    private TrackerService mTrackerService;
    private BottomNavigationView mBottomNavigationView;
    private NonSwipeableViewPager mViewPager;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private boolean mTrackerServiceRunning;
    private boolean mPermissionsGranted;
    private boolean mFloatingActionButtonSubMenuVisible;
    private List<String> mMissingPermissions;
    private FloatingActionButton mFloatingActionButtonMain;
    private FloatingActionButton mFloatingActionButtonSubSave;
    private FloatingActionButton mFloatingActionButtonSubClear;
    private FloatingActionButton mFloatingActionButtonSubResume;
    private FloatingActionButton mFloatingActionButtonLocation;
    private CardView mFloatingActionButtonSubSaveLabel;
    private CardView mFloatingActionButtonSubClearLabel;
    private CardView mFloatingActionButtonSubResumeLabel;
    private BroadcastReceiver mTrackingChangedReceiver;
    private int mFloatingActionButtonState;
    private int mSelectedTab;

    private boolean mBound = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check state of External Storage
        checkExternalStorageState();

        // empty cache
        ExportHelper.emptyCacheDirectory(this);

        // load saved state of app
        loadFloatingActionButtonState(this);

        // check permissions on Android 6 and higher
        mPermissionsGranted = false;
        if (Build.VERSION.SDK_INT >= 23) {
            // check permissions
            mMissingPermissions = checkPermissions();
            mPermissionsGranted = mMissingPermissions.size() == 0;
        } else {
            mPermissionsGranted = true;
        }

        // initialize state
        if (savedInstanceState != null) {
            // restore if saved instance is available
            mTrackerServiceRunning = savedInstanceState.getBoolean(INSTANCE_TRACKING_STATE, false);
            mSelectedTab = savedInstanceState.getInt(INSTANCE_SELECTED_TAB, FRAGMENT_ID_MAP);
            mFloatingActionButtonSubMenuVisible = savedInstanceState.getBoolean(INSTANCE_FAB_SUB_MENU_VISIBLE, false);
        } else {
            // use default values
            mTrackerServiceRunning = false;
            mSelectedTab = FRAGMENT_ID_MAP;
            mFloatingActionButtonSubMenuVisible = false;
        }

        // set user agent to prevent getting banned from the osm servers
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        // set the path for osmdroid's files (e.g. tile cache)
        Configuration.getInstance().setOsmdroidBasePath(this.getExternalFilesDir(null));

        // set up main layout
        setupLayout();
    }


    @Override
    protected void onStart() {
        super.onStart();

        // bind to TrackerService
        Intent intent = new Intent(this, TrackerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // register broadcast receiver for changed tracking state
        mTrackingChangedReceiver = createTrackingChangedReceiver();
        IntentFilter trackingStoppedIntentFilter = new IntentFilter(ACTION_TRACKING_STATE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mTrackingChangedReceiver, trackingStoppedIntentFilter);
    }


    @Override
    protected void onResume() {
        super.onResume();

        // load state of Floating Action Button
        loadFloatingActionButtonState(this);

        // handle incoming intent (from notification)
        handleIncomingIntent();

        // if not in onboarding mode: set state of FloatingActionButton
        if (mFloatingActionButtonMain != null) {
            setFloatingActionButtonState();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

    }


    @Override
    protected void onStop() {
        super.onStop();
        // unbind from TrackerService
        unbindService(mConnection);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.v(LOG_TAG, "onDestroy called.");

        // reset selected tab
        mSelectedTab = FRAGMENT_ID_MAP;

        // disable  broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTrackingChangedReceiver);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:	{
                Map<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);

                // check for ACCESS_FINE_LOCATION and WRITE_EXTERNAL_STORAGE
                Boolean location = perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                // Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

                if (location) {
                    // permissions granted - notify user
                    Toast.makeText(this, R.string.toast_message_permissions_granted, Toast.LENGTH_SHORT).show();
                    mPermissionsGranted = true;
                    // switch to main map layout
                    setupLayout();
                } else {
                    // permissions denied - notify user
                    Toast.makeText(this, R.string.toast_message_unable_to_start_app, Toast.LENGTH_SHORT).show();
                    mPermissionsGranted = false;
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(INSTANCE_TRACKING_STATE, mTrackerServiceRunning);
        outState.putInt(INSTANCE_SELECTED_TAB, mSelectedTab);
        outState.putBoolean(INSTANCE_FAB_SUB_MENU_VISIBLE, mFloatingActionButtonSubMenuVisible);
        super.onSaveInstanceState(outState);
    }


    /* Handles FloatingActionButton dialog results - called by MainActivityMapFragment after Saving and/or clearing the map */
    public void onFloatingActionButtonResult(int requestCode, int resultCode) {
        switch(requestCode) {
            case RESULT_SAVE_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    // user chose SAVE
                    handleStateAfterSave();
                    LogHelper.v(LOG_TAG, "Save dialog result: SAVE");
                } else if (resultCode == Activity.RESULT_CANCELED){
                    LogHelper.v(LOG_TAG, "Save dialog result: CANCEL");
                }
                break;
            case RESULT_CLEAR_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    // user chose CLEAR
                    handleStateAfterClear();
                    LogHelper.v(LOG_TAG, "Clear map dialog result: CLEAR");
                } else if (resultCode == Activity.RESULT_CANCELED){
                    LogHelper.v(LOG_TAG, "Clear map dialog result: User chose CANCEL.");
                }
                break;
            case RESULT_EMPTY_RECORDING_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    // User chose RESUME RECORDING
                    handleResumeButtonClick((View)mFloatingActionButtonMain);
                    LogHelper.v(LOG_TAG, "Empty recording dialog result: RESUME");
                } else if (resultCode == Activity.RESULT_CANCELED){
                    // User chose CANCEL - do nothing just hide the sub menu
                    showFloatingActionButtonMenu(false);
                    LogHelper.v(LOG_TAG, "Empty recording dialog result: CANCEL");
                }
                break;
        }
    }


    /* Handles the visual state after a save action */
    private void handleStateAfterSave() {
        // display and update tracks tab
        mBottomNavigationView.setSelectedItemId(R.id.navigation_last_tracks);

        // dismiss notification
        dismissNotification();

        // hide Floating Action Button sub menu
        showFloatingActionButtonMenu(false);

        // update Floating Action Button icon
        mFloatingActionButtonState = FAB_STATE_DEFAULT;
        setFloatingActionButtonState();
    }


    /* Start tracker service */
    private void startTrackerService() {
        // start service so that it keeps running after unbind
        Intent intent = new Intent(this, TrackerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ... start service in foreground to prevent it being killed on Oreo
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }


    /* Start recording movements */
    private void startRecording(Location lastLocation) {
        startTrackerService();
        if (mBound) {
            mTrackerService.startTracking(lastLocation);
        }
    }


    /* Resume recording movements */
    private void resumeRecording(Location lastLocation) {
        startTrackerService();
        if (mBound) {
            mTrackerService.resumeTracking(lastLocation);
        }
    }


    /* Stop recording movements */
    private void stopRecording() {
        if (mBound) {
            mTrackerService.stopTracking();
        }
    }


    /* Dismiss notification */
    private void dismissNotification() {
        if (mBound) {
            mTrackerService.dismissNotification();
        }
    }


    /* Handles the visual state after a save action */
    private void handleStateAfterClear() {
        // dismiss notification
        dismissNotification();

        // hide Floating Action Button sub menu
        showFloatingActionButtonMenu(false);

        // update Floating Action Button icon
        mFloatingActionButtonState = FAB_STATE_DEFAULT;
        setFloatingActionButtonState();
    }


    /* Handles tap on the button "save" */
    private void handleSaveButtonClick() {
        // save button click is handled by onActivityResult in MainActivityMapFragment
        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
        mainActivityMapFragment.onActivityResult(RESULT_SAVE_DIALOG, Activity.RESULT_OK, getIntent());
    }


    /* Handles tap on the button "clear" */
    private void handleClearButtonClick() {
        // prepare delete dialog
        int dialogTitle = -1;
        String dialogMessage = getString(R.string.dialog_clear_content);
        int dialogPositiveButton = R.string.dialog_clear_action_clear;
        int dialogNegativeButton = R.string.dialog_default_action_cancel;
        // show delete dialog
        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
        DialogFragment dialogFragment = DialogHelper.newInstance(dialogTitle, dialogMessage, dialogPositiveButton, dialogNegativeButton);
        dialogFragment.setTargetFragment(mainActivityMapFragment, RESULT_CLEAR_DIALOG);
        dialogFragment.show(getSupportFragmentManager(), "ClearDialog");
        // results of dialog are handled by onActivityResult in MainActivityMapFragment
    }


    /* Handles tap on the button "resume" */
    private void handleResumeButtonClick(View view) {

        // get last location from MainActivity Fragment // todo check -> may produce NullPointerException
        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
        Location lastLocation = mainActivityMapFragment.getCurrentBestLocation();

        if (lastLocation != null) {
            // show snackbar
            Snackbar.make(view, R.string.snackbar_message_tracking_resumed, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
            // resume tracking
            resumeRecording(lastLocation);
            // hide sub menu
            showFloatingActionButtonMenu(false);
        } else {
            Toast.makeText(this, getString(R.string.toast_message_location_services_not_ready), Toast.LENGTH_LONG).show();
        }
    }


    /* Loads state of Floating Action Button from preferences */
    private void loadFloatingActionButtonState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mFloatingActionButtonState = settings.getInt(PREFS_FAB_STATE, FAB_STATE_DEFAULT);
    }


    /* Set up main layout */
    private void setupLayout() {
        if (mPermissionsGranted) {
            // point to the main map layout
            setContentView(R.layout.activity_main);

            // create adapter that returns fragments for the maim map and the last track display
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

            // set up the ViewPager with the sections adapter.
            mViewPager = (NonSwipeableViewPager) findViewById(R.id.fragmentContainer);
            mViewPager.setAdapter(mSectionsPagerAdapter);

            // setup bottom navigation
            mBottomNavigationView = findViewById(R.id.navigation);
            mBottomNavigationView.setOnNavigationItemSelectedListener(getOnNavigationItemSelectedListener());

            // get references to the record button and show/hide its sub menu
            mFloatingActionButtonMain = findViewById(R.id.fabMainButton);
            mFloatingActionButtonLocation = findViewById(R.id.fabLocationButton);
            mFloatingActionButtonSubSave = findViewById(R.id.fabSubMenuButtonSave);
            mFloatingActionButtonSubSaveLabel = findViewById(R.id.fabSubMenuLabelSave);
            mFloatingActionButtonSubClear = findViewById(R.id.fabSubMenuButtonClear);
            mFloatingActionButtonSubClearLabel = findViewById(R.id.fabSubMenuLabelClear);
            mFloatingActionButtonSubResume = findViewById(R.id.fabSubMenuButtonResume);
            mFloatingActionButtonSubResumeLabel = findViewById(R.id.fabSubMenuLabelResume);
            if (mFloatingActionButtonSubMenuVisible) {
                showFloatingActionButtonMenu(true);
            } else {
                showFloatingActionButtonMenu(false);
            }

            // restore selected tab
            if (mSelectedTab == FRAGMENT_ID_TRACKS) {
                mBottomNavigationView.setSelectedItemId(R.id.navigation_last_tracks);
            } else {
                mBottomNavigationView.setSelectedItemId(R.id.navigation_map);
            }

            // add listeners to buttons
            addListenersToViews();

        } else {
            // point to the on main onboarding layout
            setContentView(R.layout.main_onboarding);

            // show the okay button and attach listener
            Button okayButton = (Button) findViewById(R.id.button_okay);
            okayButton.setOnClickListener(new View.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.M)
                @Override
                public void onClick(View view) {
                    if (mMissingPermissions != null && !mMissingPermissions.isEmpty()) {
                        // request permissions
                        String[] params = mMissingPermissions.toArray(new String[mMissingPermissions.size()]);
                        requestPermissions(params, REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
                    }
                }
            });

        }

    }


    /* Add listeners to ui buttons */
    private void addListenersToViews() {

        mFloatingActionButtonMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleFloatingActionButtonClick(view);
            }
        });
        mFloatingActionButtonSubSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSaveButtonClick();
            }
        });
        mFloatingActionButtonSubSaveLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSaveButtonClick();
            }
        });
        mFloatingActionButtonSubClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleClearButtonClick();
            }
        });
        mFloatingActionButtonSubClearLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleClearButtonClick();
            }
        });
        mFloatingActionButtonSubResume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleResumeButtonClick(view);
            }
        });
        mFloatingActionButtonSubResumeLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleResumeButtonClick(view);
            }
        });


        mFloatingActionButtonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
                mainActivityMapFragment.handleShowMyLocation();
            }
        });

        // secret night mode switch
        mFloatingActionButtonLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NightModeHelper.switchMode(MainActivity.this);
                // vibrate 50 milliseconds
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(50);
                // recreate activity
                recreate();
                return true;
            }
        });
    }


    /* Handles tap on the record button */
    private void handleFloatingActionButtonClick(View view) {

        switch (mFloatingActionButtonState) {
            case FAB_STATE_DEFAULT:

                // get last location from MainActivity Fragment // todo check -> may produce NullPointerException
                MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
                Location lastLocation = mainActivityMapFragment.getCurrentBestLocation();

                if (lastLocation != null) {
                    // show snackbar
                    Snackbar.make(view, R.string.snackbar_message_tracking_started, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                    // start recording
                    startRecording(lastLocation);
                } else {
                    Toast.makeText(this, getString(R.string.toast_message_location_services_not_ready), Toast.LENGTH_LONG).show();
                }

                break;

            case FAB_STATE_RECORDING:
                // show snackbar
                Snackbar.make(view, R.string.snackbar_message_tracking_stopped, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                // stop tracker service
                stopRecording();

                break;

            case FAB_STATE_SAVE:
                // toggle visibility floating action button sub menu
                showFloatingActionButtonMenu(!mFloatingActionButtonSubMenuVisible);

                break;

        }

        // update tracking state in MainActivityMapFragment // todo check -> may produce NullPointerException
        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
        mainActivityMapFragment.setTrackingState(mTrackerServiceRunning);
    }


    /* Set state of FloatingActionButton */
    private void setFloatingActionButtonState() {

        switch (mFloatingActionButtonState) {
            case FAB_STATE_DEFAULT:
                mFloatingActionButtonMain.hide(); // workaround todo remove asap
                mFloatingActionButtonMain.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
                mFloatingActionButtonMain.setContentDescription(getString(R.string.descr_fab_main_start));
                if (mSelectedTab == FRAGMENT_ID_MAP) mFloatingActionButtonMain.show(); // workaround todo remove asap
                break;
            case FAB_STATE_RECORDING:
                mFloatingActionButtonMain.hide(); // workaround todo remove asap
                mFloatingActionButtonMain.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp);
                mFloatingActionButtonMain.setContentDescription(getString(R.string.descr_fab_main_stop));
                if (mSelectedTab == FRAGMENT_ID_MAP) mFloatingActionButtonMain.show(); // workaround todo remove asap
                break;
            case FAB_STATE_SAVE:
                mFloatingActionButtonMain.hide(); // workaround todo remove asap
                mFloatingActionButtonMain.setImageResource(R.drawable.ic_save_white_24dp);
                mFloatingActionButtonMain.setContentDescription(getString(R.string.descr_fab_main_options));
                if (mSelectedTab == FRAGMENT_ID_MAP) mFloatingActionButtonMain.show(); // workaround todo remove asap
                break;
            default:
                mFloatingActionButtonMain.hide(); // workaround todo remove asap
                mFloatingActionButtonMain.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
                mFloatingActionButtonMain.setContentDescription(getString(R.string.descr_fab_main_start));
                if (mSelectedTab == FRAGMENT_ID_MAP) mFloatingActionButtonMain.show(); // workaround todo remove asap
                break;
        }
    }


    /* Shows (and hides) the sub menu of the floating action button */
    private void showFloatingActionButtonMenu(boolean visible) {
        if (visible) {
            mFloatingActionButtonSubResume.show();
            mFloatingActionButtonSubResumeLabel.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubClear.show();
            mFloatingActionButtonSubClearLabel.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubSave.show();
            mFloatingActionButtonSubSaveLabel.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubMenuVisible = true;
        } else {
            mFloatingActionButtonSubResume.hide();
            mFloatingActionButtonSubResumeLabel.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubClear.hide();
            mFloatingActionButtonSubClearLabel.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubSave.hide();
            mFloatingActionButtonSubSaveLabel.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubMenuVisible = false;
        }
    }


    /* Handles taps on the bottom navigation */
    private BottomNavigationView.OnNavigationItemSelectedListener getOnNavigationItemSelectedListener() {
        return new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_map:
                        // show the Floating Action Button
                        mFloatingActionButtonMain.show();

                        // show the my location button
                        mFloatingActionButtonLocation.show();

                        // show map fragment
                        mSelectedTab = FRAGMENT_ID_MAP;
                        mViewPager.setCurrentItem(mSelectedTab);

                        return true;

                    case R.id.navigation_last_tracks:
                        // hide the Floating Action Button - and its sub menu
                        mFloatingActionButtonMain.hide();
                        showFloatingActionButtonMenu(false);

                        // hide the my location button
                        mFloatingActionButtonLocation.hide();

                        // show tracks fragment
                        mSelectedTab = FRAGMENT_ID_TRACKS;
                        mViewPager.setCurrentItem(mSelectedTab);

                        return true;

                    default:
                        // show the Floating Action Button
                        mFloatingActionButtonMain.show();
                        return false;
                }
            }
        };
    }


    /* Handles new incoming intents */
    private void handleIncomingIntent() {
        Intent intent = getIntent();
        LogHelper.v(LOG_TAG, "Main Activity received intent. Content: " + intent.toString());
        String intentAction = intent.getAction();
        switch (intentAction) {
            case ACTION_SHOW_MAP:
                // show map fragment
                mBottomNavigationView.setSelectedItemId(R.id.navigation_map);

                // clear intent
                intent.setAction(ACTION_DEFAULT);

                break;

            case ACTION_CLEAR:
                // show map fragment
                mBottomNavigationView.setSelectedItemId(R.id.navigation_map);

                // show clear dialog
                handleClearButtonClick();

                // clear intent
                intent.setAction(ACTION_DEFAULT);

                break;

            default:
                break;
        }
    }


    /* Inform user and give haptic feedback (vibration) */
    private void longPressFeedback(int stringResource) {
        // inform user
        Toast.makeText(this, stringResource, Toast.LENGTH_LONG).show();
        // vibrate 50 milliseconds
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(50);
//            v.vibrate(VibrationEffect.createOneShot(50, DEFAULT_AMPLITUDE)); // todo check if there is a support library vibrator
        }
    }



    /* Check which permissions have been granted */
    private List<String> checkPermissions() {
        List<String> permissions = new ArrayList<>();

        // check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // add missing permission
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

//        // check for storage permission
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            // add missing permission
//            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        }

        return permissions;
    }


    /* Creates receiver for stopped tracking */
    private BroadcastReceiver createTrackingChangedReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // change state
                mTrackerServiceRunning = intent.getBooleanExtra(EXTRA_TRACKING_STATE, false);
                if (mTrackerServiceRunning) {
                    mFloatingActionButtonState = FAB_STATE_RECORDING;
                } else {
                    mFloatingActionButtonState = FAB_STATE_SAVE;
                }
                setFloatingActionButtonState();

                // pass tracking state to MainActivityMapFragment // todo check -> may produce NullPointerException
                MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
                mainActivityMapFragment.setTrackingState(mTrackerServiceRunning);
            }
        };
    }


    /* Checks the state of External Storage */
    private void checkExternalStorageState() {

        String state = Environment.getExternalStorageState();
        if (!state.equals(Environment.MEDIA_MOUNTED)) {
            LogHelper.e(LOG_TAG, "Error: Unable to mount External Storage. Current state: " + state);

            // move MainActivity to back
            moveTaskToBack(true);

            // shutting down app
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }


//    public class SectionsPagerAdapter extends FragmentPagerAdapter {
//
//        public SectionsPagerAdapter(FragmentManager fm) {
//            super(fm);
//        }
//
//        @Override
//        public Fragment getItem(int position) {
//            // getItem is called to instantiate the fragment for the given page.
//            switch (position) {
//                case FRAGMENT_ID_MAP:
//                    return new MainActivityMapFragment();
//                case FRAGMENT_ID_TRACKS:
//                    return new MainActivityTrackFragment();
//            }
//            return null;
//        }
//
//        @Override
//        public int getCount() {
//            return 2;
//        }
//
//        public Fragment getFragment(int pos) {
//            return getItem(pos);
//        }
//
//    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            TrackerService.LocalBinder binder = (TrackerService.LocalBinder) service;
            mTrackerService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    /**
     * Inner class: SectionsPagerAdapter that returns a fragment corresponding to one of the tabs.
     * see also: https://developer.android.com/reference/android/support/v4/app/FragmentPagerAdapter.html
     * and: http://www.truiton.com/2015/12/android-activity-fragment-communication/
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private final SparseArray<WeakReference<Fragment>> instantiatedFragments = new SparseArray<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case FRAGMENT_ID_MAP:
                    return new MainActivityMapFragment();
                case FRAGMENT_ID_TRACKS:
                    return new MainActivityTrackFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            // show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case FRAGMENT_ID_MAP:
                    return getString(R.string.tab_map);
                case FRAGMENT_ID_TRACKS:
                    return getString(R.string.tab_last_tracks);
            }
            return null;
        }

        @NonNull
        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {
            final Fragment fragment = (Fragment) super.instantiateItem(container, position);
            instantiatedFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(final ViewGroup container, final int position, final Object object) {
            instantiatedFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        @Nullable
        public Fragment getFragment(final int position) {
            final WeakReference<Fragment> wr = instantiatedFragments.get(position);
            if (wr != null) {
                return wr.get();
            } else {
                return null;
            }
        }

    }
    /**
     * End of inner class
     */


}
