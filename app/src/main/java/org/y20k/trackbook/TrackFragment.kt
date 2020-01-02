/*
 * TrackFragment.kt
 * Implements the TrackFragment fragment
 * A TrackFragment displays a previously recorded track
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
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.y20k.trackbook.Keys.ARG_TRACK_ID
import org.y20k.trackbook.dialogs.RenameTrackDialog
import org.y20k.trackbook.helpers.FileHelper
import org.y20k.trackbook.helpers.LogHelper
import org.y20k.trackbook.ui.TrackFragmentLayoutHolder

class TrackFragment : Fragment(), RenameTrackDialog.RenameTrackListener, YesNoDialog.YesNoDialogListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TrackFragment::class.java)


    /* Main class variables */
    private lateinit var layout:TrackFragmentLayoutHolder


    /* Overrides onCreateView from Fragment */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // initialize layout
        layout = TrackFragmentLayoutHolder(activity as Context, inflater, container, arguments)

        // set up share button
        layout.shareButton.setOnClickListener {
            shareGpXTrack()
        }
        // set up delete button
        layout.deleteButton.setOnClickListener {
            val dialogMessage: String = "${getString(R.string.dialog_yes_no_message_remove_recording)}\n\n- ${layout.trackNameView.text}"
            YesNoDialog(this@TrackFragment as YesNoDialog.YesNoDialogListener).show(context = activity as Context, type = Keys.DIALOG_REMOVE_TRACK, messageString = dialogMessage, yesButton = R.string.dialog_yes_no_positive_button_remove_recording)
        }
        // set up rename button
        layout.editButton.setOnClickListener {
            RenameTrackDialog(this as RenameTrackDialog.RenameTrackListener).show(activity as Context, layout.trackNameView.text.toString())
        }

        return layout.rootView
    }


    /* Overrides onResume from Fragment */
    override fun onResume() {
        super.onResume()
        // update zoom level and map center
        layout.updateMapView()
    }


    /* Overrides onPause from Fragment */
    override fun onPause() {
        super.onPause()
        // save zoom level and map center
        layout.saveViewStateToTrack()
    }


    /* Overrides onRenameTrackDialog from RenameTrackDialog */
    override fun onRenameTrackDialog(textInput: String) {
        // rename track async (= fire & forget - no return value needed)
        GlobalScope.launch { FileHelper.renameTrackSuspended(activity as Context, layout.track, textInput) }
        // update name in layout
        layout.track.name = textInput
        layout.trackNameView.text = textInput
    }


    /* Overrides onYesNoDialog from YesNoDialogListener */
    override fun onYesNoDialog(type: Int, dialogResult: Boolean, payload: Int, payloadString: String) {
        when (type) {
            Keys.DIALOG_REMOVE_TRACK -> {
                when (dialogResult) {
                    // user tapped remove track
                    true -> {
                        // switch to TracklistFragment and remove track there
                        val trackId: Long = arguments?.getLong(ARG_TRACK_ID, -1L) ?: -1L
                        val bundle: Bundle = bundleOf(Keys.ARG_TRACK_ID to trackId)
                        findNavController().navigate(R.id.tracklist_fragment, bundle)
                    }
                }
            }
        }
    }


    /* Share track as GPX via share sheet */
    private fun shareGpXTrack() {
        val gpxFile = Uri.parse(layout.track.gpxUriString).toFile()
        val gpxShareUri = FileProvider.getUriForFile(this.activity as Context, "${activity!!.applicationContext.packageName}.provider", gpxFile)
        val shareIntent: Intent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            data = gpxShareUri
            type = "application/gpx+xml"
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_STREAM, gpxShareUri)
            putExtra(Intent.EXTRA_TITLE, getString(R.string.dialog_share_gpx))
        }, null)

        // show share sheet - if file helper is available
        val packageManager: PackageManager? = activity?.packageManager
        if (packageManager != null && shareIntent.resolveActivity(packageManager) != null) {
            startActivity(shareIntent)
        } else {
            Toast.makeText(activity, R.string.toast_message_install_file_helper, Toast.LENGTH_LONG).show()
        }
    }

}
