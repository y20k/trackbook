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
import kotlinx.coroutines.Dispatchers.IO
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.helpers.*
import java.util.*


/*
 * TrackerService class
 */
class TrackerService: Service(), SensorEventListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TrackerService::class.java)


    /* Main class variables */
    var trackingState: Int = Keys.STATE_TRACKING_NOT
    var gpsProviderActive: Boolean = false
    var networkProviderActive: Boolean = false
    var useImperial: Boolean = false
    var gpsOnly: Boolean = false
    var accuracyMultiplier: Int = 1
    var currentBestLocation: Location = LocationHelper.getDefaultLocation()
    var lastSave: Date = Keys.DEFAULT_DATE
    var stepCountOffset: Float = 0f
    var resumed: Boolean = false
    var track: Track = Track()
    var gpsLocationListenerRegistered: Boolean = false
    var networkLocationListenerRegistered: Boolean = false
    var bound: Boolean = false
    private val binder = LocalBinder()
    private val handler: Handler = Handler()
    private var altitudeValues: SimpleMovingAverageQueue = SimpleMovingAverageQueue(Keys.DEFAULT_ALTITUDE_SMOOTHING_VALUE)
    private lateinit var locationManager: LocationManager
    private lateinit var sensorManager: SensorManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var gpsLocationListener: LocationListener
    private lateinit var networkLocationListener: LocationListener


    /* Overrides onCreate from Service */
    override fun onCreate() {
        super.onCreate()
        gpsOnly = PreferencesHelper.loadGpsOnly()
        useImperial = PreferencesHelper.loadUseImperialUnits()
        accuracyMultiplier = PreferencesHelper.loadAccuracyMultiplier()

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sensorManager = this.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationHelper = NotificationHelper(this)
        gpsProviderActive = LocationHelper.isGpsEnabled(locationManager)
        networkProviderActive = LocationHelper.isNetworkEnabled(locationManager)
        gpsLocationListener = createLocationListener()
        networkLocationListener = createLocationListener()
        trackingState = PreferencesHelper.loadTrackingState()
        currentBestLocation = LocationHelper.getLastKnownLocation(this)
        track = FileHelper.readTrack(this, FileHelper.getTempFileUri(this))
        altitudeValues.capacity = PreferencesHelper.loadAltitudeSmoothingValue()
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(
            sharedPreferenceChangeListener
        )
    }


    /* Overrides onStartCommand from Service */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        // SERVICE RESTART (via START_STICKY)
        if (intent == null) {
            if (trackingState == Keys.STATE_TRACKING_ACTIVE) {
                LogHelper.w(
                    TAG,
                    "Trackbook has been killed by the operating system. Trying to resume recording."
                )
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
        bound = true
        // start receiving location updates
        addGpsLocationListener()
        addNetworkLocationListener()
        // return reference to this service
        return binder
    }


    /* Overrides onRebind from Service */
    override fun onRebind(intent: Intent?) {
        bound = true
        // start receiving location updates
        addGpsLocationListener()
        addNetworkLocationListener()
    }


    /* Overrides onUnbind from Service */
    override fun onUnbind(intent: Intent?): Boolean {
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
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(
            sharedPreferenceChangeListener
        )
        // stop receiving location updates
        removeGpsLocationListener()
        removeNetworkLocationListener()
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
            track.wayPoints[lastWayPointIndex].isStopOver = true
        }
        // set resumed flag
        resumed = true
        // calculate length of recording break
        track.recordingPaused = track.recordingPaused + TrackHelper.calculateDurationOfPause(track.recordingStop)
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
        PreferencesHelper.saveTrackingState(trackingState)
        // start recording steps and location fixes
        startStepCounter()
        handler.postDelayed(periodicTrackUpdate, 0)
        // show notification
        startForeground(Keys.TRACKER_SERVICE_NOTIFICATION_ID, displayNotification())
    }


    /* Stop tracking location */
    fun stopTracking() {
        // save temp track
        track.recordingStop = GregorianCalendar.getInstance().time
        CoroutineScope(IO).launch { FileHelper.saveTempTrackSuspended(this@TrackerService, track) }
        // save state
        trackingState = Keys.STATE_TRACKING_STOPPED
        PreferencesHelper.saveTrackingState(trackingState)
        // reset altitude values queue
        altitudeValues.reset()
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
        PreferencesHelper.saveTrackingState(trackingState)
        stopForeground(true)
    }


