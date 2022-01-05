/*
 * TracklistAdapter.kt
 * Implements the TracklistAdapter class
 * A TracklistAdapter is a custom adapter for a RecyclerView
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-22 - Y20K.org
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.y20k.trackbook.Keys
import org.y20k.trackbook.R
import org.y20k.trackbook.core.Tracklist
import org.y20k.trackbook.core.TracklistElement
import org.y20k.trackbook.helpers.*
import java.util.*


/*
 * TracklistAdapter class
 */
class TracklistAdapter(private val fragment: Fragment) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(TracklistAdapter::class.java)


    /* Main class variables */
    private val context: Context = fragment.activity as Context
    private lateinit var tracklistListener: TracklistAdapterListener
    private var useImperial: Boolean = PreferencesHelper.loadUseImperialUnits()
    private var tracklist: Tracklist = Tracklist()


    /* Listener Interface */
    interface TracklistAdapterListener {
        fun onTrackElementTapped(tracklistElement: TracklistElement) {  }
        // fun onTrackElementStarred(trackId: Long, starred: Boolean)
    }


    /* Overrides onAttachedToRecyclerView from RecyclerView.Adapter */
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        // get reference to listener
        tracklistListener = fragment as TracklistAdapterListener
        // load tracklist
        tracklist = FileHelper.readTracklist(context)
        tracklist.tracklistElements.sortByDescending { tracklistElement -> tracklistElement.date  }
        // calculate total duration and distance, if necessary
        if (tracklist.tracklistElements.isNotEmpty() && tracklist.totalDurationAll == 0L) {
            TrackHelper.calculateAndSaveTrackTotals(context, tracklist)
        }
    }


    /* Overrides onCreateViewHolder from RecyclerView.Adapter */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        when (viewType) {
            Keys.VIEW_TYPE_STATISTICS -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.element_statistics, parent, false)
                return ElementStatisticsViewHolder(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.element_track, parent, false)
                return ElementTrackViewHolder(v)
            }
        }
    }


    /* Overrides getItemViewType */
    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return Keys.VIEW_TYPE_STATISTICS
        } else {
            return Keys.VIEW_TYPE_TRACK
        }
    }


    /* Overrides getItemCount from RecyclerView.Adapter */
    override fun getItemCount(): Int {
        // +1 ==> the total statistics element
        return tracklist.tracklistElements.size + 1
    }


    /* Overrides onBindViewHolder from RecyclerView.Adapter */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        when (holder) {

            // CASE STATISTICS ELEMENT
            is ElementStatisticsViewHolder -> {
                val elementStatisticsViewHolder: ElementStatisticsViewHolder = holder as ElementStatisticsViewHolder
                elementStatisticsViewHolder.totalDistanceView.text = LengthUnitHelper.convertDistanceToString(tracklist.totalDistanceAll, useImperial)
            }

            // CASE TRACK ELEMENT
            is ElementTrackViewHolder -> {
                val positionInTracklist: Int = position -1
                val elementTrackViewHolder: ElementTrackViewHolder = holder as ElementTrackViewHolder
                elementTrackViewHolder.trackNameView.text = tracklist.tracklistElements[positionInTracklist].name
                elementTrackViewHolder.trackDataView.text = createTrackDataString(positionInTracklist)
                when (tracklist.tracklistElements[positionInTracklist].starred) {
                    true -> elementTrackViewHolder.starButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_star_filled_24dp))
                    false -> elementTrackViewHolder.starButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_star_outline_24dp))
                }
                elementTrackViewHolder.trackElement.setOnClickListener {
                    tracklistListener.onTrackElementTapped(tracklist.tracklistElements[positionInTracklist])
                }
                elementTrackViewHolder.starButton.setOnClickListener {
                    toggleStarred(it, positionInTracklist)
                }
            }

        }

    }


    /* Get track name for given position */
    fun getTrackName(positionInRecyclerView: Int): String {
        // first position is always the statistics element
        return tracklist.tracklistElements[positionInRecyclerView - 1].name
    }


    /* Removes track and track files for given position - used by TracklistFragment */
    fun removeTrackAtPosition(context: Context, position: Int) {
        CoroutineScope(IO).launch {
            val positionInTracklist = position - 1
            val deferred: Deferred<Tracklist> = async { FileHelper.deleteTrackSuspended(context, positionInTracklist, tracklist) }
            // wait for result and store in tracklist
            withContext(Main) {
                tracklist = deferred.await()
                notifyItemRemoved(position)
                notifyItemChanged(0)
            }
        }
    }


    /* Removes track and track files for given track id - used by TracklistFragment */
    fun removeTrackById(context: Context, trackId: Long) {
        CoroutineScope(IO).launch {
            // reload tracklist //todo check if necessary
//            tracklist = FileHelper.readTracklist(context)
            val positionInTracklist: Int = findPosition(trackId)
            val deferred: Deferred<Tracklist> = async { FileHelper.deleteTrackSuspended(context, positionInTracklist, tracklist) }
            // wait for result and store in tracklist
            withContext(Main) {
                tracklist = deferred.await()
                val positionInRecyclerView: Int = positionInTracklist + 1 // position 0 is the statistics element
                notifyItemRemoved(positionInRecyclerView)
                notifyItemChanged(0)
            }
        }
    }


    /* Returns if the adapter is empty */
    fun isEmpty(): Boolean {
        return tracklist.tracklistElements.size == 0
    }


    /* Finds current position of track element in adapter list */
    private fun findPosition(trackId: Long): Int {
        tracklist.tracklistElements.forEachIndexed {index, tracklistElement ->
            if (tracklistElement.getTrackId() == trackId) return index
        }
        return -1
    }


    /* Toggles the starred state of tracklist element - and saves tracklist */
    private fun toggleStarred(view: View, position: Int) {
        val starButton: ImageButton = view as ImageButton
        when (tracklist.tracklistElements[position].starred) {
            true -> {
                starButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_star_outline_24dp))
                tracklist.tracklistElements[position].starred = false
            }
            false -> {
                starButton.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_star_filled_24dp))
                tracklist.tracklistElements[position].starred = true
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            FileHelper.saveTracklistSuspended(context, tracklist, GregorianCalendar.getInstance().time)
        }
    }


    /* Creates the track data string */
    private fun createTrackDataString(position: Int): String {
        val tracklistElement: TracklistElement = tracklist.tracklistElements[position]
        val trackDataString: String
        when (tracklistElement.name == tracklistElement.dateString) {
            // CASE: no individual name set - exclude date
            true -> trackDataString = "${LengthUnitHelper.convertDistanceToString(tracklistElement.length, useImperial)} • ${tracklistElement.durationString}"
            // CASE: no individual name set - include date
            false -> trackDataString = "${tracklistElement.dateString} • ${LengthUnitHelper.convertDistanceToString(tracklistElement.length, useImperial)} • ${tracklistElement.durationString}"
        }
        return trackDataString
    }


    /*
     * Inner class: DiffUtil.Callback that determines changes in data - improves list performance
     */
    private inner class DiffCallback(val oldList: Tracklist, val newList: Tracklist): DiffUtil.Callback() {

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
    inner class ElementTrackViewHolder (elementTrackLayout: View): RecyclerView.ViewHolder(elementTrackLayout) {
        val trackElement: ConstraintLayout = elementTrackLayout.findViewById(R.id.track_element)
        val trackNameView: TextView = elementTrackLayout.findViewById(R.id.track_name)
        val trackDataView: TextView = elementTrackLayout.findViewById(R.id.track_data)
        val starButton: ImageButton = elementTrackLayout.findViewById(R.id.star_button)

    }
    /*
     * End of inner class
     */


    /*
     * Inner class: ViewHolder for a statistics element
     */
    inner class ElementStatisticsViewHolder (elementStatisticsLayout: View): RecyclerView.ViewHolder(elementStatisticsLayout) {
        val totalDistanceView: TextView = elementStatisticsLayout.findViewById(R.id.total_distance_data)
    }
    /*
     * End of inner class
     */

}
