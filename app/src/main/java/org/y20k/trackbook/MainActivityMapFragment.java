/**
 * MainActivityMapFragment.java
 * Implements the map fragment used in the map tab of the main activity
 * This fragment displays a map using osmdroid
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.y20k.trackbook.core.Track;
import org.y20k.trackbook.helpers.LocationHelper;
import org.y20k.trackbook.helpers.LogHelper;
import org.y20k.trackbook.helpers.MapHelper;
import org.y20k.trackbook.helpers.StorageHelper;
import org.y20k.trackbook.helpers.TrackbookKeys;

import java.util.List;


/**
 * MainActivityMapFragment class
 */
public class MainActivityMapFragment extends Fragment implements TrackbookKeys {

    /* Define log tag */
    private static final String LOG_TAG = MainActivityMapFragment.class.getSimpleName();


    /* Main class variables */
    private Activity mActivity;
    private Track mTrack;
    private boolean mFirstStart;
    private Snackbar mLocationOffBar;
    private BroadcastReceiver mTrackUpdatedReceiver;
    private SettingsContentObserver mSettingsContentObserver;
    private MapView mMapView;
    private IMapController mController;
    private LocationManager mLocationManager;
    private LocationListener mGPSListener;
    private LocationListener mNetworkListener;
    private ItemizedIconOverlay mMyLocationOverlay;
    private ItemizedIconOverlay mTrackOverlay;
    private Location mCurrentBestLocation;
    private boolean mTrackerServiceRunning;
    private boolean mLocalTrackerRunning;
    private boolean mLocationSystemSetting;
    private boolean mFragmentVisible;


    /* Constructor (default) */
    public MainActivityMapFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get activity
        mActivity = getActivity();

        // action bar has options menu
        setHasOptionsMenu(true);

        // restore first start state
        mFirstStart = true;
        if (savedInstanceState != null) {
            mFirstStart = savedInstanceState.getBoolean(INSTANCE_FIRST_START, true);
        }

        // restore tracking state
        mTrackerServiceRunning = false;
        if (savedInstanceState != null) {
            mTrackerServiceRunning = savedInstanceState.getBoolean(INSTANCE_TRACKING_STATE, false);
        }

        // acquire reference to Location Manager
        mLocationManager = (LocationManager) mActivity.getSystemService(Context.LOCATION_SERVICE);

        // CASE 1: get saved location if possible
        if (savedInstanceState != null) {
            Location savedLocation = savedInstanceState.getParcelable(INSTANCE_CURRENT_LOCATION);
            // check if saved location is still current
            if (LocationHelper.isNewLocation(savedLocation)) {
                mCurrentBestLocation = savedLocation;
            } else {
                mCurrentBestLocation = null;
            }
        }

        // CASE 2: get last known location if no saved location or saved location is too old
        if (mCurrentBestLocation == null && mLocationManager.getProviders(true).size() > 0) {
            mCurrentBestLocation = LocationHelper.determineLastKnownLocation(mLocationManager);
        }

        // CASE 3: location services are available but unable to get location - this should not happen
        if (mCurrentBestLocation == null) {
            mCurrentBestLocation = new Location(LocationManager.NETWORK_PROVIDER);
            mCurrentBestLocation.setLatitude(DEFAULT_LATITUDE);
            mCurrentBestLocation.setLongitude(DEFAULT_LONGITUDE);
        }

        // get state of location system setting
        mLocationSystemSetting = LocationHelper.checkLocationSystemSetting(mActivity);

        // create content observer for changes in System Settings
        mSettingsContentObserver = new SettingsContentObserver( new Handler());

