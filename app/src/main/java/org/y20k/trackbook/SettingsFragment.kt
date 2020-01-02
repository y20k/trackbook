/*
 * SettingsFragment.kt
 * Implements the SettingsFragment fragment
 * A SettingsFragment displays the user accessible settings of the app
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


import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.preference.*
import org.y20k.trackbook.helpers.LengthUnitHelper
import org.y20k.trackbook.helpers.LogHelper


/*
 * SettingsFragment class
 */
class SettingsFragment : PreferenceFragmentCompat() {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(SettingsFragment::class.java)


    /* Overrides onViewCreated from PreferenceFragmentCompat */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set the background color
        view.setBackgroundColor(resources.getColor(R.color.app_window_background, null))
        // add padding - necessary because translucent status bar is used
        val topPadding = this.resources.displayMetrics.density * 24 // 24 dp * display density
        view.setPadding(0, topPadding.toInt(), 0, 0)
    }


    /* Overrides onCreatePreferences from PreferenceFragmentCompat */
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        val context = preferenceManager.context
        val screen = preferenceManager.createPreferenceScreen(context)

        // set up "Enable Imperial Measurements" preference
        val preferenceImperialMeasurementUnits: SwitchPreferenceCompat = SwitchPreferenceCompat(activity as Context)
        preferenceImperialMeasurementUnits.title = getString(R.string.pref_imperial_measurement_units_title)
        preferenceImperialMeasurementUnits.key = Keys.PREF_USE_IMPERIAL_UNITS
        preferenceImperialMeasurementUnits.summaryOn = getString(R.string.pref_imperial_measurement_units_summary_imperial)
        preferenceImperialMeasurementUnits.summaryOff = getString(R.string.pref_imperial_measurement_units_summary_metric)
        preferenceImperialMeasurementUnits.setDefaultValue(LengthUnitHelper.useImperialUnits())

        // set up "Restrict to GPS" preference
        val preferenceGpsOnly: SwitchPreferenceCompat = SwitchPreferenceCompat(activity as Context)
        preferenceGpsOnly.title = getString(R.string.pref_gps_only_title)
        preferenceGpsOnly.key = Keys.PREF_GPS_ONLY
        preferenceGpsOnly.summaryOn = getString(R.string.pref_gps_only_summary_gps_only)
        preferenceGpsOnly.summaryOff = getString(R.string.pref_gps_only_summary_gps_and_network)
        preferenceGpsOnly.setDefaultValue(false)

        // set up "Accuracy Threshold" preference
        val preferenceAccuracyThreshold: SeekBarPreference = SeekBarPreference(activity as Context)
        preferenceAccuracyThreshold.title = getString(R.string.pref_accuracy_threshold_title)
        preferenceAccuracyThreshold.key = Keys.PREF_LOCATION_ACCURACY_THRESHOLD
        preferenceAccuracyThreshold.summary = getString(R.string.pref_accuracy_threshold_summary)
        preferenceAccuracyThreshold.showSeekBarValue = true
        preferenceAccuracyThreshold.max = 50
        preferenceAccuracyThreshold.setDefaultValue(Keys.DEFAULT_THRESHOLD_LOCATION_ACCURACY)

        // set up "Reset" preference
        val preferenceResetAdvanced: Preference = Preference(activity as Context)
        preferenceResetAdvanced.title = getString(R.string.pref_reset_advanced_title)
        preferenceResetAdvanced.summary = getString(R.string.pref_reset_advanced_summary)
        preferenceResetAdvanced.setOnPreferenceClickListener{
            preferenceAccuracyThreshold.value = Keys.DEFAULT_THRESHOLD_LOCATION_ACCURACY
            return@setOnPreferenceClickListener true
        }

        // set preference categories
        val preferenceCategoryGeneral: PreferenceCategory = PreferenceCategory(activity as Context)
        preferenceCategoryGeneral.title = getString(R.string.pref_general_title)
        preferenceCategoryGeneral.contains(preferenceImperialMeasurementUnits)
        preferenceCategoryGeneral.contains(preferenceGpsOnly)
        val preferenceCategoryAdvanced: PreferenceCategory = PreferenceCategory(activity as Context)
        preferenceCategoryAdvanced.title = getString(R.string.pref_advanced_title)
        preferenceCategoryAdvanced.contains(preferenceAccuracyThreshold)
        preferenceCategoryAdvanced.contains(preferenceResetAdvanced)

        // setup preference screen
        screen.addPreference(preferenceCategoryGeneral)
        screen.addPreference(preferenceImperialMeasurementUnits)
        screen.addPreference(preferenceGpsOnly)
        screen.addPreference(preferenceCategoryAdvanced)
        screen.addPreference(preferenceAccuracyThreshold)
        screen.addPreference(preferenceResetAdvanced)
        preferenceScreen = screen
    }

}
