/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view end inflates a menu bar menu
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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
    private boolean mTrackerServiceRunning;
    private boolean mPermissionsGranted;
    private List<String> mMissingPermissions;
    private View mRootView;
    private FloatingActionButton mFloatingActionButton;
    private MainActivityFragment mMainActivityFragment;
    private BroadcastReceiver mTrackingStoppedReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set state of tracking
        mTrackerServiceRunning = false;
        if (savedInstanceState != null) {
            mTrackerServiceRunning = savedInstanceState.getBoolean(INSTANCE_TRACKING_STATE, false);
        }

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

        // register broadcast receiver for stopped tracking
        mTrackingStoppedReceiver = createTrackingStoppedReceiver();
        IntentFilter trackingStoppedIntentFilter = new IntentFilter(ACTION_TRACKING_STOPPED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mTrackingStoppedReceiver, trackingStoppedIntentFilter);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate action bar options menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // handle action bar options menu selection
        switch (item.getItemId()) {

            // CASE ABOUT
            case R.id.action_bar_about:
                // get title
                String aboutTitle = getString(R.string.header_about);
                // put title and content into intent and start activity
                Intent aboutIntent = new Intent(this, InfosheetActivity.class);
                aboutIntent.putExtra(EXTRA_INFOSHEET_TITLE, aboutTitle);
                aboutIntent.putExtra(EXTRA_INFOSHEET_CONTENT, INFOSHEET_CONTENT_ABOUT);
                startActivity(aboutIntent);
                return true;

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(INSTANCE_TRACKING_STATE, mTrackerServiceRunning);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_TRACKING_STATE)) {
            mTrackerServiceRunning = intent.getBooleanExtra(EXTRA_TRACKING_STATE, false);
            mMainActivityFragment.setTrackingState(mTrackerServiceRunning);
        }

        // if not in onboarding mode: set state of FloatingActionButton
        if (mFloatingActionButton != null) {
            setFloatingActionButtonState();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        // disable  broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mTrackingStoppedReceiver);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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


    /* Set up main layout */
    private void setupLayout() {
        if (mPermissionsGranted) {
            // point to the main map layout
            setContentView(R.layout.activity_main);

            // show action bar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // get reference to fragment
            mMainActivityFragment = (MainActivityFragment)getSupportFragmentManager().findFragmentById(R.id.content_main);

            // show the record button and attach listener
            mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
            mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handleFloatingActionButtonClick(view);
                }
            });

        } else {
            // point to the on main onboarding layout
            setContentView(R.layout.onboarding_main);

            // show the okay button and attach listener
            Button okButton = (Button) findViewById(R.id.button_okay);
            okButton.setOnClickListener(new View.OnClickListener() {
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


    /* Handles tap on the record button */
    private void handleFloatingActionButtonClick(View view) {
        if (mTrackerServiceRunning) {
            // show snackbar
            Snackbar.make(view, R.string.snackbar_message_tracking_stopped, Snackbar.LENGTH_SHORT).setAction("Action", null).show();

            // change state
            // --> is handled by broadcast receiver

            // stop tracker service
            Intent intent = new Intent(this, TrackerService.class);
            intent.setAction(ACTION_STOP);
            startService(intent);

        } else {
            // show snackbar
            Snackbar.make(view, R.string.snackbar_message_tracking_started, Snackbar.LENGTH_SHORT).setAction("Action", null).show();

            // change state
            mTrackerServiceRunning = true;
            setFloatingActionButtonState();

            // get last location from MainActivity Fragment
            Location lastLocation = mMainActivityFragment.getCurrentBestLocation();

            // start tracker service
            Intent intent = new Intent(this, TrackerService.class);
            intent.setAction(ACTION_START);
            intent.putExtra(EXTRA_LAST_LOCATION, lastLocation);
            startService(intent);
        }

        // update tracking state in MainActivityFragment
        mMainActivityFragment.setTrackingState(mTrackerServiceRunning);
    }


    /* Set state of FloatingActionButton */
    private void setFloatingActionButtonState() {
        if (mTrackerServiceRunning) {
            mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp);
        } else {
            mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
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
                setFloatingActionButtonState();

                // pass tracking state to MainActivityFragment
                mMainActivityFragment.setTrackingState(false);
            }
        };
    }

}
