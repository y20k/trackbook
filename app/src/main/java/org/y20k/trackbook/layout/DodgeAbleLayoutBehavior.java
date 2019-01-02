/**
 * DodgeAbleLayoutBehavior.java
 * Implements the DodgeAbleLayoutBehavior class
 * A DodgeAbleLayoutBehavior enables any element to be dodged up by a snackbar
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


package org.y20k.trackbook.layout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;

import androidx.coordinatorlayout.widget.CoordinatorLayout;


/**
 * DodgeAbleLayoutBehavior class
 * adapted from: http://stackoverflow.com/a/35904421
 */
public class DodgeAbleLayoutBehavior extends CoordinatorLayout.Behavior<View> {

    /* Constructor (default) */
    public DodgeAbleLayoutBehavior() {
        super();
    }


    /* Constructor for context and attributes */
    public DodgeAbleLayoutBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof Snackbar.SnackbarLayout;
    }


    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        float translationY = Math.min(0, dependency.getTranslationY() - dependency.getHeight());
        child.setTranslationY(translationY);
        return true;
    }

}