        // register broadcast receiver for new WayPoints
        mTrackUpdatedReceiver = createTrackUpdatedReceiver();
        IntentFilter trackUpdatedIntentFilter = new IntentFilter(ACTION_TRACK_UPDATED);
        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mTrackUpdatedReceiver, trackUpdatedIntentFilter);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // create basic map
        mMapView = new MapView(inflater.getContext());

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
        mMapView.getOverlays().add(compassOverlay);

        // initiate map state
        if (savedInstanceState != null) {
            // restore saved instance of map
            GeoPoint position = new GeoPoint(savedInstanceState.getDouble(INSTANCE_LATITUDE_MAIN_MAP, DEFAULT_LATITUDE), savedInstanceState.getDouble(INSTANCE_LONGITUDE_MAIN_MAP, DEFAULT_LONGITUDE));
            mController.setCenter(position);
            mController.setZoom(savedInstanceState.getInt(INSTANCE_ZOOM_LEVEL_MAIN_MAP, 16));
            // restore current location
            mCurrentBestLocation = savedInstanceState.getParcelable(INSTANCE_CURRENT_LOCATION);
        } else if (mCurrentBestLocation != null) {
            // fallback or first run: set map to current position
            GeoPoint position = convertToGeoPoint(mCurrentBestLocation);
            mController.setCenter(position);
            mController.setZoom(16);
        }

        // inform user that new/better location is on its way
        if (mFirstStart && !mTrackerServiceRunning) {
            Toast.makeText(mActivity, mActivity.getString(R.string.toast_message_acquiring_location), Toast.LENGTH_LONG).show();
            mFirstStart = false;
        }

        // restore track
        if (savedInstanceState != null) {
            mTrack = savedInstanceState.getParcelable(INSTANCE_TRACK_MAIN_MAP);
        }
        if (mTrack != null) {
            drawTrackOverlay(mTrack);
        }

        // mark user's location on map
        if (mCurrentBestLocation != null && !mTrackerServiceRunning) {
            mMyLocationOverlay = MapHelper.createMyLocationOverlay(mActivity, mCurrentBestLocation, LocationHelper.isNewLocation(mCurrentBestLocation));
            mMapView.getOverlays().add(mMyLocationOverlay);
        }

        return mMapView;
    }


    @Override
    public void onResume() {
        super.onResume();

        // set visibility
        mFragmentVisible = true;

        // center map on current position - if TrackerService is running
        if (mTrackerServiceRunning) {
            mController.setCenter(convertToGeoPoint(mCurrentBestLocation));
        }

        // draw track on map - if available
        if (mTrack != null) {
            drawTrackOverlay(mTrack);
        }

        // show/hide the location off notification bar
        toggleLocationOffBar();

        // start preliminary tracking - if no TrackerService is running
        if (!mTrackerServiceRunning && mFragmentVisible) {
            startPreliminaryTracking();
        }

        // register content observer for changes in System Settings
        mActivity.getContentResolver().registerContentObserver(android.provider.Settings.Secure.CONTENT_URI, true, mSettingsContentObserver );
    }


    @Override
    public void onPause() {
        super.onPause();

        // set visibility
        mFragmentVisible = false;

        // disable preliminary location listeners
        stopPreliminaryTracking();

        // disable content observer for changes in System Settings
        mActivity.getContentResolver().unregisterContentObserver(mSettingsContentObserver);
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

        // reset first start state
        mFirstStart = true;

        // disable  broadcast receivers
        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mTrackUpdatedReceiver);

        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // handle action bar options menu selection
        switch (item.getItemId()) {

            // CASE MY LOCATION
            case R.id.action_bar_my_location:

                // do nothing if location setting is off
                if (toggleLocationOffBar()) {
                    stopPreliminaryTracking();
                    return false;
                }

                // get current position
                GeoPoint position;

                if (mTrackerServiceRunning) {
                    // get current Location from tracker service
                    mCurrentBestLocation = mTrack.getWayPointLocation(mTrack.getSize());
                } else if (mCurrentBestLocation == null) {
                    // app does not have any location fix
                    mCurrentBestLocation = LocationHelper.determineLastKnownLocation(mLocationManager);
                }

                // check if really got a position
                if (mCurrentBestLocation != null) {
                    position = convertToGeoPoint(mCurrentBestLocation);

                    // center map on current position
                    mController.setCenter(position);

                    // mark user's new location on map and remove last marker
                    updateMyLocationMarker();

                    // inform user about location quality
                    String locationInfo;
                    long locationAge =  (SystemClock.elapsedRealtimeNanos() - mCurrentBestLocation.getElapsedRealtimeNanos()) / 1000000;
                    String locationAgeString = LocationHelper.convertToReadableTime(locationAge, false);
                    if (locationAgeString == null) {
                        locationAgeString = mActivity.getString(R.string.toast_message_last_location_age_one_hour);
                    }
                    locationInfo = " " + locationAgeString + " | " + mCurrentBestLocation.getProvider();
                    Toast.makeText(mActivity, mActivity.getString(R.string.toast_message_last_location) + locationInfo, Toast.LENGTH_LONG).show();
                    return true;
                } else {
                    Toast.makeText(mActivity, mActivity.getString(R.string.toast_message_location_services_not_ready), Toast.LENGTH_LONG).show();
                    return false;
                }

            // CASE DEFAULT
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(INSTANCE_FIRST_START, mFirstStart);
        outState.putParcelable(INSTANCE_CURRENT_LOCATION, mCurrentBestLocation);
        outState.putBoolean(INSTANCE_TRACKING_STATE, mTrackerServiceRunning);
        outState.putDouble(INSTANCE_LATITUDE_MAIN_MAP, mMapView.getMapCenter().getLatitude());
        outState.putDouble(INSTANCE_LONGITUDE_MAIN_MAP, mMapView.getMapCenter().getLongitude());
        outState.putInt(INSTANCE_ZOOM_LEVEL_MAIN_MAP, mMapView.getZoomLevel());
        outState.putParcelable(INSTANCE_TRACK_MAIN_MAP, mTrack);
        super.onSaveInstanceState(outState);
    }


    /* Setter for tracking state */
    public void setTrackingState(boolean trackingState) {
        mTrackerServiceRunning = trackingState;

        // got a new track (from notification)
        Intent intent = mActivity.getIntent();
        if (intent != null && intent.hasExtra(EXTRA_TRACK)) {
            mTrack = intent.getParcelableExtra(EXTRA_TRACK);
        }

        // turn on/off tracking for MainActivity Fragment - prevent double tracking
        if (mTrackerServiceRunning) {
            stopPreliminaryTracking();
        } else if (!mLocalTrackerRunning && mFragmentVisible) {
            startPreliminaryTracking();
        }

        if (mTrack != null) {
            drawTrackOverlay(mTrack);
        }

        // update marker
        updateMyLocationMarker();
        LogHelper.v(LOG_TAG, "TrackingState: " + trackingState);
    }


    /* Getter for current best location */
    public Location getCurrentBestLocation() {
        return mCurrentBestLocation;
    }


    /* Removes track crumbs from map */
    public void clearMap() {

        // clear map
        if (mTrackOverlay != null) {
            Toast.makeText(mActivity, mActivity.getString(R.string.toast_message_clear_map), Toast.LENGTH_LONG).show();
            mMapView.getOverlays().remove(mTrackOverlay);
        }

        // save track object
        SaveTrackAsyncHelper saveTrackAsyncHelper = new SaveTrackAsyncHelper();
        saveTrackAsyncHelper.execute();
        // TODO add toast indicating track save
    }


    /* Start preliminary tracking for map */
    private void startPreliminaryTracking() {
        if (mLocationSystemSetting && !mLocalTrackerRunning) {
            // create location listeners
            List locationProviders = mLocationManager.getAllProviders();
            if (locationProviders.contains(LocationManager.GPS_PROVIDER)) {
                mGPSListener = createLocationListener();
                mLocalTrackerRunning = true;
            }
            if (locationProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                mNetworkListener = createLocationListener();
                mLocalTrackerRunning = true;
            }
            // register listeners
            LocationHelper.registerLocationListeners(mLocationManager, mGPSListener, mNetworkListener);
            LogHelper.v(LOG_TAG, "Starting preliminary tracking.");
        }
    }


    /* Removes gps and network location listeners */
    private void stopPreliminaryTracking() {
        if (mLocalTrackerRunning) {
            mLocalTrackerRunning = false;
            // remove listeners
            LocationHelper.removeLocationListeners(mLocationManager, mGPSListener, mNetworkListener);
            LogHelper.v(LOG_TAG, "Stopping preliminary tracking.");
        }
    }


    /* Creates listener for changes in location status */
    private LocationListener createLocationListener() {
        return new LocationListener() {
            public void onLocationChanged(Location location) {
                // check if the new location is better
                if (LocationHelper.isBetterLocation(location, mCurrentBestLocation)) {
                    // save location
                    mCurrentBestLocation = location;
                    // mark user's new location on map and remove last marker
                    updateMyLocationMarker();
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                LogHelper.v(LOG_TAG, "Location provider status change: " +  provider + " | " + status);
            }

            public void onProviderEnabled(String provider) {
                LogHelper.v(LOG_TAG, "Location provider enabled: " +  provider);
            }

            public void onProviderDisabled(String provider) {
                LogHelper.v(LOG_TAG, "Location provider disabled: " +  provider);
            }
        };
    }


    /* Updates marker for current user location  */
    private void updateMyLocationMarker() {
        mMapView.getOverlays().remove(mMyLocationOverlay);
        // only update while not tracking
        if (!mTrackerServiceRunning) {
            mMyLocationOverlay = MapHelper.createMyLocationOverlay(mActivity, mCurrentBestLocation, LocationHelper.isNewLocation(mCurrentBestLocation));
            mMapView.getOverlays().add(mMyLocationOverlay);
        }
    }


    /* Draws track onto overlay */
    private void drawTrackOverlay(Track track) {
        mMapView.getOverlays().remove(mTrackOverlay);
        mTrackOverlay = MapHelper.createTrackOverlay(mActivity, track, mTrackerServiceRunning);
        mMapView.getOverlays().add(mTrackOverlay);
    }


    /* Toggles snackbar indicating that location setting is off */
    private boolean toggleLocationOffBar() {
        // create snackbar indicator for location setting off
        if (mLocationOffBar == null) {
            mLocationOffBar = Snackbar.make(mMapView, R.string.snackbar_message_location_offline, Snackbar.LENGTH_INDEFINITE).setAction("Action", null);
        }

        // get state of location system setting
        mLocationSystemSetting = LocationHelper.checkLocationSystemSetting(mActivity);

        // show snackbar if necessary
        if (!mLocationSystemSetting)  {
            // show snackbar
            mLocationOffBar.show();
            return true;

        } else {
            // hide snackbar
            mLocationOffBar.dismiss();
            return false;
        }

    }


    /* Creates receiver for new WayPoints */
    private BroadcastReceiver createTrackUpdatedReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.hasExtra(EXTRA_TRACK) && intent.hasExtra(EXTRA_LAST_LOCATION)) {
                    // draw track on map
                    mTrack = intent.getParcelableExtra(EXTRA_TRACK);
                    drawTrackOverlay(mTrack);
                    // center map over last location
                    mCurrentBestLocation = intent.getParcelableExtra(EXTRA_LAST_LOCATION);
                    mController.setCenter(convertToGeoPoint(mCurrentBestLocation));
                    // clear intent
                    intent.setAction(ACTION_DEFAULT);
                }
            }
        };
    }


    /* Converts Location to GeoPoint */
    private GeoPoint convertToGeoPoint (Location location) {
        if (location != null) {
            return new GeoPoint(location.getLatitude(), location.getLongitude());
        } else {
            return new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
        }
    }


    /* Saves state of map */
    private void saveMaoState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(PREFS_ZOOM_LEVEL, mMapView.getZoomLevel());
        editor.apply();
    }


    /* Loads app state from preferences */
    private void loadMapState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        int zoom = settings.getInt(PREFS_ZOOM_LEVEL, 16);
    }


    /**
     * Inner class: SettingsContentObserver is a custom ContentObserver for changes in Android Settings
     */
    private class SettingsContentObserver extends ContentObserver {

        SettingsContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            LogHelper.v(LOG_TAG, "System Setting change detected.");

            // check if location setting was changed
            boolean previousLocationSystemSetting = mLocationSystemSetting;
            mLocationSystemSetting = LocationHelper.checkLocationSystemSetting(mActivity);
            if (previousLocationSystemSetting != mLocationSystemSetting) {
                LogHelper.v(LOG_TAG, "Location Setting change detected.");
                toggleLocationOffBar();
            }

            // start / stop preliminary tracking
            if (!mLocationSystemSetting) {
                stopPreliminaryTracking();
            } else if (!mTrackerServiceRunning && mFragmentVisible) {
                startPreliminaryTracking();
            }
        }

    }


    /**
     * Inner class: Saves track to external storage using AsyncTask
     */
    private class SaveTrackAsyncHelper extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            LogHelper.v(LOG_TAG, "Saving track object in background.");
            // save track object
            StorageHelper storageHelper = new StorageHelper(mActivity);
            storageHelper.saveTrack(mTrack);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // clear track object
            LogHelper.v(LOG_TAG, "Clearing track object after background save operation.");
            mTrack = null;
        }
    }


}