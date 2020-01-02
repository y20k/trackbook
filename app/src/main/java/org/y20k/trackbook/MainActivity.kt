/*
 * MainActivity.kt
 * Implements the main activity of the app
 * The MainActivity hosts fragments for: current map, track list,  settings
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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.osmdroid.config.Configuration
import org.y20k.trackbook.helpers.ImportHelper
import org.y20k.trackbook.helpers.LogHelper
import org.y20k.trackbook.helpers.PreferencesHelper


/*
 * MainActivity class
 */
class MainActivity : AppCompatActivity() {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(MainActivity::class.java)


    /* Main class variables */
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var bottomNavigationView: BottomNavigationView


    /* Overrides onCreate from AppCompatActivity */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set user agent to prevent getting banned from the osm servers
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
        // set the path for osmdroid's files (e.g. tile cache)
        Configuration.getInstance().osmdroidBasePath = this.getExternalFilesDir(null)

        // set up views
        setContentView(R.layout.activity_main)
        navHostFragment  = supportFragmentManager.findFragmentById(R.id.main_container) as NavHostFragment
        bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNavigationView.setupWithNavController(navController = navHostFragment.navController)

        // listen for navigation changes
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.fragment_track -> {
                    runOnUiThread( Runnable() {
                        run(){
                            // mark menu item "Tracks" as checked
                            bottomNavigationView.menu.findItem(R.id.tracklist_fragment).setChecked(true)
                        }
                    })
                }
                else -> {
                    // do nothing
                }
            }
        }

        // convert old tracks (one-time import)
        if (PreferencesHelper.isHouseKeepingNecessary(this)) {
            ImportHelper.convertOldTracks(this)
            PreferencesHelper.saveHouseKeepingNecessaryState(this)
        }

    }

}
