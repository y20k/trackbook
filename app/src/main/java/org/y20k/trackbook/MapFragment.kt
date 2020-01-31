/*
 * MapFragment.kt
 * Implements the MapFragment fragment
 * A MapFragment displays a map using osmdroid as well as the controls to start / stop a recording
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */


package org.y20k.trackbook

import YesNoDialog
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.core.TracklistElement
import org.y20k.trackbook.helpers.*
import org.y20k.trackbook.ui.MapFragmentLayoutHolder


/*
 * MapFragment class
 */
class MapFragment : Fragment(), YesNoDialog.YesNoDialogListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(MapFragment::class.java)


    /* Main class variables */
    private var bound: Boolean = false
    private val handler: Handler = Handler()
    private var trackingState: Int = Keys.STATE_TRACKING_NOT
    private var gpsProviderActive: Boolean = false
    private var networkProviderActive: Boolean = false
    private var track: Track = Track()
    private lateinit var currentBestLocation: Location
    private lateinit var layout: MapFragmentLayoutHolder
    private lateinit var trackerService: TrackerService
    private lateinit var sharedPreferenceChangeListener2:SharedPreferences.OnSharedPreferenceChangeListener


    /* Overrides onCreate from Fragment */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // get current best location
        currentBestLocation = LocationHelper.getLastKnownLocation(activity as Context)
        // get saved tracking state
        trackingState = PreferencesHelper.loadTrackingState(activity as Context)
    }


    /* Overrides onStop from Fragment */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // initialize layout
        layout = MapFragmentLayoutHolder(activity as Context, inflater, container, currentBestLocation, trackingState)

        // set up buttons
        layout.currentLocationButton.setOnClickListener {
            layout.centerMap(currentBestLocation, animated = true)
        }
        layout.recordingButton.setOnClickListener {
            handleTrackingManagementMenu()
        }
        layout.saveButton.setOnClickListener {
            saveTrack()
        }
        layout.clearButton.setOnClickListener {
            trackerService.clearTrack()
        }
        layout.resumeButton.setOnClickListener {
            // start service via intent so that it keeps running after unbind
            startTrackerService()
            trackerService.resumeTracking()
        }

        return layout.rootView
    }


    /* Overrides onStart from Fragment */
    override fun onStart() {
        super.onStart()
        // request location permission if denied
        if (ContextCompat.checkSelfPermission(activity as Context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            this.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Keys.REQUEST_CODE_FOREGROUND)
        }
        // bind to TrackerService
        activity?.bindService(Intent(activity, TrackerService::class.java), connection, Context.BIND_AUTO_CREATE)
    }


    /* Overrides onResume from Fragment */
    override fun onResume() {
        super.onResume()
        // show hide the location error snackbar
        layout.toggleLocationErrorBar(gpsProviderActive, networkProviderActive)
        // set map center
        layout.centerMap(currentBestLocation)
    }


    /* Overrides onPause from Fragment */
    override fun onPause() {
        super.onPause()
        layout.saveState(currentBestLocation)
    }


    /* Overrides onStop from Fragment */
    override fun onStop() {
        super.onStop()
        // unbind from TrackerService
        activity?.unbindService(connection)
    }


    /* Overrides onRequestPermissionsResult from Fragment */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Keys.REQUEST_CODE_FOREGROUND -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted - re-bind service
                    activity?.unbindService(connection)
                    activity?.bindService(Intent(activity, TrackerService::class.java), connection, Context.BIND_AUTO_CREATE)
                    LogHelper.i(TAG, "Request result: Location permission has been granted.")
                } else {
                    // permission denied - unbind service
                    activity?.unbindService(connection)
                }
                layout.toggleLocationErrorBar(gpsProviderActive, networkProviderActive)
                return
            }
        }
    }


    /* Overrides onYesNoDialog from YesNoDialogListener */
    override fun onYesNoDialog(type: Int, dialogResult: Boolean, payload: Int, payloadString: String) {
        super.onYesNoDialog(type, dialogResult, payload, payloadString)
        when (type) {
            Keys.DIALOG_EMPTY_RECORDING -> {
                when (dialogResult) {
                    // user tapped resume
                    true -> {
                        trackerService.resumeTracking()
                    }
                }
            }
        }
    }


    /* Start tracker service */
    private fun startTrackerService() {
        val intent = Intent(activity, TrackerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // ... start service in foreground to prevent it being killed on Oreo
            activity?.startForegroundService(intent)
        } else {
            activity?.startService(intent)
        }
    }


    /* Starts / pauses tracking and toggles the recording sub menu_bottom_navigation */
    private fun handleTrackingManagementMenu() {
        when (trackingState) {
            Keys.STATE_TRACKING_STOPPED -> layout.toggleRecordingButtonSubMenu()
            Keys.STATE_TRACKING_ACTIVE -> trackerService.stopTracking()
            Keys.STATE_TRACKING_NOT -> {
                // start service via intent so that it keeps running after unbind
                startTrackerService()
                trackerService.startTracking()
            }
        }
    }


    /* Saves track - shows dialog, if recording is still empty */
    private fun saveTrack() {
        if (track.wayPoints.isEmpty()) {
            YesNoDialog(this as YesNoDialog.YesNoDialogListener).show(activity as Context, type = Keys.DIALOG_EMPTY_RECORDING, title = R.string.dialog_error_empty_recording_title, message = R.string.dialog_error_empty_recording_message, yesButton = R.string.dialog_error_empty_recording_action_resume)
        } else {
            GlobalScope.launch {
                // step 1: create and store filenames for json and gpx files
                track.trackUriString = FileHelper.getTrackFileUri(activity as Context, track).toString()
                track.gpxUriString = FileHelper.getGpxFileUri(activity as Context, track).toString()
                // step 2: save track
                FileHelper.saveTrackSuspended(track, saveGpxToo = true)
                // step 3: save tracklist - suspended
                FileHelper.addTrackAndSaveTracklistSuspended(activity as Context, track)
                // step 3: clear track
                trackerService.clearTrack()
                // step 4: open track in TrackFragement
                openTrack(track.toTracklistElement(activity as Context))
            }
        }
    }


    /* Opens a track in TrackFragment */
    private fun openTrack(tracklistElement: TracklistElement) {
        val bundle: Bundle = Bundle()
        bundle.putString(Keys.ARG_TRACK_TITLE, tracklistElement.name)
        bundle.putString(Keys.ARG_TRACK_FILE_URI, tracklistElement.trackUriString)
        bundle.putString(Keys.ARG_GPX_FILE_URI, tracklistElement.gpxUriString)
        bundle.putLong(Keys.ARG_TRACK_ID, TrackHelper.getTrackId(tracklistElement))
        findNavController().navigate(R.id.fragment_track, bundle)
    }


    /*
     * Defines the listener for changes in shared preferences
     */
    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        when (key) {
            Keys.PREF_TRACKING_STATE -> {
                trackingState = PreferencesHelper.loadTrackingState(activity as Context)
                layout.updateRecordingButton(trackingState)
            }
        }
    }
    /*
     * End of declaration
     */


    private fun createSharedPreferenceChangeListener(): SharedPreferences.OnSharedPreferenceChangeListener {
        return SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                Keys.PREF_TRACKING_STATE -> {
                    if (isAdded) {
                        trackingState = PreferencesHelper.loadTrackingState(activity as Context)
                        layout.updateRecordingButton(trackingState)
                    }
                }
            }
        }
    }



    /*
     * Defines callbacks for service binding, passed to bindService()
     */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as TrackerService.LocalBinder
            trackerService = binder.service
            bound = true
            // register listener for changes in shared preferences
            sharedPreferenceChangeListener2 = createSharedPreferenceChangeListener()
            PreferenceManager.getDefaultSharedPreferences(activity as Context).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener2)
            // start listening for location updates
            handler.removeCallbacks(periodicLocationRequestRunnable)
            handler.postDelayed(periodicLocationRequestRunnable, 0)
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
            // unregister listener for changes in shared preferences
            PreferenceManager.getDefaultSharedPreferences(activity as Context).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener2)
            // stop receiving location updates
            handler.removeCallbacks(periodicLocationRequestRunnable)
        }
    }
    /*
     * End of declaration
     */


    /*
     * Runnable: Periodically requests location
     */
    private val periodicLocationRequestRunnable: Runnable = object : Runnable {
        override fun run() {
            // pull values from service
            currentBestLocation = trackerService.currentBestLocation
            track = trackerService.track
            gpsProviderActive = trackerService.gpsProviderActive
            networkProviderActive = trackerService.networkProviderActive
            trackingState = trackerService.trackingState
            // update location and track
            layout.markCurrentPosition(currentBestLocation, trackingState)
            layout.overlayCurrentTrack(track, trackingState)
            // center map, if it had not been dragged/zoomed before
            if (!layout.userInteraction) { layout.centerMap(currentBestLocation, true)}
            // show error snackbar if necessary
            layout.toggleLocationErrorBar(gpsProviderActive, networkProviderActive)
            // use the handler to start runnable again after specified delay
            handler.postDelayed(this, Keys.REQUEST_CURRENT_LOCATION_INTERVAL)
        }
    }
    /*
     * End of declaration
     */

}
