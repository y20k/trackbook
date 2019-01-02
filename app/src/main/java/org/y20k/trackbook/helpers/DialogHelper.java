/**
 * DialogHelper.java
 * Implements the DialogHelper class
 * A DialogHelper creates a customizable alert dialog
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-19 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */

package org.y20k.trackbook.helpers;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;


/**
 * DialogHelper class
 */
public class DialogHelper extends DialogFragment implements TrackbookKeys {

    /* Constructs a new instance */
    public static DialogHelper newInstance(int title, String message, int positiveButton, int negativeButton) {
        DialogHelper fragment = new DialogHelper();
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TITLE, title);
        args.putString(ARG_DIALOG_MESSAGE, message);
        args.putInt(ARG_DIALOG_BUTTON_POSITIVE, positiveButton);
        args.putInt(ARG_DIALOG_BUTTON_NEGATIVE, negativeButton);
        fragment.setArguments(args);
        return fragment;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();

        // get text elements
        int title = args.getInt(ARG_DIALOG_TITLE);
        String message = args.getString(ARG_DIALOG_MESSAGE);
        int positiveButton = args.getInt(ARG_DIALOG_BUTTON_POSITIVE);
        int negativeButton = args.getInt(ARG_DIALOG_BUTTON_NEGATIVE);

        // build dialog
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        if (title != -1) {
            dialogBuilder.setTitle(title);
        }
        dialogBuilder.setMessage(message);
        dialogBuilder.setPositiveButton(positiveButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, getActivity().getIntent());
                    }
                }
        );
        dialogBuilder.setNegativeButton(negativeButton,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_CANCELED, getActivity().getIntent());
                    }
                }
        );

        return dialogBuilder.create();
    }
}