//    /* Saves track recording to storage */ // todo remove
//    fun saveTrack() {
//        // save track using "deferred await"
//        launch {
//            // step 1: create and store filenames for json and gpx files
//            track.trackUriString = FileHelper.getTrackFileUri(this@TrackerService, track).toString()
//            track.gpxUriString = FileHelper.getGpxFileUri(this@TrackerService, track).toString()
//            // step 2: save track
//            FileHelper.saveTrackSuspended(track, saveGpxToo = true)
//            // step 3: save tracklist
//            FileHelper.addTrackAndSaveTracklistSuspended(this@TrackerService, track)
//            // step 3: clear track
//            clearTrack()
//        }
//    }


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
                    LocationManager.GPS_PROVIDER -> gpsProviderActive = LocationHelper.isGpsEnabled(
                        locationManager
                    )
                    LocationManager.NETWORK_PROVIDER -> networkProviderActive =
                        LocationHelper.isNetworkEnabled(
                            locationManager
                        )
                }
            }
            override fun onProviderDisabled(provider: String) {
                LogHelper.v(TAG, "onProviderDisabled $provider")
                when (provider) {
                    LocationManager.GPS_PROVIDER -> gpsProviderActive = LocationHelper.isGpsEnabled(
                        locationManager
                    )
                    LocationManager.NETWORK_PROVIDER -> networkProviderActive =
                        LocationHelper.isNetworkEnabled(
                            locationManager
                        )
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
            gpsProviderActive = LocationHelper.isGpsEnabled(locationManager)
            if (gpsProviderActive) {
                // check for location permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                    // adds GPS location listener
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        0,
                        0f,
                        gpsLocationListener
                    )
                    gpsLocationListenerRegistered = true
                    LogHelper.v(TAG, "Added GPS location listener.")
                } else {
                    LogHelper.w(
                        TAG,
                        "Unable to add GPS location listener. Location permission is not granted."
                    )
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
            networkProviderActive = LocationHelper.isNetworkEnabled(locationManager)
            if (networkProviderActive && !gpsOnly) {
                // check for location permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {
                    // adds Network location listener
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        0,
                        0f,
                        networkLocationListener
                    )
                    networkLocationListenerRegistered = true
                    LogHelper.v(TAG, "Added Network location listener.")
                } else {
                    LogHelper.w(
                        TAG,
                        "Unable to add Network location listener. Location permission is not granted."
                    )
                }
            } else {
                LogHelper.w(TAG, "Unable to add Network location listener.")
            }
        } else {
            LogHelper.v(
                TAG,
                "Skipping registration. Network location listener has already been added."
            )
        }
    }


    /* Adds location listeners to location manager */
    fun removeGpsLocationListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(gpsLocationListener)
            gpsLocationListenerRegistered = false
            LogHelper.v(TAG, "Removed GPS location listener.")
        } else {
            LogHelper.w(
                TAG,
                "Unable to remove GPS location listener. Location permission is needed."
            )
        }
    }


    /* Adds location listeners to location manager */
    fun removeNetworkLocationListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(networkLocationListener)
            networkLocationListenerRegistered = false
            LogHelper.v(TAG, "Removed Network location listener.")
        } else {
            LogHelper.w(
                TAG,
                "Unable to remove Network location listener. Location permission is needed."
            )
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
        val notification: Notification = notificationHelper.createNotification(
            trackingState,
            track.length,
            track.duration,
            useImperial
        )
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
                    gpsOnly = PreferencesHelper.loadGpsOnly()
                    when (gpsOnly) {
                        true -> removeNetworkLocationListener()
                        false -> addNetworkLocationListener()
                    }
                }
                // preference "Use Imperial Measurements"
                Keys.PREF_USE_IMPERIAL_UNITS -> {
                    useImperial = PreferencesHelper.loadUseImperialUnits()
                }
                // preference "Recording Accuracy"
                Keys.PREF_RECORDING_ACCURACY_HIGH -> {
                    accuracyMultiplier = PreferencesHelper.loadAccuracyMultiplier()
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
            val result: Pair<Boolean, Track> = TrackHelper.addWayPointToTrack(track, currentBestLocation, accuracyMultiplier, resumed)
            // get results
            val successfullyAdded: Boolean = result.first
            track = result.second
            // check, if waypoint was added
            if (successfullyAdded) {
                // reset resumed flag, if necessary
                if (resumed) {
                    resumed = false
                }

                // store previous smoothed altitude
                val previousAltitude: Double = altitudeValues.getAverage()
                // put current altitude into queue
                val currentBestLocationAltitude: Double = currentBestLocation.altitude
                if (currentBestLocationAltitude != Keys.DEFAULT_ALTITUDE) altitudeValues.add(currentBestLocationAltitude)
                // TODO remove
                // uncomment to use test altitude values - useful if testing wirth an emulator
                //altitudeValues.add(getTestAltitude()) // TODO remove
                // TODO remove

                // only start calculating elevation differences, if enough data has been added to queue
                if (altitudeValues.prepared) {
                    // get current smoothed altitude
                    val currentAltitude: Double = altitudeValues.getAverage()
                    // calculate and store elevation differences
                    track = LocationHelper.calculateElevationDifferences(currentAltitude, previousAltitude, track)
                    // TODO remove
                    LogHelper.d(TAG, "Elevation Calculation || prev = $previousAltitude | curr = $currentAltitude | pos = ${track.positiveElevation} | neg = ${track.negativeElevation}")
                    // TODO remove
                }

                // save a temp track
                val now: Date = GregorianCalendar.getInstance().time
                if (now.time - lastSave.time > Keys.SAVE_TEMP_TRACK_INTERVAL) {
                    lastSave = now
                    CoroutineScope(IO).launch { FileHelper.saveTempTrackSuspended(this@TrackerService, track) }
                }
            }
            // update notification
            displayNotification()
            // re-run this in set interval
            handler.postDelayed(this, Keys.ADD_WAYPOINT_TO_TRACK_INTERVAL)
        }
    }
    /*
     * End of declaration
     */


    /* Simple queue that evicts older elements and holds an average */
    /* Credit: CircularQueue https://stackoverflow.com/a/51923797 */
    class SimpleMovingAverageQueue(var capacity: Int) : LinkedList<Double>() {
        var prepared: Boolean = false
        private var sum: Double = 0.0
        override fun add(element: Double): Boolean {
            prepared = this.size + 1 >= Keys.MIN_NUMBER_OF_WAYPOINTS_FOR_ELEVATION_CALCULATION
            if (this.size >= capacity) {
                sum -= this.first
                removeFirst()
            }
            sum += element
            return super.add(element)
        }
        fun getAverage(): Double = sum / this.size
        fun reset() {
            this.clear()
            prepared = false
            sum = 0.0
        }
    }


    // TODO remove
    val testAltitudes: Array<Double> = arrayOf(352.4349365234375, 358.883544921875, 358.6827392578125, 357.31396484375, 354.27459716796875, 354.573486328125, 354.388916015625, 354.6697998046875, 356.534912109375, 355.2772216796875, 356.21246337890625, 352.3499755859375, 350.37646484375, 351.2098388671875, 350.5213623046875, 350.5145263671875, 350.1728515625, 350.9075927734375, 351.5965576171875, 349.55767822265625, 351.548583984375, 357.1195068359375, 362.18634033203125, 366.3153076171875, 366.2218017578125, 362.1046142578125, 357.48291015625, 356.78570556640625, 353.7734375, 352.53936767578125, 351.8125, 353.1099853515625, 354.93035888671875, 355.4337158203125, 354.83270263671875, 352.9859619140625, 352.3006591796875, 351.63470458984375, 350.2501220703125, 351.75726318359375, 350.87664794921875, 350.4185791015625, 350.51568603515625, 349.5537109375, 345.2874755859375, 345.57196044921875, 349.99658203125, 353.3822021484375, 355.19061279296875, 359.1099853515625, 361.74365234375, 363.313232421875, 362.026611328125, 363.20703125, 363.2508544921875, 362.5870361328125, 362.521240234375)
    var testCounter: Int = 0
    fun getTestAltitude(): Double {
        if (testCounter >= testAltitudes.size) testCounter = 0
        val testAltitude: Double = testAltitudes[testCounter]
        testCounter ++
        return testAltitude
    }
    // TODO remove


}
