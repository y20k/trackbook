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
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.y20k.trackbook.helpers.DialogClearFragment;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.NotificationHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * MainActivity class
 */
public class MainActivity extends AppCompatActivity implements TrackbookKeys, DialogClearFragment.DialogClearListener {

    /* Define log tag */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();


    /* Main class variables */
    private boolean mTrackerServiceRunning;
    private boolean mPermissionsGranted;
    private List<String> mMissingPermissions;
    private FloatingActionButton mFloatingActionButton;
    private MainActivityMapFragment mMainActivityMapFragment;
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
//        setupTestLayout();

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

        // handle new intents - onNewIntent does not seem to work
        handleIncomingIntent();

        // if not in onboarding mode: set state of FloatingActionButton
        if (mFloatingActionButton != null) {
            setFloatingActionButtonState();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.v(LOG_TAG, "onDestroy called.");

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
    public void onDialogClearPositiveClick(DialogFragment dialog) {
        // DialogClear: User chose CLEAR.
        LogHelper.v(LOG_TAG, "User chose CLEAR");

        // clear the map
        mMainActivityMapFragment.clearMap();

        // dismiss notification
        NotificationHelper.stop();
    }


    @Override
    public void onDialogClearNegativeClick(DialogFragment dialog) {
        // DialogClear: User chose CANCEL.
        LogHelper.v(LOG_TAG, "User chose CANCEL.");
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
            mMainActivityMapFragment = (MainActivityMapFragment)getSupportFragmentManager().findFragmentById(R.id.content_main);

            // show the record button and attach listener
            mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
            mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // onClick: start / stop tracking
                    handleFloatingActionButtonClick(view);
                }
            });
            mFloatingActionButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // onLongClick: clear map
                    if (mTrackerServiceRunning || mMainActivityMapFragment == null) {
                        return false;
                    } else {
                        // show clear dialog
                        DialogFragment dialog = new DialogClearFragment();
                        dialog.show(getFragmentManager(), "DialogClearFragment");
                        return true;
                    }
                }
            });

        } else {
            // point to the on main onboarding layout
            setContentView(R.layout.activity_main_onboarding);

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


    /* TEST: Set up main layout */
    private void setupTestLayout() {
        if (mPermissionsGranted) {
            // point to the main map layout
            setContentView(R.layout.activity_main_test);

            // show action bar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            /* BEGIN NEW STUFF */
            // Create the adapter that will return a fragment for each of the three
            // primary sections of the activity.
            SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

            // Set up the ViewPager with the sections adapter.
            ViewPager viewPager = (ViewPager) findViewById(R.id.container);
            viewPager.setAdapter(sectionsPagerAdapter);

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);
            /* END NEW STUFF */

            // get reference to fragment
            mMainActivityMapFragment = (MainActivityMapFragment)getSupportFragmentManager().findFragmentById(R.id.content_main);

            // show the record button and attach listener
            mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
            mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // onClick: start / stop tracking
                    handleFloatingActionButtonClick(view);
                }
            });
            mFloatingActionButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // onLongClick: clear map
                    if (mTrackerServiceRunning || mMainActivityMapFragment == null) {
                        return false;
                    } else {
                        return true;
                    }
                }
            });

        } else {
            // point to the on main onboarding layout
            setContentView(R.layout.activity_main_onboarding);

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
            // TODO ask if user wants to save the last track before starting a new recording
            // TODO alternatively only ask if last track was very short
            // show snackbar
            Snackbar.make(view, R.string.snackbar_message_tracking_started, Snackbar.LENGTH_SHORT).setAction("Action", null).show();

            // change state
            mTrackerServiceRunning = true;
            setFloatingActionButtonState();

            // get last location from MainActivity Fragment
            Location lastLocation = mMainActivityMapFragment.getCurrentBestLocation();

            if (lastLocation != null) {
                // start tracker service
                Intent intent = new Intent(this, TrackerService.class);
                intent.setAction(ACTION_START);
                intent.putExtra(EXTRA_LAST_LOCATION, lastLocation);
                startService(intent);
            } else {
                Toast.makeText(this, getString(R.string.toast_message_location_services_not_ready), Toast.LENGTH_LONG).show();
                // change state back
                mTrackerServiceRunning = false;
                setFloatingActionButtonState();
            }

        }

        // update tracking state in MainActivityMapFragment
        mMainActivityMapFragment.setTrackingState(mTrackerServiceRunning);
    }


    /* Set state of FloatingActionButton */
    private void setFloatingActionButtonState() {
        if (mTrackerServiceRunning) {
            mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp);
        } else {
            mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
        }
    }


    /* Handles new incoming intents */
    private void handleIncomingIntent() {
        Intent intent = getIntent();
        String intentAction = intent.getAction();

        switch (intentAction) {
            case ACTION_SHOW_MAP:
                if (intent.hasExtra(EXTRA_TRACKING_STATE) && mMainActivityMapFragment != null) {
                    mTrackerServiceRunning = intent.getBooleanExtra(EXTRA_TRACKING_STATE, false);
                    mMainActivityMapFragment.setTrackingState(mTrackerServiceRunning);
                    // prevent multiple reactions to intent
                    intent.setAction(ACTION_DEFAULT);
                } else if (intent.hasExtra(EXTRA_CLEAR_MAP) && mMainActivityMapFragment != null) {
                    // show clear dialog
                    DialogFragment dialog = new DialogClearFragment();
                    dialog.show(getFragmentManager(), "DialogClearFragment");
                    // prevent multiple reactions to intent
                    intent.setAction(ACTION_DEFAULT);
                }
                break;

            default:
                // log
                LogHelper.v(LOG_TAG, "Intent received. Doing nothing. Type of ACTION: " +  intentAction);
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
                setFloatingActionButtonState();

                // pass tracking state to MainActivityMapFragment
                mMainActivityMapFragment.setTrackingState(false);
            }
        };
    }



    /**
     * Inner class: SectionsPagerAdapter that returns a fragment corresponding to one of the tabs.
     * see also: https://developer.android.com/reference/android/support/v4/app/FragmentPagerAdapter.html
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
//                    if (mMainActivityMapFragment == null) {
//                        mMainActivityMapFragment = new MainActivityMapFragment();
//                    }
//                    return mMainActivityMapFragment;
                    return new MainActivityMapFragment();
                case 1:
//                    if (mMainActivityMapFragment == null) {
//                        mMainActivityMapFragment = new MainActivityMapFragment();
//                    }
//                    return mMainActivityMapFragment;
                    return new MainActivityTrackFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.tab_map);
                case 1:
                    return getString(R.string.tab_last_track);
            }
            return null;
        }
    }
    /**
     * End of inner class
     */

}
