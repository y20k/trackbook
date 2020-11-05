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
import android.app.Activity
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
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.dialogs.RenameTrackDialog
import org.y20k.trackbook.helpers.*
import org.y20k.trackbook.ui.TrackFragmentLayoutHolder


class TrackFragment : Fragment(), RenameTrackDialog.RenameTrackListener, YesNoDialog.YesNoDialogListener, MapOverlayHelper.MarkerListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TrackFragment::class.java)


    /* Main class variables */
    private lateinit var layout: TrackFragmentLayoutHolder
    private lateinit var track: Track


    /* Overrides onCreate from Fragment */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // get track
        val fileUriString: String = arguments?.getString(Keys.ARG_TRACK_FILE_URI, String()) ?: String()
        track = if (fileUriString.isNotBlank()) {
            FileHelper.readTrack(activity as Context, fileUriString.toUri())
        } else {
            Track()
        }
    }


    /* Overrides onCreateView from Fragment */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // initialize layout
        val statusBarHeight: Int = UiHelper.getStatusBarHeight(activity as Context)
        layout = TrackFragmentLayoutHolder(activity as Context, this as MapOverlayHelper.MarkerListener, inflater, statusBarHeight, container, track)

        // set up share button
        layout.shareButton.setOnClickListener {
            openSaveGpxDialog()
        }
//        layout.shareButton.setOnLongClickListener {
//            val v = (activity as Context).getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//            v.vibrate(50)
//            shareGpxTrack()
//            return@setOnLongClickListener true
//        }
        // set up delete button
        layout.deleteButton.setOnClickListener {
            val dialogMessage: String = "${getString(R.string.dialog_yes_no_message_delete_recording)}\n\n- ${layout.trackNameView.text}"
            YesNoDialog(this@TrackFragment as YesNoDialog.YesNoDialogListener).show(context = activity as Context, type = Keys.DIALOG_DELETE_TRACK, messageString = dialogMessage, yesButton = R.string.dialog_yes_no_positive_button_delete_recording)
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


    /* Overrides onActivityResult from Fragment */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            // save GPX file to result file location
            Keys.REQUEST_SAVE_GPX -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val sourceUri: Uri = track.gpxUriString.toUri()
                    val targetUri: Uri? = data.data
                    if (targetUri != null) {
                        // copy file async (= fire & forget - no return value needed)
                        GlobalScope.launch { FileHelper.saveCopyOfFileSuspended( activity as  Context, originalFileUri = sourceUri, targetFileUri = targetUri) }
                        Toast.makeText(activity as Context, R.string.toast_message_save_gpx, Toast.LENGTH_LONG).show()
                    }
                }
            }
            // let activity handle result
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
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
            Keys.DIALOG_DELETE_TRACK -> {
                when (dialogResult) {
                    // user tapped remove track
                    true -> {
                        // switch to TracklistFragment and remove track there
                        val trackId: Long = arguments?.getLong(Keys.ARG_TRACK_ID, -1L) ?: -1L
                        val bundle: Bundle = bundleOf(Keys.ARG_TRACK_ID to trackId)
                        findNavController().navigate(R.id.tracklist_fragment, bundle)
                    }
                }
            }
        }
    }


    /* Overrides onMarkerTapped from MarkerListener */
    override fun onMarkerTapped(latitude: Double, longitude: Double) {
        super.onMarkerTapped(latitude, longitude)
        // update track display
        track = TrackHelper.toggleStarred(activity as Context, track, latitude, longitude)
        layout.updateTrackOverlay(track)
        // save track
        GlobalScope.launch {
            FileHelper.saveTrackSuspended(track, true)
        }
    }


    /* Opens up a file picker to select the save location */
    private fun openSaveGpxDialog() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = Keys.MIME_TYPE_GPX
            putExtra(Intent.EXTRA_TITLE, FileHelper.getGpxFileName(track))
        }
        // file gets saved in onActivityResult
        try {
            startActivityForResult(intent, Keys.REQUEST_SAVE_GPX)
        } catch (e: Exception) {
            LogHelper.e(TAG, "Unable to save GPX. ")
            Toast.makeText(activity as Context, R.string.toast_message_install_file_helper, Toast.LENGTH_LONG).show()
        }
    }


    /* Share track as GPX via share sheet */
    private fun shareGpxTrack() {
        val gpxFile = Uri.parse(layout.track.gpxUriString).toFile()
        val gpxShareUri = FileProvider.getUriForFile(this.activity as Context, "${requireActivity().applicationContext.packageName}.provider", gpxFile)
        val shareIntent: Intent = Intent.createChooser(Intent().apply {
            action = Intent.ACTION_SEND
            data = gpxShareUri
            type = Keys.MIME_TYPE_GPX
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            putExtra(Intent.EXTRA_STREAM, gpxShareUri)
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
