/**
 * MainActivity.java
 * Implements the app's main activity
 * The main activity sets up the main view end inflates a menu bar menu
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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.y20k.trackbook.helpers.DialogHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.NotificationHelper;
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
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private NonSwipeableViewPager mViewPager;
    private boolean mTrackerServiceRunning;
    private boolean mPermissionsGranted;
    private boolean mFloatingActionButtonSubMenuVisible;
    private List<String> mMissingPermissions;
    private FloatingActionButton mFloatingActionButton;
    private LinearLayout mFloatingActionButtonSubMenu1;
    private LinearLayout mFloatingActionButtonSubMenu2;
    private BroadcastReceiver mTrackingStoppedReceiver;
    private int mFloatingActionButtonState;
    private int mSelectedTab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

            // CASE MY LOCATION
            case R.id.action_bar_my_location:
                if (mSelectedTab != FRAGMENT_ID_MAP) {
                    // show map fragment
                    mSelectedTab = FRAGMENT_ID_MAP;
                    mViewPager.setCurrentItem(mSelectedTab);
                }
                return false;

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // load state of Floating Action Button
        loadFloatingActionButtonState(this);

        // handle incoming intent (from notification)
        handleIncomingIntent();

        // if not in onboarding mode: set state of FloatingActionButton
        if (mFloatingActionButton != null) {
            setFloatingActionButtonState();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();

        // save state of Floating Action Button
        saveFloatingActionButtonState(this);
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
        mSelectedTab = FRAGMENT_ID_TRACK;
        mViewPager.setCurrentItem(mSelectedTab);

        // dismiss notification
        NotificationHelper.stop();

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
        NotificationHelper.stop();

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


    /* Saves state of Floating Action Button */
    private void saveFloatingActionButtonState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREFS_FAB_STATE, mFloatingActionButtonState);
        editor.apply();
    }


    /* Set up main layout */
    private void setupLayout() {
        if (mPermissionsGranted) {
            // point to the main map layout
            setContentView(R.layout.activity_main);

            // show action bar
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // create adapter that returns fragments for the maim map and the last track display
            mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

            // Set up the ViewPager with the sections adapter.
            mViewPager = (NonSwipeableViewPager) findViewById(R.id.container);
            mViewPager.setAdapter(mSectionsPagerAdapter);
            mViewPager.setCurrentItem(mSelectedTab);

            TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(mViewPager);
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    switch (tab.getPosition()) {
                        case FRAGMENT_ID_MAP:
                            // show the Floating Action Button
                            mFloatingActionButton.show();
                            mSelectedTab = FRAGMENT_ID_MAP;
                            break;
                        case FRAGMENT_ID_TRACK:
                            // hide the Floating Action Button - and its sub menu
                            mFloatingActionButton.hide();
                            showFloatingActionButtonMenu(false);
                            mSelectedTab = FRAGMENT_ID_TRACK;
                            break;
                        default:
                            // show the Floating Action Button
                            mFloatingActionButton.show();
                            break;
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });

            // get references to the record button and show/hide its sub menu
            mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fabMainButton);
            mFloatingActionButtonSubMenu1 = (LinearLayout) findViewById(R.id.fabSubMenu1);
            mFloatingActionButtonSubMenu2 = (LinearLayout) findViewById(R.id.fabSubMenu2);
            if (mFloatingActionButtonSubMenuVisible) {
                showFloatingActionButtonMenu(true);
            } else {
                showFloatingActionButtonMenu(false);
            }

            // add listeners to button and submenu
            if (mFloatingActionButton != null) {
                mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        handleFloatingActionButtonClick(view);
                    }
                });
            }
            if (mFloatingActionButtonSubMenu1 != null) {
                mFloatingActionButtonSubMenu1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
                        mainActivityMapFragment.onActivityResult(RESULT_SAVE_DIALOG, Activity.RESULT_OK, getIntent());
                        handleStateAfterSave();
                    }
                });
            }
            if (mFloatingActionButtonSubMenu2 != null) {
                mFloatingActionButtonSubMenu2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int dialogTitle = -1;
                        String dialogMessage = getString(R.string.dialog_clear_content);
                        int dialogPositiveButton = R.string.dialog_clear_action_clear;
                        int dialogNegativeButton = R.string.dialog_default_action_cancel;

                        // show delete dialog - results are handles by onActivityResult
                        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
                        DialogFragment dialogFragment = DialogHelper.newInstance(dialogTitle, dialogMessage, dialogPositiveButton, dialogNegativeButton);
                        dialogFragment.setTargetFragment(mainActivityMapFragment, RESULT_CLEAR_DIALOG);
                        dialogFragment.show(getSupportFragmentManager(), "ClearDialog");
                    }
                });
            }

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

                // get last location from MainActivity Fragment
                MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
                Location lastLocation = mainActivityMapFragment.getCurrentBestLocation();

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

        // update tracking state in MainActivityMapFragment
        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
        mainActivityMapFragment.setTrackingState(mTrackerServiceRunning);
    }


//    /* Handles tap on the save and clear button */
//    private void handleButtonSaveAndClearClick() {
//        // clear map and save track
//        MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
//        mainActivityMapFragment.clearMap(true);
//
//        // display and update track tab
//        mSelectedTab = FRAGMENT_ID_TRACK;
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
                mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
                break;
            case FAB_STATE_RECORDING:
                mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_red_24dp);
                break;
            case FAB_STATE_SAVE:
                mFloatingActionButton.setImageResource(R.drawable.ic_save_white_24dp);
                break;
            default:
                mFloatingActionButton.setImageResource(R.drawable.ic_fiber_manual_record_white_24dp);
                break;
        }
    }


    /* Shows (and hides) the sub menu of the floating action button */
    private void showFloatingActionButtonMenu(boolean visible) {
        if (visible) {
            mFloatingActionButtonSubMenu1.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubMenu2.setVisibility(View.VISIBLE);
            mFloatingActionButtonSubMenuVisible = true;
        } else {
            mFloatingActionButtonSubMenu1.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubMenu2.setVisibility(View.INVISIBLE);
            mFloatingActionButtonSubMenuVisible = false;
        }

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
                mViewPager.setCurrentItem(mSelectedTab);

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

                // pass tracking state to MainActivityMapFragment
                MainActivityMapFragment mainActivityMapFragment = (MainActivityMapFragment) mSectionsPagerAdapter.getFragment(FRAGMENT_ID_MAP);
                mainActivityMapFragment.setTrackingState(false);
            }
        };
    }


    /**
     * Inner class: SectionsPagerAdapter that returns a fragment corresponding to one of the tabs.
     * see also: https://developer.android.com/reference/android/support/v4/app/FragmentPagerAdapter.html
     * and: http://www.truiton.com/2015/12/android-activity-fragment-communication/
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

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
                case FRAGMENT_ID_TRACK:
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
                case FRAGMENT_ID_MAP:
                    return getString(R.string.tab_map);
                case FRAGMENT_ID_TRACK:
                    return getString(R.string.tab_last_tracks);
            }
            return null;
        }

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
