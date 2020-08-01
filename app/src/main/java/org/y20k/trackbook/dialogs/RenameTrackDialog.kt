/*
 * RenameTrackDialog.kt
 * Implements the RenameTrackDialog class
 * A RenameTrackDialog offers user to change name of track
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


package org.y20k.trackbook.dialogs

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.y20k.trackbook.R
import org.y20k.trackbook.helpers.LogHelper


/*
 * RenameTrackDialog class
 */
class RenameTrackDialog(private var renameTrackListener: RenameTrackListener) {

    /* Interface used to communicate back to activity */
    interface RenameTrackListener {
        fun onRenameTrackDialog(textInput: String) {
        }
    }

    /* Define log tag */
    private val TAG = LogHelper.makeLogTag(RenameTrackDialog::class.java.simpleName)


    /* Construct and show dialog */
    fun show(context: Context, trackName: String) {
        // prepare dialog builder
        val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context)

        // get input field
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_rename_track, null)
        val inputField = view.findViewById<EditText>(R.id.dialog_rename_track_input_edit_text)

        // pre-fill with current track name
        inputField.setText(trackName, TextView.BufferType.EDITABLE)
        inputField.setSelection(trackName.length)
        inputField.inputType = InputType.TYPE_CLASS_TEXT

        // set dialog view
        builder.setView(view)

        // add "add" button
        builder.setPositiveButton(R.string.dialog_rename_track_button) { _, _ ->
            // hand text over to initiating activity
            inputField.text?.let {
                renameTrackListener.onRenameTrackDialog(it.toString())
            }
        }

        // add cancel button
        builder.setNegativeButton(R.string.dialog_generic_button_cancel) { _, _ ->
            // listen for click on cancel button
            // do nothing
        }

        // display add dialog
        builder.show()
    }

}
