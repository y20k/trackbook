/**
 * DialogClearFragment.java
 * Implements the DialogClearFragment class
 * A DialogClearFragment appears when the user wants to clear the map
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */

package org.y20k.trackbook.helpers;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import org.y20k.trackbook.R;


/**
 * DialogClearFragment class
 */
public class DialogClearFragment extends DialogFragment {

    /* Define log tag */
    private static final String LOG_TAG = DialogClearFragment.class.getSimpleName();


    /* Interface that the context that creates an instance of this fragment must implement */
    public interface DialogClearListener {
        public void onDialogClearPositiveClick(DialogFragment dialog);
        public void onDialogClearNegativeClick(DialogFragment dialog);
    }


    /* Main class variables */
    private DialogClearListener mListener;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // verify that the host context implements the callback interface
        try {
            // instantiate the NoticeDialogListener so we can send events to the host
            mListener = (DialogClearListener) context;
        } catch (ClassCastException e) {
            LogHelper.e(LOG_TAG, "Context does not implement the DialogClearListener interface.");
        }
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // construct dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.dialog_clear_map_title)
                .setMessage(R.string.dialog_clear_map_message)
                .setPositiveButton(R.string.dialog_clear_map_okay, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // user clicked CLEAR - inform initiating fragment / context
                        mListener.onDialogClearPositiveClick(DialogClearFragment.this);
                    }
                })
                .setNegativeButton(R.string.dialog_clear_map_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // user clicked CANCEL - inform initiating fragment / context
                        mListener.onDialogClearNegativeClick(DialogClearFragment.this);
                    }
                });
        // create the AlertDialog object and return it
        return builder.create();
    }
}
