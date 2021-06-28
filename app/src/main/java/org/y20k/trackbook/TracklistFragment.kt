/*
 * TracklistFragment.kt
 * Implements the TracklistFragment fragment
 * A TracklistFragment displays a list recorded tracks
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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import org.y20k.trackbook.core.TracklistElement
import org.y20k.trackbook.helpers.LogHelper
import org.y20k.trackbook.helpers.TrackHelper
import org.y20k.trackbook.helpers.UiHelper
import org.y20k.trackbook.tracklist.TracklistAdapter


/*
 * TracklistFragment class
 */
class TracklistFragment : Fragment(), TracklistAdapter.TracklistAdapterListener, YesNoDialog.YesNoDialogListener {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TracklistFragment::class.java)


    /* Main class variables */
    private lateinit var tracklistAdapter: TracklistAdapter
    private lateinit var trackElementList: RecyclerView
    private lateinit var tracklistOnboarding: ConstraintLayout


    /* Overrides onCreateView from Fragment */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // create tracklist adapter
        tracklistAdapter = TracklistAdapter(this)
    }


    /* Overrides onCreateView from Fragment */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // find views
        val rootView = inflater.inflate(R.layout.fragment_tracklist, container, false)
        trackElementList = rootView.findViewById(R.id.track_element_list)
        tracklistOnboarding = rootView.findViewById(R.id.track_list_onboarding)

        // add padding - necessary because translucent status bar is used
        val topPadding: Int = UiHelper.getStatusBarHeight(activity as Context)
        trackElementList.setPadding(0, topPadding, 0, 0)

        // set up recycler view
        trackElementList.layoutManager = CustomLinearLayoutManager(activity as Context)
        trackElementList.itemAnimator = DefaultItemAnimator()
        trackElementList.adapter = tracklistAdapter

        // enable swipe to delete
        val swipeHandler = object : UiHelper.SwipeToDeleteCallback(activity as Context) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // ask user
                val adapterPosition: Int = viewHolder.adapterPosition
                val dialogMessage: String = "${getString(R.string.dialog_yes_no_message_delete_recording)}\n\n- ${tracklistAdapter.getTrackName(adapterPosition)}"
                YesNoDialog(this@TracklistFragment as YesNoDialog.YesNoDialogListener).show(context = activity as Context, type = Keys.DIALOG_DELETE_TRACK, messageString = dialogMessage, yesButton = R.string.dialog_yes_no_positive_button_delete_recording, payload = adapterPosition)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(rootView.findViewById(R.id.track_element_list))

        // toggle onboarding layout
        toggleOnboardingLayout(tracklistAdapter.itemCount)

        return rootView
    }


    /* Overrides onTrackElementTapped from TracklistElementAdapterListener */
    override fun onTrackElementTapped(tracklistElement: TracklistElement) {
        val bundle: Bundle = bundleOf(
            Keys.ARG_TRACK_TITLE to tracklistElement.name,
            Keys.ARG_TRACK_FILE_URI to tracklistElement.trackUriString,
            Keys.ARG_GPX_FILE_URI to tracklistElement.gpxUriString,
            Keys.ARG_TRACK_ID to TrackHelper.getTrackId(tracklistElement)
        )
        findNavController().navigate(R.id.fragment_track, bundle)
    }


    /* Overrides onYesNoDialog from YesNoDialogListener */
    override fun onYesNoDialog(type: Int, dialogResult: Boolean, payload: Int, payloadString: String) {
        when (type) {
            Keys.DIALOG_DELETE_TRACK -> {
                when (dialogResult) {
                    // user tapped remove track
                    true -> {
                        toggleOnboardingLayout(tracklistAdapter.itemCount -1)
                        tracklistAdapter.removeTrackAtPosition(activity as Context, payload)
                    }
                    // user tapped cancel
                    false -> {
                        tracklistAdapter.notifyItemChanged(payload)
                    }
                }
            }
        }
    }


    // toggle onboarding layout
    private fun toggleOnboardingLayout(trackCount: Int) {
        when (trackCount == 0) {
            true -> {
                // show onboarding layout
                tracklistOnboarding.visibility = View.VISIBLE
                trackElementList.visibility = View.GONE
            }
            false -> {
                // hide onboarding layout
                tracklistOnboarding.visibility = View.GONE
                trackElementList.visibility = View.VISIBLE
            }
        }
    }



    /*
     * Inner class: custom LinearLayoutManager that overrides onLayoutCompleted
     */
    inner class CustomLinearLayoutManager(context: Context): LinearLayoutManager(context, VERTICAL, false) {

        override fun supportsPredictiveItemAnimations(): Boolean {
            return true
        }

        override fun onLayoutCompleted(state: RecyclerView.State?) {
            super.onLayoutCompleted(state)
            // handle delete request from TrackFragment - after layout calculations are complete
            val deleteTrackId: Long = arguments?.getLong(Keys.ARG_TRACK_ID, -1L) ?: -1L
            arguments?.putLong(Keys.ARG_TRACK_ID, -1L)
            if (deleteTrackId != -1L) {
                CoroutineScope(Main). launch {
                    tracklistAdapter.removeTrackById(this@TracklistFragment.activity as Context, deleteTrackId)
                    toggleOnboardingLayout(tracklistAdapter.itemCount - 1)
                }
            }
        }

    }
    /*
     * End of inner class
     */

}
