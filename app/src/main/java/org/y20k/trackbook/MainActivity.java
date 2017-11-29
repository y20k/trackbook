/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view
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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.y20k.trackbook.helpers.DialogHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

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
    private BottomNavigationView mBottomNavigationView;
    private boolean mTrackerServiceRunning;
    private boolean mPermissionsGranted;
    private boolean mFloatingActionButtonSubMenuVisible;
    private List<String> mMissingPermissions;
    private FloatingActionButton mFloatingActionButtonMain;
    private FloatingActionButton mFloatingActionButtonSubSave;
    private FloatingActionButton mFloatingActionButtonSubClear;
    private FloatingActionButton mFloatingActionButtonLocation;
    private CardView mFloatingActionButtonSubSaveLabel;
    private CardView mFloatingActionButtonSubClearLabel;
    private BroadcastReceiver mTrackingStoppedReceiver;
    private int mFloatingActionButtonState;
    private int mSelectedTab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check state of External Storage
        checkExternalStorageState();

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

        // set user agent to prevent getting banned from the osm servers
        org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants.setUserAgentValue(BuildConfig.APPLICATION_ID);

        // set up main layout
        setupLayout();

    }


    @Override
    protected void onStart() {
        super.onStart();

        // register broadcast receiver for stopped tracking
        mTrackingStoppedReceiver = createTrackingStoppedReceiver();
        IntentFilter trackingStoppedIntentFilter = new IntentFilter(ACTION_TRACKING_STOPPED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mTrackingStoppedReceiver, trackingStoppedIntentFilter);

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

//        // save state of Floating Action Button
//        saveFloatingActionButtonState(this);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.v(LOG_TAG, "onDestroy called.");

        // reset selected tab
        mSelectedTab = FRAGMENT_ID_MAP;

        // disable  broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTrackingStoppedReceiver);
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
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

                if (location && storage) {
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


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTrackerServiceRunning = savedInstanceState.getBoolean(INSTANCE_TRACKING_STATE, false);
        mSelectedTab = savedInstanceState.getInt(INSTANCE_SELECTED_TAB, 0);
        mFloatingActionButtonSubMenuVisible = savedInstanceState.getBoolean(INSTANCE_FAB_SUB_MENU_VISIBLE, false);
    }


    /* Handles FloatingActionButton dialog results */
    public void onFloatingActionButtonResult(int requestCode, int resultCode) {
        switch(requestCode) {
            case RESULT_SAVE_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    // user chose SAVE
                    handleStateAfterSave();
                } else if (resultCode == Activity.RESULT_CANCELED){
                    LogHelper.v(LOG_TAG, "Save dialog result: CANCEL");
                }
                break;
            case RESULT_CLEAR_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    // user chose CLEAR
                    handleStateAfterClear();
                } else if (resultCode == Activity.RESULT_CANCELED){
                    LogHelper.v(LOG_TAG, "Clear map: User chose CANCEL.");
                }
                break;
        }
    }


    /* Handles the visual state after a save action */
    private void handleStateAfterSave() {
        // display and update track tab
        mSelectedTab = FRAGMENT_ID_TRACKS;

        // dismiss notification
        Intent intent = new Intent(this, TrackerService.class);
        intent.setAction(ACTION_DISMISS);
        startService(intent);

        // hide Floating Action Button sub menu
        showFloatingActionButtonMenu(false);

        // update Floating Action Button icon
        mFloatingActionButtonState = FAB_STATE_DEFAULT;
        setFloatingActionButtonState();
    }


    /* Handles the visual state after a save action */
    private void handleStateAfterClear() {
        // notify user
        Toast.makeText(this, getString(R.string.toast_message_track_clear), Toast.LENGTH_LONG).show();

        // dismiss notification
        Intent intent = new Intent(this, TrackerService.class);
        intent.setAction(ACTION_DISMISS);
        startService(intent);

        // hide Floating Action Button sub menu
        showFloatingActionButtonMenu(false);

        // update Floating Action Button icon
        mFloatingActionButtonState = FAB_STATE_DEFAULT;
        setFloatingActionButtonState();
    }


    /* Loads state of Floating Action Button from preferences */
    private void loadFloatingActionButtonState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mFloatingActionButtonState = settings.getInt(PREFS_FAB_STATE, FAB_STATE_DEFAULT);
    }


