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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

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
    private static final String LOG_TAG = MainActivityFragment.class.getSimpleName();


    /* Main class variables */
    private boolean mTracking;
    private boolean mPermissionsGranted;
    private List<String> mMissingPermissions;
    private FloatingActionButton mFloatingActionButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTracking = false;
        mPermissionsGranted = false;

        // check permissions on Android 6 and higher
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate action bar options menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // handle action bar options menu selection
        switch (item.getItemId()) {

            // CASE SETTINGS
            case R.id.action_settings:
                LogHelper.v(LOG_TAG, "Settings was selected");
                return true;

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }
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
        if (mTracking) {
            Snackbar.make(view, R.string.snackbar_message_tracking_stopped, Snackbar.LENGTH_SHORT).setAction("Action", null).show();

            // change state
            mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
            mTracking = false;

            // re-start preliminary tracking
            // startFindingLocation();

            // stop tracker service
            Intent intent = new Intent(this, TrackerService.class);
            intent.setAction(ACTION_STOP);
            startService(intent);
            LogHelper.v(LOG_TAG, "Stopping tracker service.");


        } else {
            Snackbar.make(view, R.string.snackbar_message_tracking_started, Snackbar.LENGTH_SHORT).setAction("Action", null).show();

            // change state
            mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp);
            mTracking = true;

            // TODO putParcelable lastLocation

            // start tracker service
            Intent intent = new Intent(this, TrackerService.class);
            intent.setAction(ACTION_START);
            startService(intent);
            LogHelper.v(LOG_TAG, "Starting tracker service.");
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

}
