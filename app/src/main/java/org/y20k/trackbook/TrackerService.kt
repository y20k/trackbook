/*
 * TrackerService.kt
 * Implements the app's movement tracker service
 * The TrackerService keeps track of the current location
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

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.helpers.*
import java.util.*
import kotlin.coroutines.CoroutineContext


/*
 * TrackerService class
 */
class TrackerService(): Service(), CoroutineScope, SensorEventListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TrackerService::class.java)


    /* Main class variables */
    var trackingState: Int = Keys.STATE_TRACKING_NOT
    var gpsProviderActive: Boolean = false
    var networkProviderActive: Boolean = false
    var useImperial: Boolean = false
    var gpsOnly: Boolean = false
    var locationAccuracyThreshold: Int = Keys.DEFAULT_THRESHOLD_LOCATION_ACCURACY
    var currentBestLocation: Location = LocationHelper.getDefaultLocation()
    var stepCountOffset: Float = 0f
    var track: Track = Track()
    var gpsLocationListenerRegistered: Boolean = false
    var networkLocationListenerRegistered: Boolean = false
    var bound: Boolean = false
    private val binder = LocalBinder()
    private val handler: Handler = Handler()
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var gpsLocationListener: LocationListener
    private lateinit var networkLocationListener: LocationListener
    private lateinit var backgroundJob: Job


    /* Overrides coroutineContext variable */
    override val coroutineContext: CoroutineContext get() = backgroundJob + Dispatchers.Main


    /* Overrides onCreate from Service */
    override fun onCreate() {
        super.onCreate()
        gpsOnly = PreferencesHelper.loadGpsOnly(this)
        useImperial = PreferencesHelper.loadUseImperialUnits(this)
        locationAccuracyThreshold = PreferencesHelper.loadAccuracyThreshold(this)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationHelper = NotificationHelper(this)
        gpsProviderActive = LocationHelper.isGpsEnabled(locationManager)
        networkProviderActive = LocationHelper.isNetworkEnabled(locationManager)
        gpsLocationListener = createLocationListener()
        networkLocationListener = createLocationListener()
        trackingState = PreferencesHelper.loadTrackingState(this)
        currentBestLocation = LocationHelper.getLastKnownLocation(this)
        track = FileHelper.readTrack(this, FileHelper.getTempFileUri(this))
        backgroundJob = Job()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
    }


    /* Overrides onStartCommand from Service */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // SERVICE RESTART (via START_STICKY)
        if (intent == null) {
            if (trackingState == Keys.STATE_TRACKING_ACTIVE) {
                LogHelper.w(TAG, "Trackbook has been killed by the operating system. Trying to resume recording.")
                resumeTracking()
            }
        // ACTION STOP
        } else if (Keys.ACTION_STOP == intent.action) {
            stopTracking()
        // ACTION START
        } else if (Keys.ACTION_START == intent.action) {
            startTracking()
        // ACTION RESUME
        } else if (Keys.ACTION_RESUME == intent.action) {
            resumeTracking()
        }

        // START_STICKY is used for services that are explicitly started and stopped as needed
        return START_STICKY
    }


    /* Overrides onBind from Service */
    override fun onBind(p0: Intent?): IBinder? {
        LogHelper.e(TAG, "onBind called.") // todo remove
        bound = true
        // start receiving location updates
        addGpsLocationListener()
        addNetworkLocationListener()
        // return reference to this service
        return binder
    }


    /* Overrides onRebind from Service */
    override fun onRebind(intent: Intent?) {
        LogHelper.e(TAG, "onRebind called.") // todo remove
        bound = true
        // start receiving location updates
        addGpsLocationListener()
        addNetworkLocationListener()
    }


    /* Overrides onUnbind from Service */
    override fun onUnbind(intent: Intent?): Boolean {
        LogHelper.e(TAG, "onUnbind called.") // todo remove
        bound = false
        // stop receiving location updates - if not tracking
        if (trackingState != Keys.STATE_TRACKING_ACTIVE) {
            removeGpsLocationListener()
            removeNetworkLocationListener()
        }
        // ensures onRebind is called
        return true
    }

    /* Overrides onDestroy from Service */
    override fun onDestroy() {
        super.onDestroy()
        LogHelper.i(TAG, "onDestroy called.")
        // stop tracking
        if (trackingState == Keys.STATE_TRACKING_ACTIVE) stopTracking()
        // remove notification
        stopForeground(true)
        // stop listening for changes in shared preferences
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener)
        // stop receiving location updates
        removeGpsLocationListener()
        removeNetworkLocationListener()
        // cancel background job
        backgroundJob.cancel()
    }


    /* Overrides onAccuracyChanged from SensorEventListener */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        LogHelper.v(TAG, "Accuracy changed: $accuracy")
    }


    /* Overrides onSensorChanged from SensorEventListener */
    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        var steps: Float = 0f
        if (sensorEvent != null) {
            if (stepCountOffset == 0f) {
                // store steps previously recorded by the system
                stepCountOffset = (sensorEvent.values[0] - 1) - track.stepCount // subtract any steps recorded during this session in case the app was killed
            }
            // calculate step count - subtract steps previously recorded
            steps = sensorEvent.values[0] - stepCountOffset
        }
        // update step count in track
        track.stepCount = steps
    }


    /* Resume tracking after stop/pause */
    fun resumeTracking() {
        // load temp track - returns an empty track if not available
        track = FileHelper.readTrack(this, FileHelper.getTempFileUri(this))
        // try to mark last waypoint as stopover
        if (track.wayPoints.size > 0) {
            val lastWayPointIndex = track.wayPoints.size - 1
            track.wayPoints.get(lastWayPointIndex).isStopOver = true
        }
        // start tracking
        startTracking(newTrack = false)
    }


    /* Start tracking location */
    fun startTracking(newTrack: Boolean = true) {
        // start receiving location updates
        addGpsLocationListener()
        addNetworkLocationListener()
        // set up new track
        if (newTrack) {
            track = Track()
            track.recordingStart = GregorianCalendar.getInstance().time
            track.recordingStop = track.recordingStart
            track.name = DateTimeHelper.convertToReadableDate(track.recordingStart)
            stepCountOffset = 0f
        }
        // set state
        trackingState = Keys.STATE_TRACKING_ACTIVE
        PreferencesHelper.saveTrackingState(this, trackingState)
        // start recording steps and location fixes
        startStepCounter()
        handler.postDelayed(periodicTrackUpdate, 0)
        // show notification
        startForeground(Keys.TRACKER_SERVICE_NOTIFICATION_ID, displayNotification())
    }


    /* Stop tracking location */
    fun stopTracking() {
        // stop receiving location updates - if app is not bound (= visible)
        if (!bound) {
            removeGpsLocationListener()
            removeGpsLocationListener()
        }
        // save state
        track.recordingStop = GregorianCalendar.getInstance().time
        trackingState = Keys.STATE_TRACKING_STOPPED
        PreferencesHelper.saveTrackingState(this, trackingState)
        // stop recording steps and location fixes
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(periodicTrackUpdate)
        // update notification
        displayNotification()
        stopForeground(false)
    }


    /* Clear track recording */
    fun clearTrack() {
        track = Track()
        FileHelper.deleteTempFile(this)
        trackingState = Keys.STATE_TRACKING_NOT
        PreferencesHelper.saveTrackingState(this, trackingState)
        stopForeground(true)
    }


    /* Saves track recording to storage */
    fun saveTrack() {
        // save track using "deferred await"
        launch {
            // step 1: create and store filenames for json and gpx files
            track.trackUriString = FileHelper.getTrackFileUri(this@TrackerService, track).toString()
            track.gpxUriString = FileHelper.getGpxFileUri(this@TrackerService, track).toString()
            // step 2: save track
            FileHelper.saveTrackSuspended(track, saveGpxToo = true)
            // step 3: save tracklist
            FileHelper.addTrackAndSaveTracklistSuspended(this@TrackerService, track)
            // step 3: clear track
            clearTrack()
        }
    }


    /* Creates location listener */
    private fun createLocationListener(): LocationListener {
        return object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // update currentBestLocation if a better location is available
                if (LocationHelper.isBetterLocation(location, currentBestLocation)) {
                    currentBestLocation = location
                }
            }
            override fun onProviderEnabled(provider: String) {
                LogHelper.v(TAG, "onProviderEnabled $provider")
                when (provider) {
                    LocationManager.GPS_PROVIDER -> gpsProviderActive = LocationHelper.isGpsEnabled(locationManager)
                    LocationManager.NETWORK_PROVIDER -> networkProviderActive = LocationHelper.isNetworkEnabled(locationManager)
                }
            }
            override fun onProviderDisabled(provider: String) {
                LogHelper.v(TAG, "onProviderDisabled $provider")
                when (provider) {
                    LocationManager.GPS_PROVIDER -> gpsProviderActive = LocationHelper.isGpsEnabled(locationManager)
                    LocationManager.NETWORK_PROVIDER -> networkProviderActive = LocationHelper.isNetworkEnabled(locationManager)
                }
            }
            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
                // deprecated method
            }
        }
    }


    /* Adds a GPS location listener to location manager */
    private fun addGpsLocationListener() {
        // check if already registered
        if (!gpsLocationListenerRegistered) {
            // check if Network provider is available
            if (gpsProviderActive) {
                // check for location permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // adds GPS location listener
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f,gpsLocationListener)
                    gpsLocationListenerRegistered = true
                    LogHelper.v(TAG, "Added GPS location listener.")
                } else {
                    LogHelper.w(TAG, "Unable to add GPS location listener. Location permission is not granted.")
                }
            } else {
                LogHelper.w(TAG, "Unable to add GPS location listener.")
            }
        } else {
            LogHelper.v(TAG, "Skipping registration. GPS location listener has already been added.")
        }
    }


    /* Adds a Network location listener to location manager */
    private fun addNetworkLocationListener() {
        // check if already registered
        if (!networkLocationListenerRegistered) {
            // check if Network provider is available
            if (networkProviderActive && !gpsOnly) {
                // check for location permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // adds Network location listener
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, networkLocationListener)
                    networkLocationListenerRegistered = true
                    LogHelper.v(TAG, "Added Network location listener.")
                } else {
                    LogHelper.w(TAG, "Unable to add Network location listener. Location permission is not granted.")
                }
            } else {
                LogHelper.w(TAG, "Unable to add Network location listener.")
            }
        } else {
            LogHelper.v(TAG, "Skipping registration. Network location listener has already been added.")
        }
    }


    /* Adds location listeners to location manager */
    fun removeGpsLocationListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(gpsLocationListener)
            gpsLocationListenerRegistered = false
            LogHelper.v(TAG, "Removed GPS location listener.")
        } else {
            LogHelper.w(TAG, "Unable to remove GPS location listener. Location permission is needed.")
        }
    }


    /* Adds location listeners to location manager */
    fun removeNetworkLocationListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(gpsLocationListener)
            networkLocationListenerRegistered = false
            LogHelper.v(TAG, "Removed Network location listener.")
        } else {
            LogHelper.w(TAG, "Unable to remove Network location listener. Location permission is needed.")
        }
    }


    /* Registers a step counter listener */
    private fun startStepCounter() {
        val stepCounterAvailable = sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), SensorManager.SENSOR_DELAY_UI)
        if (!stepCounterAvailable) {
            LogHelper.w(TAG, "Pedometer sensor not available.")
            track.stepCount = -1f
        }
    }


    /* Displays / updates notification */
    private fun displayNotification(): Notification {
        val notification: Notification = notificationHelper.createNotification(trackingState, track.length, track.duration, useImperial)
        notificationManager.notify(Keys.TRACKER_SERVICE_NOTIFICATION_ID, notification)
        return notification
    }


    /*
     * Defines the listener for changes in shared preferences
     */
    private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            when (key) {
                // preference "Restrict to GPS"
                Keys.PREF_GPS_ONLY -> {
                    gpsOnly = PreferencesHelper.loadGpsOnly(this@TrackerService)
                    when (gpsOnly) {
                        true -> removeNetworkLocationListener()
                        false -> addNetworkLocationListener()
                    }
                }
                // preference "Use Imperial Measurements"
                Keys.PREF_USE_IMPERIAL_UNITS -> {
                    useImperial = PreferencesHelper.loadUseImperialUnits(this@TrackerService)
                }
                // preference "Accuracy Threshold"
                Keys.PREF_LOCATION_ACCURACY_THRESHOLD -> {
                    locationAccuracyThreshold = PreferencesHelper.loadAccuracyThreshold(this@TrackerService)
                }
            }
        }
    /*
     * End of declaration
     */


    /*
     * Inner class: Local Binder that returns this service
     */
    inner class LocalBinder : Binder() {
        val service: TrackerService = this@TrackerService
    }
    /*
     * End of inner class
     */


    /*
     * Runnable: Periodically track updates (if recording active)
     */
    private val periodicTrackUpdate: Runnable = object : Runnable {
        override fun run() {
            // add waypoint to track - step count is continuously updated in onSensorChanged
            track = TrackHelper.addWayPointToTrack(track, currentBestLocation, locationAccuracyThreshold)
            // update notification
            displayNotification()
            // save temp track using GlobalScope.launch = fire & forget (no return value from save)
            GlobalScope.launch { FileHelper.saveTempTrackSuspended(this@TrackerService, track) }
            // re-run this in 10 seconds
            handler.postDelayed(this, Keys.ADD_WAYPOINT_TO_TRACK_INTERVAL)
        }
    }
    /*
     * End of declaration
     */


}