//    /* Saves state of Floating Action Button */
//    private void saveFloatingActionButtonState(Context context) {
//        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putInt(PREFS_FAB_STATE, mFloatingActionButtonState);
//        editor.apply();
//    }


    /* Set up main layout */
    private void setupLayout() {
        if (mPermissionsGranted) {
            // point to the main map layout
            setContentView(R.layout.activity_main);

            // setup bottom navigation
            mBottomNavigationView = findViewById(R.id.navigation);
            mBottomNavigationView.setOnNavigationItemSelectedListener(getOnNavigationItemSelectedListener());

            // get references to the record button and show/hide its sub menu
            mFloatingActionButtonMain = findViewById(R.id.fabMainButton);
            mFloatingActionButtonLocation = findViewById(R.id.fabLLcationButton);
            mFloatingActionButtonSubSave = findViewById(R.id.fabSubMenuButtonSave);
            mFloatingActionButtonSubSaveLabel = findViewById(R.id.fabSubMenuLabelSave);
            mFloatingActionButtonSubClear = findViewById(R.id.fabSubMenuButtonClear);
            mFloatingActionButtonSubClearLabel = findViewById(R.id.fabSubMenuLabelClear);
            if (mFloatingActionButtonSubMenuVisible) {
                showFloatingActionButtonMenu(true);
            } else {
                showFloatingActionButtonMenu(false);
            }

            // add listeners to buttons
            addListenersToViews();

            // show map fragment
            showFragment(FRAGMENT_ID_MAP);

        } else {
            // point to the on main onboarding layout
            setContentView(R.layout.activity_main_onboarding);

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
        mFloatingActionButtonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) getFragmentFromTag(FRAGMENT_TAG_MAP);
                mainActivityMapFragment.handleShowMyLocation();
            }
        });
    }


    /* Handles tap on the button "save and clear" */
    private void handleSaveButtonClick() {
        // todo check -> may produce NullPointerException
        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) getFragmentFromTag(FRAGMENT_TAG_MAP);
        mainActivityMapFragment.onActivityResult(RESULT_SAVE_DIALOG, Activity.RESULT_OK, getIntent());
        handleStateAfterSave();
    }


    /* Handles tap on the button "clear" */
    private void handleClearButtonClick() {
        int dialogTitle = -1;
        String dialogMessage = getString(R.string.dialog_clear_content);
        int dialogPositiveButton = R.string.dialog_clear_action_clear;
        int dialogNegativeButton = R.string.dialog_default_action_cancel;

        // show delete dialog - results are handles by onActivityResult
        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) getFragmentFromTag(FRAGMENT_TAG_MAP);
        DialogFragment dialogFragment = DialogHelper.newInstance(dialogTitle, dialogMessage, dialogPositiveButton, dialogNegativeButton);
        dialogFragment.setTargetFragment(mainActivityMapFragment, RESULT_CLEAR_DIALOG);
        dialogFragment.show(getSupportFragmentManager(), "ClearDialog");
    }


    /* Handles tap on the record button */
    private void handleFloatingActionButtonClick(View view) {

        switch (mFloatingActionButtonState) {
            case FAB_STATE_DEFAULT:
                // show snackbar
                Snackbar.make(view, R.string.snackbar_message_tracking_started, Snackbar.LENGTH_SHORT).setAction("Action", null).show();

                // change state
                mTrackerServiceRunning = true;
                mFloatingActionButtonState = FAB_STATE_RECORDING;
                setFloatingActionButtonState();

                // get last location from MainActivity Fragment // todo check -> may produce NullPointerException
                MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) getFragmentFromTag(FRAGMENT_TAG_MAP);
                Location lastLocation = mainActivityMapFragment.getCurrentBestLocation();

                if (lastLocation != null) {
                    // start tracker service
                    Intent intent = new Intent(this, TrackerService.class);
                    intent.setAction(ACTION_START);
                    intent.putExtra(EXTRA_LAST_LOCATION, lastLocation);
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }

                } else {
                    Toast.makeText(this, getString(R.string.toast_message_location_services_not_ready), Toast.LENGTH_LONG).show();
                    // change state back
                    mTrackerServiceRunning = false;
                    setFloatingActionButtonState();
                }

                break;

            case FAB_STATE_RECORDING:
                // show snackbar
                Snackbar.make(view, R.string.snackbar_message_tracking_stopped, Snackbar.LENGTH_SHORT).setAction("Action", null).show();

                // change state
                // --> is handled by broadcast receiver

                // stop tracker service
                Intent intent = new Intent(this, TrackerService.class);
                intent.setAction(ACTION_STOP);
                startService(intent);

                break;

            case FAB_STATE_SAVE:
                // toggle floating action button sub menu
                if (!mFloatingActionButtonSubMenuVisible) {
                    showFloatingActionButtonMenu(true);
                } else {
                    showFloatingActionButtonMenu(false);
                }

                break;

        }

        // update tracking state in MainActivityMapFragment // todo check -> may produce NullPointerException
        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) getFragmentFromTag(FRAGMENT_TAG_MAP);
        mainActivityMapFragment.setTrackingState(mTrackerServiceRunning);
    }



