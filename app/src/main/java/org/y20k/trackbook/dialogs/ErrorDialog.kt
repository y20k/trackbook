/*
 * ErrorDialog.kt
 * Implements the ErrorDialog object
 * A ErrorDialog shows an error dialog with details
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
import android.content.DialogInterface
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.y20k.trackbook.R
import org.y20k.trackbook.helpers.LogHelper


/*
 * ErrorDialog object
 */
object ErrorDialog {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(ErrorDialog::class.java)


    /* Construct and show dialog */
    fun show(context: Context, errorTitle: Int, errorMessage: Int, errorDetails: String = String()) {
        // prepare dialog builder
        val builder: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(context, R.style.AlertDialogTheme)

        // set title
        builder.setTitle(context.getString(errorTitle))

        // get views
        val inflater: LayoutInflater = LayoutInflater.from(context)
        val view: View = inflater.inflate(R.layout.dialog_generic_with_details, null)
        val errorMessageView: TextView = view.findViewById(R.id.dialog_message) as TextView
        val errorDetailsLinkView: TextView = view.findViewById(R.id.dialog_details_link) as TextView
        val errorDetailsView: TextView = view.findViewById(R.id.dialog_details) as TextView

        // set dialog view
        builder.setView(view)

        // set detail view
        val detailsNotEmpty = errorDetails.isNotEmpty()
        // show/hide details link depending on whether details are empty or not
        errorDetailsLinkView.isVisible = detailsNotEmpty

        if (detailsNotEmpty) {
            // allow scrolling on details view
            errorDetailsView.movementMethod = ScrollingMovementMethod()

            // show and hide details on click
            errorDetailsLinkView.setOnClickListener {
                when (errorDetailsView.visibility) {
                    View.GONE -> errorDetailsView.isVisible = true
                    View.VISIBLE -> errorDetailsView.isGone = true
                }
            }
            // set details text view
            errorDetailsView.text = errorDetails
        }

        // set text views
        errorMessageView.text = context.getString(errorMessage)

        // add okay button
        builder.setPositiveButton(R.string.dialog_generic_button_okay, DialogInterface.OnClickListener { _, _ ->
            // listen for click on okay button
            // do nothing
        })

        // display error dialog
        builder.show()
    }
}