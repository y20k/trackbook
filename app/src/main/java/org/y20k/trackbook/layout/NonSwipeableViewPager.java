/**
 * NonSwipeableViewPager.java
 * Implements the NonSwipeableViewPager class
 * A NonSwipeableViewPager is a ViewPager with swiping gestures disabled
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-18 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */

package org.y20k.trackbook.layout;

/**
 * NonSwipeableViewPager class
 * adapted from: http://stackoverflow.com/a/9650884
 */

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import org.y20k.trackbook.helpers.LogHelper;

import java.lang.reflect.Field;

import androidx.viewpager.widget.ViewPager;


public class NonSwipeableViewPager extends ViewPager {

    /* Define log tag */
    private static final String LOG_TAG = NonSwipeableViewPager.class.getSimpleName();


    /* Constructor */
    public NonSwipeableViewPager(Context context) {
        super(context);
        setMyScroller();
    }


    /* Constructor */
    public NonSwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        setMyScroller();
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }


    /* Attaches a custom smooth scrolling scroller to a ViewPager */
    private void setMyScroller() {
        try {
            Class<?> viewpager = ViewPager.class;
            Field scroller = viewpager.getDeclaredField("mScroller");
            scroller.setAccessible(true);
            scroller.set(this, new MyScroller(getContext()));
        } catch (Exception e) {
            LogHelper.e(LOG_TAG, "Problem accessing or modifying the mScroller field. Exception: " + e);
            e.printStackTrace();
        }
    }


    /**
     * Inner class: MyScroller is a custom Scroller
     */
    public class MyScroller extends Scroller {
        public MyScroller(Context context) {
            super(context, new DecelerateInterpolator());
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, 350 /*1 secs*/);
        }
    }
    /**
     * End of inner class
     */

}