//    /* Handles tap on the save and clear button */
//    private void handleButtonSaveAndClearClick() {
//        // clear map and save track
//        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
//        mainActivityMapFragment.clearMap(true);
//
//        // display and update track tab
//        mSelectedTab = FRAGMENT_ID_TRACKS;
//        mViewPager.setCurrentItem(mSelectedTab);
//
//        // dismiss notification
//        NotificationHelper.stop();
//
//        // hide Floating Action Button sub menu
//        showFloatingActionButtonMenu(false);
//
//        // update Floating Action Button icon
//        mFloatingActionButtonState = FAB_STATE_DEFAULT;
//        setFloatingActionButtonState();
//    }


//    /* Handles tap on the clear button */
//    private void handleButtonClearClick() {
//        // clear map, do not save track
//        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
//        mainActivityMapFragment.clearMap(false);
//
//        // dismiss notification
//        NotificationHelper.stop();
//
//        // hide Floating Action Button sub menu
//        showFloatingActionButtonMenu(false);
//
//        // update Floating Action Button icon
//        mFloatingActionButtonState = FAB_STATE_DEFAULT;
//        setFloatingActionButtonState();
//
//        Toast.makeText(this, getString(R.string.toast_message_track_clear), Toast.LENGTH_LONG).show();
//    }


    /* Set state of FloatingActionButton */
    private void setFloatingActionButtonState() {

        switch (mFloatingActionButtonState) {
            case FAB_STATE_DEFAULT:
                mFloatingActionButtonMain.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
                break;
            case FAB_STATE_RECORDING:
                mFloatingActionButtonMain.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp);
                break;
            case FAB_STATE_SAVE:
                mFloatingActionButtonMain.setImageResource(R.drawable.ic_save_white_24dp);
                break;
            default:
                mFloatingActionButtonMain.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
                break;
        }
    }


    /* Shows (and hides) the sub menu of the floating action button */
    private void showFloatingActionButtonMenu(boolean visible) {
        if (visible) {
            mFloatingActionButtonSubClear.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubClearLabel.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubSave.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubSaveLabel.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubMenuVisible = true;
        } else {
            mFloatingActionButtonSubClear.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubClearLabel.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubSaveLabel.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubSave.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubMenuVisible = false;
        }
    }


    /* Show fragment for given position */
    private void showFragment(int pos) {
        Fragment fragment = null;
        String tag = null;

        // define tag
        if (pos == FRAGMENT_ID_TRACKS) {
            tag = FRAGMENT_TAG_TRACKS;
        } else {
            tag = FRAGMENT_TAG_MAP;
        }
        // get fragment
        fragment = getFragmentFromTag(tag);

        // update selected tab
        mSelectedTab = pos;

        // place fragment in container
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment, tag )
                .commit();
    }


    /* Returns a fragment for a given tag - creates a new instance if necessary */
    private Fragment getFragmentFromTag(String tag) {
        Fragment fragment = null;
        fragment = getSupportFragmentManager().findFragmentByTag(tag);

        if (fragment != null) {
            return fragment;
        } else {
            if (tag.equals(FRAGMENT_TAG_TRACKS)) {
                fragment = MainActivityTrackFragment.newInstance();
            } else {
                fragment = MainActivityMapFragment.newInstance();
            }
            return fragment;
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
                        showFragment(FRAGMENT_ID_MAP);

                        return true;

                    case R.id.navigation_last_tracks:
                        // hide the Floating Action Button - and its sub menu
                        mFloatingActionButtonMain.hide();
                        showFloatingActionButtonMenu(false);

                        // hide the my location button
                        mFloatingActionButtonLocation.hide();

                        // show tracks fragment
                        mSelectedTab = FRAGMENT_ID_TRACKS;
                        showFragment(FRAGMENT_ID_TRACKS);

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
                mSelectedTab = FRAGMENT_ID_MAP;
                mBottomNavigationView.setSelectedItemId(FRAGMENT_ID_MAP);

                // clear intent
                intent.setAction(ACTION_DEFAULT);

                break;

            default:
                break;
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

        // check for storage permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // add missing permission
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        return permissions;
    }


    /* Creates receiver for stopped tracking */
    private BroadcastReceiver createTrackingStoppedReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // change state
                mTrackerServiceRunning = false;
                mFloatingActionButtonState = FAB_STATE_SAVE;
                setFloatingActionButtonState();

                // pass tracking state to MainActivityMapFragment // todo check -> may produce NullPointerException
                MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) getFragmentFromTag(FRAGMENT_TAG_MAP);
                mainActivityMapFragment.setTrackingState(false);
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

}
