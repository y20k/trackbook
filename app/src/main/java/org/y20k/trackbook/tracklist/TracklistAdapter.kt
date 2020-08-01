/*
 * TracklistAdapter.kt
 * Implements the TracklistAdapter class
 * A TracklistAdapter is a custom adapter for a RecyclerView
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


package org.y20k.trackbook.tracklist


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.y20k.trackbook.R
import org.y20k.trackbook.core.Tracklist
import org.y20k.trackbook.core.TracklistElement
import org.y20k.trackbook.helpers.*
import java.util.*


/*
 * TracklistAdapter class
 */
class TracklistAdapter(private val fragment: Fragment) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TracklistAdapter::class.java)


    /* Main class variables */
    private val context: Context = fragment.activity as Context
    private lateinit var tracklistListener: TracklistAdapterListener
    private var useImperial: Boolean = PreferencesHelper.loadUseImperialUnits(context)
    private var tracklist: Tracklist = Tracklist()


    /* Listener Interface */
    interface TracklistAdapterListener {
        fun onTrackElementTapped(tracklistElement: TracklistElement) {}
        // fun onTrackElementStarred(trackId: Long, starred: Boolean)
    }


    /* Overrides onAttachedToRecyclerView from RecyclerView.Adapter */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        // get reference to listener
        tracklistListener = fragment as TracklistAdapterListener
        // load tracklist
        tracklist = FileHelper.readTracklist(context)
        tracklist.tracklistElements.sortByDescending { tracklistElement -> tracklistElement.date }
    }


    /* Overrides onCreateViewHolder from RecyclerView.Adapter */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.track_element, parent, false)
        return TrackElementViewHolder(v)
    }


    /* Overrides getItemCount from RecyclerView.Adapter */
    override fun getItemCount(): Int {
        return tracklist.tracklistElements.size
    }


    /* Overrides onBindViewHolder from RecyclerView.Adapter */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val trackElementViewHolder: TrackElementViewHolder = holder as TrackElementViewHolder
        trackElementViewHolder.trackNameView.text = tracklist.tracklistElements[position].name
        trackElementViewHolder.trackDataView.text = createTrackDataString(position)
        when (tracklist.tracklistElements[position].starred) {
            true -> trackElementViewHolder.starButton.setImageDrawable(context.getDrawable(R.drawable.ic_star_filled_24dp))
            false -> trackElementViewHolder.starButton.setImageDrawable(context.getDrawable(R.drawable.ic_star_outline_24dp))
        }
        trackElementViewHolder.trackElement.setOnClickListener {
            tracklistListener.onTrackElementTapped(tracklist.tracklistElements[position])
        }
        trackElementViewHolder.starButton.setOnClickListener {
            toggleStarred(it, position)
        }
    }


    /* Get track name for given position */
    fun getTrackName(position: Int): String {
        return tracklist.tracklistElements[position].name
    }


    /* Removes track and track files for given position - used by TracklistFragment */
    fun removeTrack(context: Context, position: Int) {
        val backgroundJob = Job()
        val uiScope = CoroutineScope(Dispatchers.Main + backgroundJob)
        uiScope.launch {
            val deferred: Deferred<Tracklist> =
                async { FileHelper.deleteTrackSuspended(context, position, tracklist) }
            // wait for result and store in tracklist
            tracklist = deferred.await()
            notifyItemRemoved(position)
            backgroundJob.cancel()
        }
    }


    /* Finds current position of track element in adapter list */
    fun findPosition(trackId: Long): Int {
        tracklist.tracklistElements.forEachIndexed { index, tracklistElement ->
            if (tracklistElement.getTrackId() == trackId) return index
        }
        return -1
    }


    /* Toggles the starred state of tracklist element - and saves tracklist */
    private fun toggleStarred(view: View, position: Int) {
        val starButton: ImageButton = view as ImageButton
        when (tracklist.tracklistElements[position].starred) {
            true -> {
                starButton.setImageDrawable(context.getDrawable(R.drawable.ic_star_outline_24dp))
                tracklist.tracklistElements[position].starred = false
            }
            false -> {
                starButton.setImageDrawable(context.getDrawable(R.drawable.ic_star_filled_24dp))
                tracklist.tracklistElements[position].starred = true
            }
        }
        GlobalScope.launch {
            FileHelper.saveTracklistSuspended(
                context,
                tracklist,
                GregorianCalendar.getInstance().time
            )
        }
    }


    /* Creates the track data string */
    private fun createTrackDataString(position: Int): String {
        val tracklistElement: TracklistElement = tracklist.tracklistElements[position]
        val trackDataString: String
        when (tracklistElement.name == tracklistElement.dateString) {
            // CASE: no individual name set - exclude date
            true -> trackDataString = "${LengthUnitHelper.convertDistanceToString(
                tracklistElement.length,
                useImperial
            )} • ${tracklistElement.durationString}"
            // CASE: no individual name set - include date
            false -> trackDataString =
                "${tracklistElement.dateString} • ${LengthUnitHelper.convertDistanceToString(
                    tracklistElement.length,
                    useImperial
                )} • ${tracklistElement.durationString}"
        }
        return trackDataString
    }


    /*
     * Inner class: DiffUtil.Callback that determines changes in data - improves list performance
     */
    private inner class DiffCallback(val oldList: Tracklist, val newList: Tracklist) :
        DiffUtil.Callback() {

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList.tracklistElements[oldItemPosition]
            val newItem = newList.tracklistElements[newItemPosition]
            return TrackHelper.getTrackId(oldItem) == TrackHelper.getTrackId(newItem)
        }

        override fun getOldListSize(): Int {
            return oldList.tracklistElements.size
        }

        override fun getNewListSize(): Int {
            return newList.tracklistElements.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList.tracklistElements[oldItemPosition]
            val newItem = newList.tracklistElements[newItemPosition]
            return TrackHelper.getTrackId(oldItem) == TrackHelper.getTrackId(newItem) && oldItem.length == newItem.length
        }
    }
    /*
     * End of inner class
     */


    /*
     * Inner class: ViewHolder for a track element
     */
    private inner class TrackElementViewHolder(trackElementLayout: View) :
        RecyclerView.ViewHolder(trackElementLayout) {
        val trackElement: ConstraintLayout = trackElementLayout.findViewById(R.id.track_element)
        val trackNameView: TextView = trackElementLayout.findViewById(R.id.track_name)
        val trackDataView: TextView = trackElementLayout.findViewById(R.id.track_data)
        val starButton: ImageButton = trackElementLayout.findViewById(R.id.star_button)
    }
    /*
     * End of inner class
     */

}
